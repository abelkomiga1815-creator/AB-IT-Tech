/** Shapes returned by the Spring Boot API (see backend docs/api/API-REFERENCE.md). */

export type Role = "STUDENT" | "CUSTOMER" | "ADMIN";

export interface User {
  id: string;
  fullName: string;
  email: string;
  role: Role;
  enabled: boolean;
}

export interface StudentProfile {
  id: string;
  university: string;
  programme: string;
  yearOfStudy: number | null;
  studentNumber: string | null;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

export type ProjectStatus = "PLANNING" | "IN_PROGRESS" | "COMPLETED" | "ARCHIVED";

export type ProjectStage =
  | "TOPIC_SELECTION"
  | "PROPOSAL"
  | "LITERATURE_REVIEW"
  | "METHODOLOGY"
  | "IMPLEMENTATION"
  | "WRITING"
  | "REVIEW"
  | "DEFENSE_PREPARATION"
  | "SUBMISSION";

export interface Project {
  id: string;
  title: string;
  description: string | null;
  researchArea: string | null;
  supervisorName: string | null;
  deadline: string | null;
  status: ProjectStatus;
  currentStage: ProjectStage | null;
  progressPercent: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProgressEntry {
  id: string;
  stage: ProjectStage;
  percentComplete: number;
  note: string | null;
  createdAt: string;
}

export type DocumentType = "PROPOSAL" | "THESIS_DRAFT" | "REFERENCE" | "DATA" | "PRESENTATION" | "OTHER";

export interface ProjectDocument {
  id: string;
  type: DocumentType;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  description: string | null;
  createdAt: string;
}

export type DefenseCategory = "GENERAL" | "METHODOLOGY" | "LITERATURE" | "RESULTS" | "IMPLEMENTATION" | "OTHER";

export interface DefenseQuestion {
  id: string;
  question: string;
  answer: string | null;
  category: DefenseCategory;
  prepared: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Report produced by the AI research engine (see ai-service/docs/AI-SERVICE.md). */
export type SimilarityLevel = "low" | "moderate" | "high" | "insufficient_data";
export type SourceStatus = "ok" | "no_results" | "rate_limited" | "unavailable";

export interface SourceReport {
  provider: string;
  status: SourceStatus;
  papersFound: number;
  message: string;
}

export interface RelatedPaper {
  title: string;
  authors: string[];
  year: number | null;
  venue: string | null;
  doi: string | null;
  url: string | null;
  sources: string[];
  citationCount: number | null;
  similarityScore: number;
  sharedTerms: string[];
  basis: "title_and_abstract" | "title_only";
}

export interface GapEvidence {
  text: string;
  papers: { title: string; year: number | null; url: string | null }[];
}

export interface GapSuggestion {
  type: string;
  title: string;
  description: string;
  evidence: GapEvidence[];
  confidence: "low" | "medium";
}

export interface ResearchReport {
  generatedAt: string;
  retrieved: {
    queries: string[];
    sources: SourceReport[];
    papersFound: number;
    relatedWork: RelatedPaper[];
  };
  analysis: {
    keywords: { term: string; score: number; kind: string; inTitle: boolean }[];
    similarity: {
      level: SimilarityLevel;
      maxScore: number;
      papersCompared: number;
      papersWithAbstract: number;
      explanation: string;
      disclaimer: string;
    };
    gapAnalysis: { suggestions: GapSuggestion[]; caveat: string; note: string };
    relatedTerms: { term: string; papersMentioning: number }[];
    alternativeTitles: { title: string; rationale: string }[];
  };
  methodology: string[];
  notices: string[];
}

export interface AnalysisSummary {
  id: string;
  projectTitle: string;
  similarityLevel: SimilarityLevel;
  papersFound: number;
  createdAt: string;
}

export interface AnalysisDetail extends AnalysisSummary {
  report: ResearchReport;
}

export interface SavedSource {
  id: string;
  title: string;
  authors: string | null;
  year: number | null;
  venue: string | null;
  doi: string | null;
  url: string | null;
  provider: string | null;
  note: string | null;
  createdAt: string;
}

export interface LoginResult {
  user: User;
}
