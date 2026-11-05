import unittest

from app.gap_analysis.analyzer import analyze_gaps
from app.nlp.keywords import Keyword, extract_keywords
from app.recommendations.terms import related_terms
from app.recommendations.titles import suggest_titles, title_case
from tests.helpers import PROJECT_ABSTRACT, PROJECT_TITLE, crop_papers, mobile_papers, paper


class TitleTests(unittest.TestCase):
    def setUp(self):
        self.keywords = extract_keywords(PROJECT_TITLE, PROJECT_ABSTRACT)

    def test_titles_are_distinct_limited_and_never_the_original(self):
        titles = suggest_titles(PROJECT_TITLE, self.keywords, None)
        names = [t.title.lower() for t in titles]
        self.assertTrue(1 <= len(titles) <= 5)
        self.assertEqual(len(names), len(set(names)))
        self.assertNotIn(PROJECT_TITLE.lower(), names)
        self.assertTrue(all(t.rationale for t in titles))

    def test_titles_reuse_the_students_own_terms(self):
        top = self.keywords[0].term.split()[0].lower()
        titles = suggest_titles(PROJECT_TITLE, self.keywords, None)
        self.assertTrue(all(top in t.title.lower() for t in titles))

    def test_a_detected_gap_produces_a_combination_title_first(self):
        keywords = [Keyword("crop disease detection", 1.0, "phrase", True), Keyword("smartphone", 0.8, "term", True)]
        gaps = analyze_gaps(keywords, crop_papers(8) + mobile_papers(5), current_year=2026)
        titles = suggest_titles(PROJECT_TITLE, keywords, gaps)
        self.assertIn("Under-Studied Combination", titles[0].title)
        self.assertIn("Crop Disease Detection", titles[0].title)

    def test_no_keywords_no_titles(self):
        self.assertEqual(suggest_titles("x", [], None), [])

    def test_title_case_keeps_small_words_lower(self):
        self.assertEqual(title_case("detection of crop disease in the field"),
                         "Detection of Crop Disease in the Field")


class RelatedTermsTests(unittest.TestCase):
    def test_terms_frequent_in_papers_but_missing_from_the_project_are_suggested(self):
        papers = [paper(f"Paper {i}", "Convolutional networks classify images with transfer learning.") for i in range(6)]
        found = [t.term for t in related_terms("Crop disease detection app", papers)]
        self.assertIn("convolutional", found)
        self.assertNotIn("crop", found)

    def test_terms_already_in_the_project_are_excluded(self):
        papers = [paper(f"Paper {i}", "Transfer learning for leaf images.") for i in range(6)]
        terms = [t.term for t in related_terms("Transfer learning on leaf images", papers)]
        for term in terms:
            self.assertFalse({"transfer", "learning", "leaf", "image"} & set(term.split()))

    def test_too_few_papers_gives_nothing(self):
        self.assertEqual(related_terms("anything", [paper("A", "text")]), [])

    def test_no_pairs_across_sentence_boundaries(self):
        papers = [paper(f"P{i}", "Results improved the network. Future experiments continue.") for i in range(6)]
        terms = [t.term for t in related_terms("unrelated project", papers)]
        self.assertNotIn("network future", terms)


if __name__ == "__main__":
    unittest.main()
