import { clampPercent } from "@/lib/format";

export function ProgressBar({ percent, label }: { percent: number; label?: string }) {
  const value = clampPercent(percent);
  return (
    <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={value} aria-label={label ?? "Progress"}>
      <div className="progress-fill" style={{ width: `${value}%` }} />
      <span className="progress-text">{value}%</span>
    </div>
  );
}
