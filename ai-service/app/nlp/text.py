"""Tokenisation and light normalisation. Standard library only."""
from __future__ import annotations

import re

STOPWORDS: frozenset[str] = frozenset(
    """
    a about above after again against all almost also although always am among an and any are as at
    be because been before being below between both but by can could did do does doing down during
    each either else etc few for from further had has have having he her here hers herself him himself
    his how however i if in into is it its itself just may me might more most much must my myself no
    nor not of off on once only or other our ours ourselves out over own same shall she should so some
    such than that the their theirs them themselves then there these they this those through thus to
    too toward towards under until up upon us very via was we were what when where whether which while
    who whom whose why will with within without would you your yours yourself yourselves
    paper papers study studies research propose proposed proposes present presents presented result
    results show shows shown showed using used use uses based novel new approach approaches method
    methods many several various different one two three first second well often since given make
    makes made order case cases
    help helps helped reduce reduces reduced improve improves improved enable enables allow allows
    provide provides achieve achieves develop develops developed demonstrate demonstrates investigate
    investigates analyze analyzes analyse evaluate evaluates describe describes discuss discusses
    introduce introduces focus focuses require requires need needs lead leads early good better best
    recent recently existing current significant effective efficient important main key general
    """.split()
)

_TOKEN = re.compile(r"[^\W\d_](?:\w|-(?=\w))*", re.UNICODE)
_SENTENCE_END = re.compile(r"(?<=[.!?])\s+")
# punctuation (and spaced dashes) that end a phrase
SEGMENT_SPLIT = re.compile(r"[.,;:!?()\[\]{}\"\u201c\u201d\u2018\u2019/\n\r]|\s[-\u2013\u2014]\s")


def tokenize(text: str) -> list[str]:
    """Lower-case word tokens (letters first, digits and inner hyphens allowed)."""
    return _TOKEN.findall((text or "").lower())


def stem(token: str) -> str:
    """Very light plural stripping, so 'disease' and 'diseases' match. Not a full stemmer."""
    if len(token) <= 3 or "-" in token:
        return token
    if token.endswith("ies") and len(token) > 4:
        return token[:-3] + "y"
    if token.endswith(("sses", "xes", "ches", "shes")):
        return token[:-2]
    if token.endswith("s") and not token.endswith(("ss", "us", "is", "ics")):
        return token[:-1]
    return token


def is_content_word(token: str) -> bool:
    return len(token) > 1 and token not in STOPWORDS and not token.isdigit()


def content_tokens(text: str) -> list[str]:
    """Normalised tokens without stop-words, in order."""
    return [stem(token) for token in tokenize(text) if is_content_word(token)]


def sentences(text: str) -> list[str]:
    return [part.strip() for part in _SENTENCE_END.split(text or "") if part.strip()]


def content_runs(text: str) -> list[list[str]]:
    """Runs of consecutive normalised content words; stop-words and punctuation end a run."""
    runs: list[list[str]] = []
    for segment in SEGMENT_SPLIT.split((text or "").lower()):
        current: list[str] = []
        for token in tokenize(segment):
            if is_content_word(token):
                current.append(stem(token))
            elif current:
                runs.append(current)
                current = []
        if current:
            runs.append(current)
    return runs
