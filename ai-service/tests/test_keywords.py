import unittest

from app.nlp.keywords import build_search_queries, extract_keywords

TITLE = "Machine learning for crop disease detection from leaf images"
ABSTRACT = ("We study crop disease detection. Convolutional neural networks classify leaf images captured by "
            "smartphones. Early disease detection helps farmers reduce crop losses.")


class KeywordTests(unittest.TestCase):
    def test_best_keyword_is_the_repeated_title_phrase(self):
        keywords = extract_keywords(TITLE, ABSTRACT)
        self.assertEqual(keywords[0].term, "crop disease detection")
        self.assertEqual(keywords[0].score, 1.0)
        self.assertTrue(keywords[0].in_title)
        self.assertEqual(keywords[0].kind, "phrase")

    def test_expected_phrases_are_found(self):
        terms = [k.term for k in extract_keywords(TITLE, ABSTRACT)]
        for expected in ("leaf images", "convolutional neural networks", "machine learning"):
            self.assertIn(expected, terms)

    def test_stopwords_never_appear_and_scores_are_relative(self):
        keywords = extract_keywords(TITLE, ABSTRACT)
        for keyword in keywords:
            self.assertNotIn("for", keyword.tokens)
            self.assertNotIn("from", keyword.tokens)
            self.assertTrue(0 < keyword.score <= 1)

    def test_subphrases_of_selected_phrases_are_dropped(self):
        terms = [k.term for k in extract_keywords(TITLE, ABSTRACT)]
        self.assertNotIn("disease detection", terms)

    def test_works_without_an_abstract_and_with_empty_input(self):
        self.assertTrue(extract_keywords("Blockchain voting systems"))
        self.assertEqual(extract_keywords("", ""), [])
        self.assertEqual(extract_keywords("the of and", ""), [])

    def test_max_keywords_is_respected(self):
        self.assertLessEqual(len(extract_keywords(TITLE, ABSTRACT, max_keywords=3)), 3)

    def test_queries_contain_only_extracted_terms_without_repeats(self):
        keywords = extract_keywords(TITLE, ABSTRACT)
        queries = build_search_queries(keywords)
        self.assertTrue(1 <= len(queries) <= 3)
        self.assertEqual(len(queries), len(set(queries)))
        allowed = {word for k in keywords for word in k.term.split()}
        for query in queries:
            words = query.split()
            self.assertEqual(len(words), len(set(words)))
            self.assertTrue(set(words) <= allowed)
            self.assertLessEqual(len(query), 150)

    def test_no_keywords_no_queries(self):
        self.assertEqual(build_search_queries([]), [])


if __name__ == "__main__":
    unittest.main()
