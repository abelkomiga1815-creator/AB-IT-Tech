"""HTTP-layer tests. They need fastapi and httpx (pip install -r requirements-dev.txt) and are skipped otherwise."""
import unittest

try:
    from fastapi.testclient import TestClient
    import httpx  # noqa: F401  (TestClient needs it)
    HAS_FASTAPI = True
except Exception:  # pragma: no cover
    HAS_FASTAPI = False

from app.core.config import Settings
from app.search.models import ProviderReport, ProviderStatus
from tests.helpers import FakeSearchService, crop_papers, mobile_papers

KEY = "k" * 40
BODY = {"title": "Crop disease detection on smartphones",
        "abstract": "A mobile app that detects crop disease from leaf images using deep learning."}
RESEARCH_PATHS = ["keywords", "related-work", "similarity", "gaps", "titles", "report", "report/markdown"]


@unittest.skipUnless(HAS_FASTAPI, "fastapi/httpx not installed")
class ApiTests(unittest.TestCase):
    def setUp(self):
        from app.main import create_app
        self.search = FakeSearchService(crop_papers(8) + mobile_papers(5), [
            ProviderReport("openalex", ProviderStatus.OK, 13),
            ProviderReport("crossref", ProviderStatus.UNAVAILABLE, 0, "The provider did not answer in time."),
        ])
        self.app = create_app(Settings(api_key=KEY), self.search)
        self.client = TestClient(self.app, raise_server_exceptions=False)
        self.auth = {"X-API-Key": KEY}

    def post(self, path, body=BODY, headers=None):
        return self.client.post(f"/api/v1/research/{path}", json=body, headers=self.auth if headers is None else headers)

    # ---- health, auth ----
    def test_health_is_public(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"success": True, "status": "UP"})

    def test_every_research_endpoint_requires_the_api_key(self):
        for path in RESEARCH_PATHS:
            for headers in ({}, {"X-API-Key": "wrong"}, {"X-API-Key": KEY + "x"}):
                response = self.post(path, headers=headers)
                self.assertEqual(response.status_code, 401, path)
                self.assertEqual(response.json()["code"], "UNAUTHORIZED")
                self.assertFalse(response.json()["success"])
        self.assertEqual(self.search.calls, 0)   # nothing is searched for unauthenticated callers

    # ---- validation ----
    def test_invalid_input_returns_400_with_field_errors(self):
        for body in ({}, {"title": "ab"}, {"title": "   "}, {"title": "Valid title", "maxResults": 21},
                     {"title": "Valid title", "maxResults": 0}, {"title": "x" * 301},
                     {"title": "Valid title", "abstract": "x" * 5001}):
            response = self.post("report", body)
            self.assertEqual(response.status_code, 400, body)
            payload = response.json()
            self.assertEqual(payload["code"], "VALIDATION_ERROR")
            self.assertTrue(payload["errors"])
            self.assertIn("field", payload["errors"][0])

    def test_blank_abstract_is_treated_as_missing(self):
        response = self.post("keywords", {"title": "Blockchain voting systems", "abstract": "   "})
        self.assertEqual(response.status_code, 200)

    # ---- endpoints ----
    def test_keywords_make_no_external_search(self):
        response = self.post("keywords")
        data = response.json()["data"]
        self.assertEqual(response.status_code, 200)
        self.assertTrue(data["keywords"] and data["queries"])
        self.assertEqual(set(data["keywords"][0]), {"term", "score", "kind", "inTitle"})
        self.assertEqual(self.search.calls, 0)

    def test_related_work_reports_each_source_separately(self):
        data = self.post("related-work", {**BODY, "maxResults": 5}).json()["data"]
        self.assertEqual(self.search.last_limit, 5)
        statuses = {s["provider"]: s["status"] for s in data["sources"]}
        self.assertEqual(statuses, {"openalex": "ok", "crossref": "unavailable"})
        self.assertEqual(data["papersFound"], 13)
        self.assertIn("similarityScore", data["relatedWork"][0])

    def test_similarity_gaps_and_titles(self):
        similarity = self.post("similarity").json()["data"]
        self.assertIn(similarity["similarity"]["level"], ("low", "moderate", "high", "insufficient_data"))
        self.assertIn("not a plagiarism check", similarity["similarity"]["disclaimer"])

        gaps = self.post("gaps").json()["data"]["gapAnalysis"]
        self.assertTrue(gaps["suggestions"])
        self.assertNotIn("high", {s["confidence"] for s in gaps["suggestions"]})
        self.assertIn("caveat", gaps)

        titles = self.post("titles").json()["data"]
        self.assertTrue(titles["alternativeTitles"])

    def test_report_json_and_markdown(self):
        report = self.post("report").json()["data"]
        self.assertEqual(set(report), {"generatedAt", "project", "retrieved", "analysis", "methodology", "notices"})

        response = self.post("report/markdown")
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.headers["content-type"].startswith("text/markdown"))
        self.assertIn("## Part 1. Retrieved from the sources", response.text)

    def test_all_sources_failing_still_gives_an_honest_answer(self):
        self.search.papers = []
        self.search.providers = [ProviderReport("openalex", ProviderStatus.UNAVAILABLE, 0),
                                 ProviderReport("crossref", ProviderStatus.RATE_LIMITED, 0)]
        response = self.post("similarity")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"]["similarity"]["level"], "insufficient_data")

    # ---- errors, headers ----
    def test_unexpected_errors_are_generic(self):
        self.search.error = RuntimeError("secret internal detail")
        response = self.post("report")
        self.assertEqual(response.status_code, 500)
        self.assertEqual(response.json()["code"], "INTERNAL_ERROR")
        self.assertNotIn("secret internal detail", response.text)

    def test_unknown_path_and_wrong_method_use_the_standard_error_format(self):
        missing = self.client.get("/nope")
        self.assertEqual((missing.status_code, missing.json()["code"]), (404, "RESOURCE_NOT_FOUND"))
        wrong = self.client.get("/api/v1/research/report", headers=self.auth)
        self.assertEqual((wrong.status_code, wrong.json()["code"]), (405, "METHOD_NOT_ALLOWED"))

    def test_request_id_is_kept_when_safe_and_replaced_otherwise(self):
        kept = self.client.get("/health", headers={"X-Request-Id": "client-id-12345"})
        self.assertEqual(kept.headers["x-request-id"], "client-id-12345")
        replaced = self.client.get("/health", headers={"X-Request-Id": "bad id!"})
        self.assertRegex(replaced.headers["x-request-id"], r"^[0-9a-f-]{36}$")

    def test_interactive_docs_are_off_by_default(self):
        self.assertEqual(self.client.get("/docs").status_code, 404)
        self.assertEqual(self.client.get("/openapi.json").status_code, 404)

    def test_docs_can_be_enabled_for_development(self):
        from app.main import create_app
        client = TestClient(create_app(Settings(api_key=KEY, enable_docs=True), self.search))
        self.assertEqual(client.get("/docs").status_code, 200)


if __name__ == "__main__":
    unittest.main()
