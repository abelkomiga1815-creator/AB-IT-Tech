import type { DefenseCategory, DocumentType, ProjectStage, ProjectStatus } from "./types";

/** Stages in their natural order; must match the backend enum `ProjectStage`. */
export const PROJECT_STAGES: { value: ProjectStage; label: string }[] = [
  { value: "TOPIC_SELECTION", label: "Topic selection" },
  { value: "PROPOSAL", label: "Proposal" },
  { value: "LITERATURE_REVIEW", label: "Literature review" },
  { value: "METHODOLOGY", label: "Methodology" },
  { value: "IMPLEMENTATION", label: "Implementation" },
  { value: "WRITING", label: "Writing" },
  { value: "REVIEW", label: "Review" },
  { value: "DEFENSE_PREPARATION", label: "Defense preparation" },
  { value: "SUBMISSION", label: "Submission" },
];

export const PROJECT_STATUSES: { value: ProjectStatus; label: string }[] = [
  { value: "PLANNING", label: "Planning" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "COMPLETED", label: "Completed" },
  { value: "ARCHIVED", label: "Archived" },
];

export const DOCUMENT_TYPES: { value: DocumentType; label: string }[] = [
  { value: "PROPOSAL", label: "Proposal" },
  { value: "THESIS_DRAFT", label: "Thesis draft" },
  { value: "REFERENCE", label: "Reference" },
  { value: "DATA", label: "Data" },
  { value: "PRESENTATION", label: "Presentation" },
  { value: "OTHER", label: "Other" },
];

export const DEFENSE_CATEGORIES: { value: DefenseCategory; label: string }[] = [
  { value: "GENERAL", label: "General" },
  { value: "METHODOLOGY", label: "Methodology" },
  { value: "LITERATURE", label: "Literature" },
  { value: "RESULTS", label: "Results" },
  { value: "IMPLEMENTATION", label: "Implementation" },
  { value: "OTHER", label: "Other" },
];

/** Upload rules; the backend enforces the same limits (this is only for faster feedback). */
export const ALLOWED_UPLOAD_EXTENSIONS = [
  "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv", "txt", "md", "png", "jpg", "jpeg",
] as const;
export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

export function labelOf<T extends string>(options: { value: T; label: string }[], value: T | null | undefined): string {
  return options.find((option) => option.value === value)?.label ?? "";
}
