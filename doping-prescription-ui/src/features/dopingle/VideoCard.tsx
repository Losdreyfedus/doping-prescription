import { tr } from '@/shared/i18n/tr';
import { PlayCircleIcon } from '@/shared/ui/icons';

import type { FeedCard } from './api';
import { formatClock } from './time';

/** One hue per topic for the simulated video's background (docs/arayuz.md, Simüle oynatıcı). */
const TOPIC_BACKGROUND: Record<number, string> = {
  1: 'from-topic-1',
  2: 'from-topic-2',
  3: 'from-topic-3',
  4: 'from-topic-4',
  5: 'from-topic-5',
  6: 'from-topic-6',
  7: 'from-topic-7',
  8: 'from-topic-8',
  9: 'from-topic-9',
  10: 'from-topic-10',
};

const TOPIC_DOT: Record<number, string> = {
  1: 'bg-topic-1',
  2: 'bg-topic-2',
  3: 'bg-topic-3',
  4: 'bg-topic-4',
  5: 'bg-topic-5',
  6: 'bg-topic-6',
  7: 'bg-topic-7',
  8: 'bg-topic-8',
  9: 'bg-topic-9',
  10: 'bg-topic-10',
};

interface Props {
  card: FeedCard;
  progress: number;
  elapsedSec: number;
  playing: boolean;
  onToggle: () => void;
}

/** The 9:16 simulated video with the project's additions: topic chip, reason pill and points badge. */
export function VideoCard({ card, progress, elapsedSec, playing, onToggle }: Props) {
  const { content, topic } = card;
  return (
    <div className="flex h-[88vh] flex-col gap-2">
      <button
        type="button"
        onClick={onToggle}
        aria-label={playing ? tr.dopingle.pause : tr.dopingle.play}
        className={`relative flex aspect-[9/16] h-full flex-col overflow-hidden rounded-2xl bg-gradient-to-b ${TOPIC_BACKGROUND[topic.topicId] ?? 'from-navy'} to-dopingle-bg text-left text-surface focus-visible:outline-2 focus-visible:outline-select`}
      >
        <span className="absolute top-4 left-4 rounded-full bg-surface/90 px-3 py-1 text-xs font-bold text-ink">
          {topic.name}
        </span>
        <span
          className="absolute top-3 right-4 font-display text-lg font-bold text-surface/80"
          aria-hidden="true"
        >
          dopi<span className="text-brand-red">shorts</span>
        </span>
        <span className="flex flex-1 flex-col items-center justify-center gap-6 px-8 text-center">
          <span className="font-display text-4xl font-bold [text-shadow:0_2px_8px_rgb(0_0_0/0.6)]">
            {topic.name}
          </span>
          <span className="text-xl font-semibold [text-shadow:0_2px_6px_rgb(0_0_0/0.7)]">
            {content.title}
          </span>
          <span
            aria-hidden="true"
            className="flex size-20 items-center justify-center rounded-full border-4 border-surface/70 bg-surface/15 font-display text-4xl font-bold"
          >
            {content.teacherName.charAt(0).toLocaleUpperCase('tr')}
          </span>
        </span>
        {!playing && progress < 1 && (
          <span
            aria-hidden="true"
            className="absolute inset-0 m-auto flex size-16 items-center justify-center rounded-full bg-surface/30 [&_svg]:size-8"
          >
            <PlayCircleIcon />
          </span>
        )}
        <span className="mx-4 mb-4 flex flex-col gap-2">
          <span
            data-testid="reason"
            className="flex w-fit items-center gap-2 rounded-full bg-dopingle-bg/75 px-3 py-1.5 text-sm font-semibold"
          >
            <span
              aria-hidden="true"
              className={`size-2.5 rounded-full ${TOPIC_DOT[topic.topicId] ?? 'bg-surface'}`}
            />
            {card.reasonText}
          </span>
          <span className="self-end rounded-full bg-orange px-3 py-1 text-sm font-bold">
            {tr.dopingle.points(card.pointsAvailable)}
          </span>
        </span>
      </button>
      <div className="flex flex-col gap-1">
        <div
          role="progressbar"
          aria-label={tr.dopingle.progress}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(progress * 100)}
          className="h-1 rounded-full bg-surface/20"
        >
          <div className="h-full rounded-full bg-select" style={{ width: `${String(progress * 100)}%` }} />
        </div>
        <div className="flex justify-between text-xs text-band-unknown">
          <span>{formatClock(elapsedSec)}</span>
          <span>{formatClock(content.durationSec)}</span>
        </div>
      </div>
    </div>
  );
}
