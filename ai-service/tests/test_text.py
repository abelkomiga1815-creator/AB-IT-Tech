import unittest

from app.nlp.text import content_runs, content_tokens, sentences, stem, tokenize


class TextTests(unittest.TestCase):
    def test_tokenize_lowercases_and_keeps_inner_hyphens(self):
        self.assertEqual(tokenize("Deep-learning for Crop Diseases, 2024!"),
                         ["deep-learning", "for", "crop", "diseases"])

    def test_stem_strips_simple_plurals_only(self):
        self.assertEqual(stem("diseases"), "disease")
        self.assertEqual(stem("studies"), "study")
        self.assertEqual(stem("classes"), "class")
        self.assertEqual(stem("processes"), "process")
        for word in ("analysis", "data", "this", "class", "bus"):
            self.assertEqual(stem(word), word)

    def test_content_tokens_drop_stopwords_and_numbers(self):
        self.assertEqual(content_tokens("The detection of diseases in 2024 using images"),
                         ["detection", "disease", "image"])

    def test_runs_break_at_stopwords_and_punctuation(self):
        self.assertEqual(content_runs("Deep learning for crop diseases. Early detection, using images."),
                         [["deep", "learning"], ["crop", "disease"], ["detection"], ["image"]])

    def test_sentences(self):
        self.assertEqual(sentences("One. Two! Three?"), ["One.", "Two!", "Three?"])
        self.assertEqual(sentences(""), [])


if __name__ == "__main__":
    unittest.main()
