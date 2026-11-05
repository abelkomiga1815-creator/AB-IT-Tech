import asyncio
import unittest
from datetime import datetime, timezone

from app.pipeline import ResearchPipeline
from app.reports.builder import build_report
from app.reports.markdown import render_markdown
from app.search.models import ProviderReport, ProviderStatus
from app.similarity.indicator import SimilarityLevel
from tests.helpers import (PROJECT_ABSTRACT, PROJECT_TITLE, FakeSearchService, crop_papers, mobile_papers)

NOW = datetime(2026, 10, 7, 12, 0, tzinfo=timezone.utc)


def analyze(service, title=PROJECT_TITLE, abstract=PROJECT_ABSTRACT):
    return asyncio.run(ResearchPipeline(service, clock=lambda: NOW).analyze(title, abstract, 10))


class PipelineTests(unittest.TestCase):
    def test_full_analysis_with_two_topics(self):
        service = FakeSearchService(crop_papers(8) + mobile_papers(5))
        result = analyze(service)
        self.assertEqual(service.calls, 1)
        self.assertTrue(result.keywords and result.queries)
        self.assertEqual(len(result.ranked_papers), 13)
        scores = [item.score for item in result.ranked_papers]
        self.assertEqual(scores, sorted(scores, reverse=True))
        self.assertIn(result.similarity.level, (SimilarityLevel.MODERATE, SimilarityLevel.HIGH, SimilarityLevel.LOW))
        self.assertTrue(result.gaps.suggestions)
        self.assertTrue(result.titles)
        self.assertEqual(result.generated_at, NOW)

    def test_no_papers_found_is_handled(self):
        result = analyze(FakeSearchService([], [ProviderReport("openalex", ProviderStatus.UNAVAILABLE, 0, "down")]))
        self.assertEqual(result.similarity.level, SimilarityLevel.INSUFFICIENT_DATA)
        self.assertEqual(result.gaps.suggestions, ())
        self.assertEqual(result.ranked_papers, [])

    def test_title_only_input_works(self):
        result = analyze(FakeSearchService(crop_papers(6)), abstract=None)
        self.assertTrue(result.keywords)


class ReportTests(unittest.TestCase):
    def setUp(self):
        providers = [ProviderReport("openalex", ProviderStatus.OK, 13),
                     ProviderReport("crossref", ProviderStatus.UNAVAILABLE, 0, "The provider did not answer in time."),
                     ProviderReport("semantic_scholar", ProviderStatus.NO_RESULTS, 0)]
        self.result = analyze(FakeSearchService(crop_papers(8) + mobile_papers(5), providers))
        self.report = build_report(self.result)
        self.markdown = render_markdown(self.report)

    def test_structure_separates_retrieved_facts_from_analysis(self):
        self.assertEqual(set(self.report), {"generatedAt", "project", "retrieved", "analysis", "methodology", "notices"})
        self.assertEqual(set(self.report["retrieved"]), {"queries", "sources", "papersFound", "relatedWork"})
        self.assertEqual(set(self.report["analysis"]),
                         {"keywords", "similarity", "gapAnalysis", "relatedTerms", "alternativeTitles"})
        self.assertEqual(self.report["generatedAt"], "2026-10-07T12:00:00Z")

    def test_provider_statuses_are_reported_separately(self):
        statuses = {s["provider"]: s["status"] for s in self.report["retrieved"]["sources"]}
        self.assertEqual(statuses, {"openalex": "ok", "crossref": "unavailable", "semantic_scholar": "no_results"})
        self.assertTrue(any("crossref" in notice and "unavailable" in notice for notice in self.report["notices"]))

    def test_related_work_carries_links_scores_and_basis(self):
        first = self.report["retrieved"]["relatedWork"][0]
        for key in ("title", "url", "similarityScore", "sharedTerms", "basis", "sources", "doi"):
            self.assertIn(key, first)
        self.assertLessEqual(len(self.report["retrieved"]["relatedWork"]), 15)

    def test_report_never_overclaims(self):
        text = (self.markdown + " ".join(self.report["notices"])).lower()
        self.assertIn("not a plagiarism check", text)
        for forbidden in ("100%", "guarantee", "your project is original", "plagiarism-free", "no similar work exists"):
            self.assertNotIn(forbidden, text)
        self.assertIn("No language model was used", " ".join(self.report["methodology"]))

    def test_markdown_has_both_parts_and_the_evidence(self):
        for heading in ("# Research report:", "## Part 1. Retrieved from the sources", "## Part 2. Analysis by this tool",
                        "### Similarity indicator", "### Possible research gaps", "## Notices"):
            self.assertIn(heading, self.markdown)
        self.assertIn("Evidence:", self.markdown)
        self.assertIn("**crossref**: unavailable", self.markdown)

    def test_markdown_for_an_empty_search(self):
        empty = build_report(analyze(FakeSearchService([], [ProviderReport("openalex", ProviderStatus.UNAVAILABLE, 0)])))
        markdown = render_markdown(empty)
        self.assertIn("No papers were retrieved.", markdown)
        self.assertIn("insufficient data", markdown)


if __name__ == "__main__":
    unittest.main()
