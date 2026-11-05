"use client";

import { Alert } from "@/components/Alert";
import { formatScore, hasSourceProblems, providerLabel, SIMILARITY_LABELS, SOURCE_STATUS_LABELS } from "@/lib/research";
import { safeHttpUrl } from "@/lib/safe-url";
import type { RelatedPaper, ResearchReport } from "@/lib/types";

interface ReportViewProps {
  report: ResearchReport;
  /** Called when the student saves a paper; omit to hide the save buttons. */
  onSave?: (paper: RelatedPaper) => void;
  savedTitles?: Set<string>;
}

function PaperLink({ title, url }: { title: string; url: string | null }) {
  const href = safeHttpUrl(url);
  return href ? (
    <a href={href} target="_blank" rel="noopener noreferrer">{title}</a>
  ) : (
    <span>{title}</span>
  );
}

/**
 * Shows an AI engine report. Retrieved facts (Part 1) and the tool's interpretation (Part 2) are kept
 * visibly apart, and the disclaimers and source problems are always shown.
 */
export function ReportView({ report, onSave, savedTitles }: ReportViewProps) {
  const { retrieved, analysis } = report;
  const similarity = analysis.similarity;

  return (
    <div className="report">
      {hasSourceProblems(retrieved.sources) && (
        <Alert kind="info">Some sources had problems, so these results may be incomplete. Run the analysis again later for fuller results.</Alert>
      )}

      <h3>Part 1. Found in the sources</h3>
      <ul className="source-status">
        {retrieved.sources.map((source) => (
          <li key={source.provider}>
            <strong>{providerLabel(source.provider)}</strong>: {SOURCE_STATUS_LABELS[source.status]}
            {source.papersFound > 0 ? ` (${source.papersFound} paper${source.papersFound === 1 ? "" : "s"})` : ""}
          </li>
        ))}
      </ul>

      {retrieved.relatedWork.length === 0 ? (
        <p className="muted">No papers were retrieved.</p>
      ) : (
        <ul className="paper-list">
          {retrieved.relatedWork.map((paper) => (
            <li key={`${paper.doi ?? paper.title}`}>
              <div className="row-between">
                <strong><PaperLink title={paper.title} url={paper.url} /></strong>
                {onSave && (
                  <button
                    type="button"
                    className="btn btn-outline btn-small"
                    disabled={savedTitles?.has(paper.title.toLowerCase())}
                    onClick={() => onSave(paper)}
                  >
                    {savedTitles?.has(paper.title.toLowerCase()) ? "Saved" : "Save to reading list"}
                  </button>
                )}
              </div>
              <p className="muted">
                {[paper.year, paper.venue, paper.authors.slice(0, 3).join(", ")].filter(Boolean).join(" · ")}
              </p>
              <p className="hint">
                Similarity to your project: {formatScore(paper.similarityScore)}
                {paper.basis === "title_only" ? " (title only)" : ""}
                {paper.sharedTerms.length > 0 ? ` · shared terms: ${paper.sharedTerms.join(", ")}` : ""}
              </p>
            </li>
          ))}
        </ul>
      )}

      <h3>Part 2. Analysis by this tool (interpretation, not fact)</h3>

      <div className={`similarity similarity-${similarity.level}`}>
        <p>
          <strong>Similarity indicator: {SIMILARITY_LABELS[similarity.level]}</strong>
          {similarity.papersCompared > 0 && (
            <span className="muted"> (highest score {formatScore(similarity.maxScore)}, {similarity.papersCompared} papers compared)</span>
          )}
        </p>
        <p>{similarity.explanation}</p>
        <p className="hint">{similarity.disclaimer}</p>
      </div>

      <h4>Possible research gaps</h4>
      {analysis.gapAnalysis.note && <p className="muted">{analysis.gapAnalysis.note}</p>}
      {!analysis.gapAnalysis.note && analysis.gapAnalysis.suggestions.length === 0 && (
        <p className="muted">No gap suggestions could be supported by the retrieved papers.</p>
      )}
      <ul className="gap-list">
        {analysis.gapAnalysis.suggestions.map((gap) => (
          <li key={`${gap.type}-${gap.title}`}>
            <strong>{gap.title}</strong> <span className="badge">confidence: {gap.confidence}</span>
            <p>{gap.description}</p>
            <ul>
              {gap.evidence.map((item, index) => (
                <li key={index}>
                  {item.text}
                  {item.papers.length > 0 && (
                    <ul>
                      {item.papers.map((paper) => (
                        <li key={paper.title}><PaperLink title={paper.title} url={paper.url} />{paper.year ? ` (${paper.year})` : ""}</li>
                      ))}
                    </ul>
                  )}
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
      <p className="hint">{analysis.gapAnalysis.caveat}</p>

      {analysis.relatedTerms.length > 0 && (
        <>
          <h4>Terms common in related papers but missing from your text</h4>
          <p>{analysis.relatedTerms.map((term) => term.term).join(", ")}</p>
        </>
      )}

      {analysis.alternativeTitles.length > 0 && (
        <>
          <h4>Alternative title ideas (to discuss with your supervisor)</h4>
          <ul>
            {analysis.alternativeTitles.map((item) => (
              <li key={item.title}>{item.title} <span className="hint">({item.rationale})</span></li>
            ))}
          </ul>
        </>
      )}

      <h4>Notices</h4>
      <ul className="hint">
        {report.notices.map((notice) => (
          <li key={notice}>{notice}</li>
        ))}
      </ul>
    </div>
  );
}
