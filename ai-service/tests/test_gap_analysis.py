import unittest

from app.gap_analysis.analyzer import MIN_PAPERS, analyze_gaps
from app.nlp.keywords import Keyword
from tests.helpers import crop_papers, mobile_papers, paper


def keyword(term: str) -> Keyword:
    return Keyword(term=term, score=1.0, kind="phrase" if " " in term else "term", in_title=True)


class GapAnalysisTests(unittest.TestCase):
    keywords = [keyword("crop disease detection"), keyword("smartphone")]

    def test_two_topics_that_never_meet_are_reported_with_evidence(self):
        result = analyze_gaps(self.keywords, crop_papers(8) + mobile_papers(5), current_year=2026)
        gap = next(g for g in result.suggestions if g.type == "COMBINATION_UNDER_EXPLORED")
        self.assertIn("crop disease detection", gap.title)
        self.assertIn("smartphone", gap.title)
        self.assertEqual(gap.confidence, "low")             # only 5 papers on the smaller side: not enough for "medium"
        counts = " ".join(e.text for e in gap.evidence)
        self.assertIn("8 of 13", counts)
        self.assertIn("0 retrieved paper(s) mention both", counts)
        self.assertTrue(all(e.papers for e in gap.evidence[:2]))     # example papers are listed

    def test_medium_confidence_needs_enough_papers_on_both_sides_and_no_overlap(self):
        result = analyze_gaps(self.keywords, crop_papers(20) + mobile_papers(20), current_year=2026)
        gap = next(g for g in result.suggestions if g.type == "COMBINATION_UNDER_EXPLORED")
        self.assertEqual(gap.confidence, "medium")

    def test_confidence_is_never_high(self):
        result = analyze_gaps(self.keywords, crop_papers(20) + mobile_papers(20), current_year=2026)
        self.assertTrue(result.suggestions)
        for suggestion in result.suggestions:
            self.assertIn(suggestion.confidence, ("low", "medium"))

    def test_topics_that_often_appear_together_are_not_a_gap(self):
        both = [paper(f"Smartphone crop disease detection {i}", "Smartphone apps detect crop disease.") for i in range(8)]
        result = analyze_gaps(self.keywords, both, current_year=2026)
        self.assertFalse([g for g in result.suggestions if g.type == "COMBINATION_UNDER_EXPLORED"])

    def test_too_few_papers_gives_no_suggestions_and_says_why(self):
        result = analyze_gaps(self.keywords, crop_papers(MIN_PAPERS - 1), current_year=2026)
        self.assertEqual(result.suggestions, ())
        self.assertIn("at least", result.note)

    def test_missing_concept_is_flagged_cautiously(self):
        keywords = [keyword("crop disease detection"), keyword("hyperspectral drones")]
        result = analyze_gaps(keywords, crop_papers(8), current_year=2026)
        missing = [g for g in result.suggestions if g.type == "CONCEPT_NOT_FOUND"]
        self.assertEqual(len(missing), 1)
        self.assertIn("synonyms", missing[0].description)
        self.assertEqual(missing[0].confidence, "low")

    def test_old_literature_is_reported(self):
        old = [paper(f"Crop disease detection {i}", "Leaf images.", year=2005 + i) for i in range(10)]
        result = analyze_gaps([keyword("crop disease detection")], old, current_year=2026)
        self.assertIn("LITTLE_RECENT_WORK", [g.type for g in result.suggestions])

    def test_recent_literature_is_not_reported_as_old(self):
        recent = [paper(f"Crop disease detection {i}", "Leaf images.", year=2023 + i % 3) for i in range(10)]
        result = analyze_gaps([keyword("crop disease detection")], recent, current_year=2026)
        self.assertNotIn("LITTLE_RECENT_WORK", [g.type for g in result.suggestions])

    def test_limitation_sentences_are_quoted_with_their_paper(self):
        result = analyze_gaps(self.keywords, crop_papers(8) + mobile_papers(5), current_year=2026)
        limitations = next(g for g in result.suggestions if g.type == "REPORTED_LIMITATIONS")
        self.assertIn("Future work", limitations.evidence[0].text)
        self.assertTrue(limitations.evidence[0].papers[0].title)
        self.assertLessEqual(len(limitations.evidence), 5)

    def test_concept_coverage_counts_are_reported(self):
        result = analyze_gaps(self.keywords, crop_papers(8) + mobile_papers(5), current_year=2026)
        coverage = {c.concept: c.papers_mentioning for c in result.concepts}
        self.assertEqual(coverage["crop disease detection"], 8)
        self.assertEqual(coverage["smartphone"], 5)

    def test_every_analysis_carries_the_caveat(self):
        result = analyze_gaps(self.keywords, crop_papers(8), current_year=2026)
        self.assertIn("only on the papers retrieved", result.caveat)


if __name__ == "__main__":
    unittest.main()
