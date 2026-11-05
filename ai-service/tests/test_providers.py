import asyncio
import unittest

from app.core.config import Settings
from app.search.http import HttpResult, ProviderRateLimited, ProviderUnavailable
from app.search.providers import CrossrefProvider, OpenAlexProvider, SemanticScholarProvider
from tests.helpers import FakeHttpClient
from tests.test_parsing import CROSSREF, OPENALEX, SEMANTIC

KEY = "k" * 40


def settings(**overrides):
    return Settings(api_key=KEY, **overrides)


def run(coro):
    return asyncio.run(coro)


class ProviderRequestTests(unittest.TestCase):
    def test_openalex_request_and_parsing(self):
        client = FakeHttpClient(HttpResult(200, OPENALEX))
        papers = run(OpenAlexProvider(client, settings(contact_email="me@example.org")).search("crop disease", 7))
        request = client.requests[0]
        self.assertEqual(request["url"], "https://api.openalex.org/works")
        self.assertEqual(request["params"], {"search": "crop disease", "per-page": 7, "mailto": "me@example.org"})
        self.assertEqual(len(papers), 2)

    def test_no_mailto_when_no_contact_email(self):
        client = FakeHttpClient(HttpResult(200, OPENALEX))
        run(OpenAlexProvider(client, settings()).search("q", 5))
        self.assertNotIn("mailto", client.requests[0]["params"])

    def test_crossref_request_and_parsing(self):
        client = FakeHttpClient(HttpResult(200, CROSSREF))
        papers = run(CrossrefProvider(client, settings()).search("leaf", 4))
        request = client.requests[0]
        self.assertEqual(request["params"]["query.bibliographic"], "leaf")
        self.assertEqual(request["params"]["rows"], 4)
        self.assertIn("abstract", request["params"]["select"])
        self.assertEqual(len(papers), 1)

    def test_semantic_scholar_sends_the_key_only_when_configured(self):
        without = FakeHttpClient(HttpResult(200, SEMANTIC))
        run(SemanticScholarProvider(without, settings()).search("q", 3))
        self.assertNotIn("x-api-key", without.requests[0]["headers"])

        with_key = FakeHttpClient(HttpResult(200, SEMANTIC))
        papers = run(SemanticScholarProvider(with_key, settings(semantic_scholar_api_key="s3cret")).search("q", 3))
        self.assertEqual(with_key.requests[0]["headers"]["x-api-key"], "s3cret")
        self.assertEqual(with_key.requests[0]["params"]["limit"], 3)
        self.assertEqual(len(papers), 1)

    def test_http_429_is_rate_limiting(self):
        for provider in (OpenAlexProvider, CrossrefProvider, SemanticScholarProvider):
            with self.assertRaises(ProviderRateLimited):
                run(provider(FakeHttpClient(HttpResult(429, None)), settings()).search("q", 5))

    def test_server_errors_and_bad_bodies_are_unavailable(self):
        for result in (HttpResult(500, {"error": "boom"}), HttpResult(503, None), HttpResult(200, None), HttpResult(404, {})):
            with self.assertRaises(ProviderUnavailable):
                run(OpenAlexProvider(FakeHttpClient(result), settings()).search("q", 5))

    def test_network_errors_propagate_as_unavailable(self):
        client = FakeHttpClient(error=ProviderUnavailable("timeout"))
        with self.assertRaises(ProviderUnavailable):
            run(CrossrefProvider(client, settings()).search("q", 5))

    def test_the_configured_timeout_is_used(self):
        client = FakeHttpClient(HttpResult(200, OPENALEX))
        run(OpenAlexProvider(client, settings(request_timeout_seconds=3.5)).search("q", 5))
        self.assertEqual(client.requests[0]["timeout"], 3.5)


if __name__ == "__main__":
    unittest.main()
