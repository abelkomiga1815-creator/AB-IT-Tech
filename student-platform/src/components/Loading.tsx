export function Loading({ text = "Loading..." }: { text?: string }) {
  return <p className="muted" role="status">{text}</p>;
}
