import { PROJECT_STATUSES, labelOf } from "@/lib/constants";
import type { ProjectStatus } from "@/lib/types";

export function StatusBadge({ status }: { status: ProjectStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{labelOf(PROJECT_STATUSES, status)}</span>;
}
