"""Renders the report dictionary as Markdown, for download or printing."""
from __future__ import annotations

from typing import Any


def _link(title: str, url: str | None) -> str:
    return f"[{title}]({url})" if url else title


def render_markdown(report: dict[str, Any]) -> str:
    lines: list[str] = []
    add = lines.append
    add(f"# Research report: {report['project']['title']}")
    add("")
    add(f"Generated: {report['generatedAt']}")
    add("")
    add("> " + report["notices"][0])
    add("")

    retrieved = report["retrieved"]
    add("## Part 1. Retrieved from the sources")
    add("")
    add("### Sources searched")
    for source in retrieved["sources"]:
        extra = f" - {source['message']}" if source.get("message") else ""
        add(f"- **{source['provider']}**: {source['status']}, {source['papersFound']} paper(s){extra}")
    add("")
    add("Queries used: " + "; ".join(f"`{q}`" for q in retrieved["queries"]) if retrieved["queries"] else "Queries used: none")
    add("")
    add(f"### Related work ({retrieved['papersFound']} paper(s) found, best matches first)")
    if not retrieved["relatedWork"]:
        add("")
        add("No papers were retrieved.")
    for paper in retrieved["relatedWork"]:
        meta = ", ".join(str(x) for x in (paper.get("year"), paper.get("venue")) if x)
        authors = ", ".join(paper.get("authors") or [])
        add("")
        add(f"- {_link(paper['title'], paper.get('url'))}" + (f" ({meta})" if meta else ""))
        if authors:
            add(f"  - Authors: {authors}")
        add(f"  - Similarity to your project: {paper['similarityScore']:.2f} ({paper['basis'].replace('_', ' ')}); "
            f"found in: {', '.join(paper['sources'])}")
        if paper.get("sharedTerms"):
            add(f"  - Shared terms: {', '.join(paper['sharedTerms'])}")
    add("")

    analysis = report["analysis"]
    add("## Part 2. Analysis by this tool (interpretation, not fact)")
    add("")
    add("### Key terms")
    add(", ".join(f"{k['term']}" for k in analysis["keywords"]) or "None extracted.")
    add("")
    similarity = analysis["similarity"]
    add("### Similarity indicator")
    add(f"**Level: {similarity['level'].replace('_', ' ')}** (highest score {similarity['maxScore']:.2f}; "
        f"{similarity['papersCompared']} papers compared, {similarity['papersWithAbstract']} with abstracts)")
    add("")
    add(similarity["explanation"])
    add("")
    gaps = analysis["gapAnalysis"]
    add("### Possible research gaps")
    if gaps.get("note"):
        add(gaps["note"])
    elif not gaps["suggestions"]:
        add("No gap suggestions could be supported by the retrieved papers.")
    for suggestion in gaps["suggestions"]:
        add("")
        add(f"- **{suggestion['title']}** (confidence: {suggestion['confidence']})")
        add(f"  - {suggestion['description']}")
        for evidence in suggestion["evidence"]:
            add(f"  - Evidence: {evidence['text']}")
            for ref in evidence["papers"]:
                add(f"    - {_link(ref['title'], ref.get('url'))}" + (f" ({ref['year']})" if ref.get("year") else ""))
    add("")
    add(f"_{gaps['caveat']}_")
    add("")
    add("### Terms common in related papers but missing from your text")
    terms = analysis["relatedTerms"]
    add(", ".join(f"{t['term']} ({t['papersMentioning']})" for t in terms) if terms else "None found.")
    add("")
    add("### Alternative title ideas (templates to discuss with your supervisor)")
    for title in analysis["alternativeTitles"]:
        add(f"- {title['title']}  \n  _{title['rationale']}_")
    if not analysis["alternativeTitles"]:
        add("None.")
    add("")
    add("## Method")
    for step in report["methodology"]:
        add(f"- {step}")
    add("")
    add("## Notices")
    for notice in report["notices"]:
        add(f"- {notice}")
    add("")
    return "\n".join(lines)
