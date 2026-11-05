import type { SimilarityLevel, SourceStatus } from "./types";

export const SIMILARITY_LABELS: Record<SimilarityLevel, string> = {
  low: "Low",
  moderate: "Moderate",
  high: "High",
  insufficient_data: "Not enough data",
};

export const SOURCE_STATUS_LABELS: Record<SourceStatus, string> = {
  ok: "Answered",
  no_results: "Answered, no matching papers",
  rate_limited: "Rate limited (results may be incomplete)",
  unavailable: "Unavailable (results may be incomplete)",
};

export const PROVIDER_LABELS: Record<string, string> = {
  openalex: "OpenAlex",
  crossref: "Crossref",
  semantic_scholar: "Semantic Scholar",
};

export function providerLabel(provider: string): string {
  return PROVIDER_LABELS[provider] ?? provider;
}

/** True when at least one source had a problem, so the student should be told results may be incomplete. */
export function hasSourceProblems(sources: { status: SourceStatus }[]): boolean {
  return sources.some((source) => source.status === "rate_limited" || source.status === "unavailable");
}

export function formatScore(score: number): string {
  return Number.isFinite(score) ? score.toFixed(2) : "";
}
