import { Link, useLocation } from 'react-router';

import type { TopicProfile } from '@/shared/api/learnerProfile';
import { tr } from '@/shared/i18n/tr';

/** Band colours come from the API's band; the UI never applies thresholds itself (docs/arayuz.md). */
const BAND_COLOR: Record<TopicProfile['band'], string> = {
  WEAK: 'bg-band-weak',
  MEDIUM: 'bg-band-medium',
  STRONG: 'bg-band-strong',
  UNKNOWN: 'bg-band-unknown',
};

const TREND: Record<TopicProfile['trend'], { arrow: string; className: string; label: string }> = {
  UP: { arrow: '↑', className: 'text-success', label: tr.progress.trendUp },
  DOWN: { arrow: '↓', className: 'text-danger', label: tr.progress.trendDown },
  FLAT: { arrow: '→', className: 'text-surface/50', label: tr.progress.trendFlat },
};

export function TopicMasteryRow({ topic }: { topic: TopicProfile }) {
  const unknown = topic.band === 'UNKNOWN';
  const percent = Math.round(topic.mastery * 100);
  const trend = TREND[topic.trend];
  const location = useLocation();

  return (
    <li
      data-band={topic.band}
      className={`flex items-center gap-5 rounded-card border border-surface/10 bg-navy/70 px-5 py-3.5 ${unknown ? 'opacity-50' : ''}`}
    >
      <span className="w-52 font-semibold">{topic.name}</span>
      {unknown ? (
        <span className="flex-1 text-surface/60">{tr.progress.noData}</span>
      ) : (
        <>
          <div
            role="meter"
            aria-label={tr.progress.masteryOf(topic.name)}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={percent}
            className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface/15"
          >
            <div
              className={`h-full origin-left animate-bar-fill rounded-full ${BAND_COLOR[topic.band]}`}
              style={{ width: `${String(percent)}%` }}
            />
          </div>
          <span className="w-12 text-right font-bold">%{percent}</span>
          <span className={`w-4 text-lg font-bold ${trend.className}`}>
            <span aria-hidden="true">{trend.arrow}</span>
            <span className="sr-only">{trend.label}</span>
          </span>
        </>
      )}
      <span className="w-40 text-xs text-surface/60">{tr.progress.readyVideos(topic.readyContentCount)}</span>
      <Link
        to={`/app/dopingle?topicId=${String(topic.topicId)}`}
        // closing Dopingle returns to Gelişimim (docs/arayuz.md)
        state={{ from: location.pathname }}
        className="rounded-button border border-surface/25 bg-ink px-5 py-2 font-semibold text-surface hover:border-surface/60"
        aria-label={tr.progress.studyTopic(topic.name)}
      >
        {tr.progress.study}
      </Link>
    </li>
  );
}
