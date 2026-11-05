import unittest

from app.similarity.indicator import SimilarityLevel, compute_similarity, score_papers
from app.similarity.tfidf import build_vectors, cosine, features, similarity_scores
from tests.helpers import PROJECT_ABSTRACT, PROJECT_TITLE, mobile_papers, paper

PROJECT = f"{PROJECT_TITLE}. {PROJECT_ABSTRACT}"


class TfidfTests(unittest.TestCase):
    def test_identical_texts_score_one_and_disjoint_texts_zero(self):
        scores = similarity_scores("crop disease detection", ["crop disease detection", "quantum chromodynamics lattice"])
        self.assertAlmostEqual(scores[0], 1.0, places=3)
        self.assertEqual(scores[1], 0.0)

    def test_closer_text_scores_higher(self):
        scores = similarity_scores(PROJECT, [
            "Crop disease detection from leaf images with deep learning on a mobile app",
            "Soil moisture sensors for irrigation scheduling",
        ])
        self.assertGreater(scores[0], scores[1])

    def test_cosine_of_empty_vectors_is_zero(self):
        self.assertEqual(cosine({}, {"a": 1.0}), 0.0)

    def test_phrases_count_as_features(self):
        self.assertIn("crop_disease", features("crop disease detection"))

    def test_vectors_are_built_for_every_document(self):
        vectors, idf = build_vectors([["a", "b"], ["b", "c"]])
        self.assertEqual(len(vectors), 2)
        self.assertGreater(idf["a"], idf["b"])   # rarer term, higher weight


class IndicatorTests(unittest.TestCase):
    def indicator(self, papers):
        return compute_similarity(PROJECT_TITLE, PROJECT_ABSTRACT, score_papers(PROJECT, papers))

    def test_near_duplicate_gives_high_and_lists_the_match(self):
        twin = paper(PROJECT_TITLE, PROJECT_ABSTRACT, doi="10.1/twin")
        result = self.indicator([twin, *mobile_papers(4)])
        self.assertEqual(result.level, SimilarityLevel.HIGH)
        self.assertEqual(result.matches[0].paper.doi, "10.1/twin")
        self.assertIn("crop", result.matches[0].shared_terms)
        self.assertEqual(result.matches[0].basis, "title_and_abstract")

    def test_unrelated_papers_give_low(self):
        result = self.indicator([paper(f"Soil moisture sensing {i}", "Sensors measure irrigation needs in orchards.") for i in range(6)])
        self.assertEqual(result.level, SimilarityLevel.LOW)
        self.assertIn("other relevant work may exist", result.explanation)

    def test_no_papers_is_insufficient_data_not_originality(self):
        result = self.indicator([])
        self.assertEqual(result.level, SimilarityLevel.INSUFFICIENT_DATA)
        self.assertIn("says nothing about the originality", result.explanation)

    def test_titles_only_is_insufficient_data(self):
        result = self.indicator([paper(PROJECT_TITLE, None) for _ in range(3)])
        self.assertEqual(result.level, SimilarityLevel.INSUFFICIENT_DATA)
        self.assertEqual(result.papers_with_abstract, 0)

    def test_few_abstracts_add_a_caution(self):
        result = self.indicator([paper("Soil sensing", "Sensors measure irrigation needs in orchards.")])
        self.assertIn("extra caution", result.explanation)

    def test_disclaimer_is_always_present_and_never_claims_originality(self):
        result = self.indicator(mobile_papers(5))
        self.assertIn("not a plagiarism check", result.disclaimer)
        text = (result.explanation + result.disclaimer).lower()
        for forbidden in ("100%", "guarantee", "your project is original", "is unique", "no plagiarism", "plagiarism-free"):
            self.assertNotIn(forbidden, text)

    def test_matches_are_sorted_best_first_and_limited(self):
        papers = [paper(f"Crop disease detection {i}", "Leaf images show disease." + " word" * i) for i in range(8)]
        result = self.indicator(papers)
        scores = [m.score for m in result.matches]
        self.assertEqual(scores, sorted(scores, reverse=True))
        self.assertLessEqual(len(result.matches), 5)


if __name__ == "__main__":
    unittest.main()
