/**
 * A pulsing placeholder for a value that is still loading (docs/arayuz.md, shared/ui). Sighted users see the block,
 * screen readers hear `label`.
 */
export function Skeleton({ label, className = 'h-[1em] w-8' }: { label: string; className?: string }) {
  return (
    <span className={`inline-block animate-pulse rounded bg-current/25 align-middle ${className}`}>
      <span className="sr-only">{label}</span>
    </span>
  );
}
