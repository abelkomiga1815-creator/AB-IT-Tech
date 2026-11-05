import asyncio
import unittest

from app.core.cache import TTLCache
from app.search.models import ProviderStatus
from app.search.service import SearchService, merge_papers
from tests.helpers import RATE_LIMITED, UNAVAILABLE, FakeProvider, paper


def run(coro):
    return asyncio.run(coro)


def service(*providers, ttl=3600.0):
    return SearchService(providers, TTLCache(ttl, 100), provider_timeout=1.0)


def status_of(outcome, name):
    return next(r for r in outcome.providers if r.provider == name)


class SearchServiceTests(unittest.TestCase):
    def test_healthy_providers_are_merged(self):
        a = FakeProvider("openalex", [paper("Alpha study", "abstract a", doi="10.1/a")])
        b = FakeProvider("crossref", [paper("Beta study", "abstract b", doi="10.1/b", sources=("crossref",))])
        outcome = run(service(a, b).search(["q"], 5))
        self.assertEqual(sorted(p.title for p in outcome.papers), ["Alpha study", "Beta study"])
        self.assertTrue(all(r.status == ProviderStatus.OK for r in outcome.providers))
        self.assertFalse(outcome.all_providers_failed)

    def test_no_results_is_different_from_unavailable(self):
        empty = FakeProvider("openalex", [])
        broken = FakeProvider("crossref", error=UNAVAILABLE)
        good = FakeProvider("semantic_scholar", [paper("Gamma", "x")])
        outcome = run(service(empty, broken, good).search(["q"], 5))
        self.assertEqual(status_of(outcome, "openalex").status, ProviderStatus.NO_RESULTS)
        self.assertEqual(status_of(outcome, "crossref").status, ProviderStatus.UNAVAILABLE)
        self.assertEqual(status_of(outcome, "semantic_scholar").status, ProviderStatus.OK)
        self.assertEqual(len(outcome.papers), 1)

    def test_rate_limit_is_reported_and_stops_further_queries_for_that_provider(self):
        limited = FakeProvider("openalex", error=RATE_LIMITED)
        outcome = run(service(limited).search(["one", "two", "three"], 5))
        self.assertEqual(status_of(outcome, "openalex").status, ProviderStatus.RATE_LIMITED)
        self.assertEqual(len(limited.calls), 1)
        self.assertTrue(outcome.all_providers_failed)

    def test_one_failing_provider_does_not_hide_the_others(self):
        outcome = run(service(FakeProvider("openalex", error=UNAVAILABLE),
                              FakeProvider("crossref", [paper("Delta", "x")])).search(["q"], 5))
        self.assertEqual([p.title for p in outcome.papers], ["Delta"])
        self.assertFalse(outcome.all_providers_failed)

    def test_unexpected_provider_bugs_are_contained(self):
        outcome = run(service(FakeProvider("openalex", error=RuntimeError("bug")),
                              FakeProvider("crossref", [paper("Epsilon", "x")])).search(["q"], 5))
        self.assertEqual(status_of(outcome, "openalex").status, ProviderStatus.UNAVAILABLE)
        self.assertNotIn("bug", status_of(outcome, "openalex").message)
        self.assertEqual(len(outcome.papers), 1)

    def test_slow_provider_times_out_as_unavailable(self):
        class Slow(FakeProvider):
            async def search(self, query, limit):
                await asyncio.sleep(5)
                return []
        svc = SearchService([Slow("openalex")], TTLCache(60, 10), provider_timeout=0.0)  # ceiling = 2 s
        svc._call_timeout = 0.05
        outcome = run(svc.search(["q"], 5))
        self.assertEqual(status_of(outcome, "openalex").status, ProviderStatus.UNAVAILABLE)
        self.assertIn("did not answer in time", status_of(outcome, "openalex").message)

    def test_partial_results_before_a_failure_are_kept_and_reported(self):
        class FailsOnSecond(FakeProvider):
            async def search(self, query, limit):
                self.calls.append((query, limit))
                if len(self.calls) == 2:
                    raise UNAVAILABLE
                return [paper("Zeta", "x")]
        outcome = run(service(FailsOnSecond("openalex")).search(["one", "two"], 5))
        report = status_of(outcome, "openalex")
        self.assertEqual(report.status, ProviderStatus.UNAVAILABLE)
        self.assertEqual(report.papers_found, 1)
        self.assertIn("1 paper(s) were obtained", report.message)
        self.assertFalse(outcome.all_providers_failed)

    def test_answers_are_cached(self):
        provider = FakeProvider("openalex", [paper("Eta", "x")])
        svc = service(provider)
        run(svc.search(["q"], 5))
        run(svc.search(["q"], 5))
        self.assertEqual(len(provider.calls), 1)
        run(svc.search(["q"], 6))          # a different limit is a different request
        self.assertEqual(len(provider.calls), 2)

    def test_failures_are_not_cached(self):
        provider = FakeProvider("openalex", error=UNAVAILABLE)
        svc = service(provider)
        run(svc.search(["q"], 5))
        run(svc.search(["q"], 5))
        self.assertEqual(len(provider.calls), 2)

    def test_no_queries_or_providers_means_empty_outcome(self):
        self.assertEqual(run(service(FakeProvider("a")).search([], 5)).papers, [])
        self.assertEqual(run(service().search(["q"], 5)).providers, [])


class MergeTests(unittest.TestCase):
    def test_same_doi_is_one_paper_with_combined_information(self):
        a = paper("Same Work", "short", doi="10.1/x", year=None, sources=("openalex",), citations=3)
        b = paper("Same work (journal version)", "a much longer abstract text", doi="10.1/x", year=2020,
                  sources=("crossref",), citations=9)
        merged = merge_papers([a, b])
        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0].sources, ("openalex", "crossref"))
        self.assertEqual(merged[0].abstract, "a much longer abstract text")
        self.assertEqual((merged[0].year, merged[0].citation_count), (2020, 9))

    def test_same_title_without_doi_is_one_paper(self):
        merged = merge_papers([paper("Deep Learning: A Survey!", "x"), paper("deep learning a survey", "y", sources=("crossref",))])
        self.assertEqual(len(merged), 1)

    def test_different_papers_stay_separate(self):
        self.assertEqual(len(merge_papers([paper("One", "x", doi="10.1/1"), paper("Two", "y", doi="10.1/2")])), 2)


if __name__ == "__main__":
    unittest.main()
