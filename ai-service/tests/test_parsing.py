import unittest

from app.search.parsing import (normalize_doi, parse_crossref, parse_openalex, parse_semantic_scholar,
                                reconstruct_abstract)

OPENALEX = {"results": [
    {"id": "https://openalex.org/W1", "display_name": "Deep learning for crop disease", "publication_year": 2021,
     "doi": "https://doi.org/10.1000/ABC", "cited_by_count": 42,
     "abstract_inverted_index": {"Crops": [0], "suffer": [1], "from": [2], "disease": [3, 5], "and": [4], "pests": [6]},
     "authorships": [{"author": {"display_name": "A. Author"}}, {"author": {"display_name": "B. Writer"}}],
     "primary_location": {"source": {"display_name": "Journal of Crops"}}},
    {"id": "https://openalex.org/W2", "display_name": "No doi, no abstract", "publication_year": None,
     "doi": None, "abstract_inverted_index": None, "authorships": [], "primary_location": None},
    {"id": "https://openalex.org/W3", "display_name": None, "title": None},
]}

CROSSREF = {"message": {"items": [
    {"DOI": "10.5555/XYZ", "title": ["Leaf image <i>classification</i>"], "issued": {"date-parts": [[2019, 5]]},
     "abstract": "<jats:p>Abstract Classifying &amp; counting leaves.</jats:p>",
     "author": [{"given": "Ann", "family": "Lee"}, {"family": "Solo"}], "container-title": ["Plant Journal"],
     "is-referenced-by-count": 7, "URL": "http://dx.doi.org/10.5555/XYZ"},
    {"DOI": "10.1/notitle", "title": []},
]}}

SEMANTIC = {"data": [
    {"title": "Mobile plant diagnosis", "abstract": "A mobile tool.", "year": 2020, "venue": "",
     "authors": [{"name": "C. Coder"}, {"name": None}], "externalIds": {"DOI": "10.7/Mobile"},
     "citationCount": 3, "url": "https://www.semanticscholar.org/paper/abc"},
    {"title": "  ", "year": 2020},
]}


class ParsingTests(unittest.TestCase):
    def test_openalex_inverted_index_becomes_text_in_order(self):
        self.assertEqual(reconstruct_abstract({"b": [1], "a": [0], "c": [2, 3]}), "a b c c")
        self.assertIsNone(reconstruct_abstract(None))
        self.assertIsNone(reconstruct_abstract({}))

    def test_openalex_papers(self):
        papers = parse_openalex(OPENALEX)
        self.assertEqual([p.title for p in papers], ["Deep learning for crop disease", "No doi, no abstract"])
        first = papers[0]
        self.assertEqual(first.doi, "10.1000/abc")
        self.assertEqual(first.url, "https://doi.org/10.1000/abc")
        self.assertEqual(first.abstract, "Crops suffer from disease and disease pests")
        self.assertEqual(first.authors, ("A. Author", "B. Writer"))
        self.assertEqual((first.year, first.venue, first.citation_count, first.sources),
                         (2021, "Journal of Crops", 42, ("openalex",)))
        second = papers[1]
        self.assertIsNone(second.doi)
        self.assertIsNone(second.abstract)
        self.assertEqual(second.url, "https://openalex.org/W2")
        self.assertFalse(second.has_abstract)

    def test_crossref_strips_markup_and_the_word_abstract(self):
        papers = parse_crossref(CROSSREF)
        self.assertEqual(len(papers), 1)
        paper = papers[0]
        self.assertEqual(paper.title, "Leaf image classification")
        self.assertEqual(paper.abstract, "Classifying & counting leaves.")
        self.assertEqual((paper.year, paper.doi, paper.venue, paper.citation_count),
                         (2019, "10.5555/xyz", "Plant Journal", 7))
        self.assertEqual(paper.authors, ("Ann Lee", "Solo"))
        self.assertEqual(paper.url, "https://doi.org/10.5555/xyz")

    def test_semantic_scholar_papers(self):
        papers = parse_semantic_scholar(SEMANTIC)
        self.assertEqual(len(papers), 1)
        paper = papers[0]
        self.assertEqual((paper.title, paper.doi, paper.year, paper.citation_count), ("Mobile plant diagnosis", "10.7/mobile", 2020, 3))
        self.assertEqual(paper.authors, ("C. Coder",))
        self.assertIsNone(paper.venue)
        self.assertEqual(paper.url, "https://www.semanticscholar.org/paper/abc")

    def test_garbage_input_gives_no_papers_instead_of_errors(self):
        for parse in (parse_openalex, parse_crossref, parse_semantic_scholar):
            self.assertEqual(parse(None), [])
            self.assertEqual(parse({}), [])
            self.assertEqual(parse({"results": None, "message": None, "data": None}), [])

    def test_doi_normalisation(self):
        self.assertEqual(normalize_doi("https://doi.org/10.1/ABC "), "10.1/abc")
        self.assertEqual(normalize_doi("http://dx.doi.org/10.1/x"), "10.1/x")
        self.assertIsNone(normalize_doi(""))
        self.assertIsNone(normalize_doi(None))


if __name__ == "__main__":
    unittest.main()
