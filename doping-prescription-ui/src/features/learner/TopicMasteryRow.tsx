import { Link } from 'react-router';

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
  UP: { arrow: '↑', className: 'text-success', label: tr.weakTopics.trendUp },
  DOWN: { arrow: '↓', className: 'text-danger', label: tr.weakTopics.trendDown },
  FLAT: { arrow: '→', className: 'text-text-muted', label: tr.weakTopics.trendFlat },
};

export function TopicMasteryRow({ topic }: { topic: TopicProfile }) {
  const unknown = topic.band === 'UNKNOWN';
  const percent = Math.round(topic.mastery * 100);
  const trend = TREND[topic.trend];

  return (
    <li
      data-band={topic.band}
      className={`flex items-center gap-6 rounded-card bg-surface px-6 py-4 shadow-sm ${unknown ? 'opacity-60' : ''}`}
    >
      <span className="w-56 font-semibold">{topic.name}</span>
      {unknown ? (
        <span className="flex-1 text-text-muted">{tr.weakTopics.noData}</span>
      ) : (
        <>
          <div
            role="meter"
            aria-label={tr.weakTopics.masteryOf(topic.name)}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={percent}
            className="h-3 flex-1 overflow-hidden rounded-full bg-surface-muted"
          >
            <div
              className={`h-full origin-left animate-bar-fill rounded-full ${BAND_COLOR[topic.band]}`}
              style={{ width: `${String(percent)}%` }}
            />
          </div>
          <span className="w-12 text-right font-bold">%{percent}</span>
          <span aria-label={trend.label} className={`w-4 text-lg font-bold ${trend.className}`}>
            {trend.arrow}
          </span>
        </>
      )}
      <span className="w-44 text-sm text-text-muted">
        {tr.weakTopics.readyVideos(topic.readyContentCount)}
      </span>
      <Link
        to={`/app/dopingle?topicId=${String(topic.topicId)}`}
        className="rounded-button bg-ink px-5 py-2 font-semibold text-surface"
        aria-label={tr.weakTopics.studyTopic(topic.name)}
      >
        {tr.weakTopics.study}
      </Link>
    </li>
  );
}
