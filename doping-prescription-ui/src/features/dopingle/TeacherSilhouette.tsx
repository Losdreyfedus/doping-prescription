import { useId } from 'react';

/**
 * A plain teacher silhouette drawn for the simulated player (no downloaded images): head and shoulders in dark grey
 * with a thin rim light in `currentColor`, so the topic colour only touches the edges.
 */
export function TeacherSilhouette({ className = '' }: { className?: string }) {
  // SVG ids are document-wide; a unique one keeps two silhouettes on screen from sharing a gradient.
  const fillId = useId();
  return (
    <svg viewBox="0 0 200 240" aria-hidden="true" className={className}>
      <defs>
        <linearGradient id={fillId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="var(--navy)" />
          <stop offset="1" stopColor="var(--dopingle-bg)" />
        </linearGradient>
      </defs>
      <g fill={`url(#${fillId})`} stroke="currentColor" strokeOpacity="0.4" strokeWidth="1.5">
        {/* shoulders and torso */}
        <path d="M18 240c4-46 30-74 64-82l18 18 18-18c34 8 60 36 64 82z" />
        {/* neck */}
        <path d="M86 132h28v28l-14 14-14-14z" />
        {/* head with a simple hair line */}
        <ellipse cx="100" cy="92" rx="38" ry="46" />
        <path d="M62 86c2-30 22-44 38-44s36 14 38 44c-10-14-24-20-38-20s-28 6-38 20z" />
      </g>
    </svg>
  );
}
