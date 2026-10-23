import type { RefObject } from 'react';

import { tr } from '@/shared/i18n/tr';
import { PlayCircleIcon } from '@/shared/ui/icons';

import type { FeedCard } from './api';
import { TeacherSilhouette } from './TeacherSilhouette';
import { formatClock } from './time';

/** The topic colour as a thin accent only: the card's edge light and the silhouette's rim (docs/arayuz.md). */
const TOPIC_ACCENT: Record<number, { edge: string; rim: string }> = {
  1: { edge: 'border-topic-1/60', rim: 'text-topic-1' },
  2: { edge: 'border-topic-2/60', rim: 'text-topic-2' },
  3: { edge: 'border-topic-3/60', rim: 'text-topic-3' },
  4: { edge: 'border-topic-4/60', rim: 'text-topic-4' },
  5: { edge: 'border-topic-5/60', rim: 'text-topic-5' },
  6: { edge: 'border-topic-6/60', rim: 'text-topic-6' },
  7: { edge: 'border-topic-7/60', rim: 'text-topic-7' },
  8: { edge: 'border-topic-8/60', rim: 'text-topic-8' },
  9: { edge: 'border-topic-9/60', rim: 'text-topic-9' },
  10: { edge: 'border-topic-10/60', rim: 'text-topic-10' },
};
const NO_ACCENT = { edge: 'border-surface/20', rim: 'text-surface' };

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
  /** Set when the card plays its real file; the simulated scene is not drawn then. */
  videoRef?: RefObject<HTMLVideoElement | null> | undefined;
  onVideoError?: () => void;
}

/**
 * The 9:16 simulated video, styled like Doping's short videos: a dark, softly blurred classroom, a teacher silhouette and a
 * burned-in subtitle, plus the project's topic chip, reason pill and points badge.
 */
export function VideoCard({ card, progress, elapsedSec, playing, onToggle, videoRef, onVideoError }: Props) {
  const { content, topic } = card;
  const accent = TOPIC_ACCENT[topic.topicId] ?? NO_ACCENT;
  const realVideo = videoRef !== undefined && content.videoUrl !== null;
  return (
    <div className="flex h-[88vh] flex-col gap-2">
      <button
        type="button"
        onClick={onToggle}
        aria-label={playing ? tr.dopingle.pause : tr.dopingle.play}
        className={`relative flex aspect-[9/16] h-full flex-col overflow-hidden rounded-2xl border-2 ${accent.edge} bg-gradient-to-b from-navy-end via-ink to-dopingle-bg text-left text-surface focus-visible:outline-2 focus-visible:outline-select`}
      >
        {realVideo ? (
          <video
            ref={videoRef}
            src={content.videoUrl ?? undefined}
            preload="auto"
            playsInline
            onError={onVideoError}
            className="absolute inset-0 size-full object-cover"
          >
            {/* Doping's videos carry burned-in subtitles */}
            <track kind="captions" />
          </video>
        ) : (
          <SimulatedScene accent={accent} />
        )}
        <span className="absolute top-4 left-4 rounded-full bg-surface/90 px-3 py-1 text-xs font-bold text-ink">
          {topic.name}
        </span>
        <span
          className="absolute top-3 right-4 font-display text-lg font-bold text-surface/80"
          aria-hidden="true"
        >
          dopi<span className="text-brand-red">shorts</span>
        </span>
        <span className="flex-1" />
        {!playing && progress < 1 && (
          <span
            aria-hidden="true"
            className="absolute inset-0 m-auto flex size-16 items-center justify-center rounded-full bg-surface/30 [&_svg]:size-8"
          >
            <PlayCircleIcon />
          </span>
        )}
        <span className="relative mx-4 mb-4 flex flex-col gap-2">
          {/* burned-in subtitle like Doping's own videos: topic in yellow, title in white; real videos carry their own */}
          {!realVideo && (
            <span className="mb-2 flex flex-col items-center gap-1 text-center font-display leading-tight font-bold [text-shadow:0_2px_0_rgb(0_0_0/0.9),0_0_12px_rgb(0_0_0/0.8)]">
              <span className="text-2xl text-sun uppercase">{topic.name}</span>
              <span className="text-xl">{content.title}</span>
            </span>
          )}
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

/** The simulated video's scene: a softly blurred classroom (board, window light, shelf) and the teacher. */
function SimulatedScene({ accent }: { accent: { rim: string } }) {
  return (
    <>
      <span aria-hidden="true" className="absolute inset-0 blur-xl">
        <span className="absolute top-[12%] left-[10%] h-[22%] w-[80%] rounded-lg bg-surface/25" />
        <span className="absolute top-[8%] right-[-10%] size-[45%] rounded-full bg-sun/10" />
        <span className="absolute bottom-[30%] left-[-5%] h-[8%] w-[45%] rounded bg-orange/10" />
      </span>
      <span
        aria-hidden="true"
        className="absolute inset-0 bg-gradient-to-t from-dopingle-bg via-transparent to-transparent"
      />
      <TeacherSilhouette
        className={`absolute bottom-[14%] left-1/2 h-[58%] -translate-x-1/2 ${accent.rim} drop-shadow-[0_0_8px_currentColor]`}
      />
    </>
  );
}
