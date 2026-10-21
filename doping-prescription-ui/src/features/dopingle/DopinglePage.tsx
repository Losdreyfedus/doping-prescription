import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { BookmarkIcon, ChevronDownIcon, ChevronUpIcon, HeartIcon, SpeakerIcon } from '@/shared/ui/icons';

import { type FeedCard, useFeed, useInteractionRecorder } from './api';
import { useSimulatedPlayer } from './useSimulatedPlayer';
import { VideoCard } from './VideoCard';

const DEFAULT_SPEED = 4;
/** docs/arayuz.md: the next page loads when 3 cards are left. */
const PREFETCH_REMAINING = 3;
/** docs/arayuz.md: changing video within 3 seconds (wall clock) of starting counts as a skip. */
const SKIP_WINDOW_MS = 3000;
const WHEEL_COOLDOWN_MS = 600;
const AUTO_NEXT_DELAY_MS = 800;

function toTopicId(value: string | null): number | undefined {
  const parsed = Number(value);
  return value && Number.isInteger(parsed) ? parsed : undefined;
}

/** Route /app/dopingle inside the Dopingle layer (docs/arayuz.md, Dopingle). */
export function DopinglePage() {
  const [params] = useSearchParams();
  const topicId = toTopicId(params.get('topicId'));
  // Every opening is a new session; ?refresh=1 needs nothing extra (docs/api.md).
  const [sessionKey] = useState(() => crypto.randomUUID());
  const feed = useFeed(topicId, sessionKey);
  const cards = feed.data?.pages.flatMap((page) => page.cards) ?? [];
  const speed = feed.data?.pages[0]?.demoVideoSpeed ?? DEFAULT_SPEED;
  const [index, setIndex] = useState(0);
  const current = cards.at(index);

  const { hasNextPage, isFetchingNextPage, fetchNextPage } = feed;
  useEffect(() => {
    if (
      cards.length > 0 &&
      cards.length - index <= PREFETCH_REMAINING &&
      hasNextPage &&
      !isFetchingNextPage
    ) {
      void fetchNextPage();
    }
  }, [cards.length, index, hasNextPage, isFetchingNextPage, fetchNextPage]);

  const go = (delta: number) => {
    setIndex((position) => Math.max(0, Math.min(cards.length - 1, position + delta)));
  };

  if (feed.isPending) {
    return <p className="flex h-full items-center justify-center text-band-unknown">{tr.dopingle.loading}</p>;
  }
  if (feed.isError) {
    return (
      <p role="alert" className="flex h-full items-center justify-center">
        {feed.error.message}
      </p>
    );
  }
  if (!current) {
    return <p className="flex h-full items-center justify-center text-band-unknown">{tr.dopingle.empty}</p>;
  }
  return <Player key={current.impressionId} card={current} speed={speed} onMove={go} />;
}

interface PlayerProps {
  card: FeedCard;
  speed: number;
  onMove: (delta: number) => void;
}

/** One card on screen: playback, keyboard and wheel navigation, actions and interaction events. */
function Player({ card, speed, onMove }: PlayerProps) {
  const { record } = useInteractionRecorder();
  const [liked, setLiked] = useState(false);
  const [autoNext, setAutoNext] = useState(true);
  const startedAt = useRef(0);
  const completed = useRef(false);
  const lastWheel = useRef(0);

  const onComplete = useCallback(() => {
    completed.current = true;
    record(card.impressionId, 'VIEW_COMPLETE', card.content.durationSec * 1000);
  }, [record, card]);
  const player = useSimulatedPlayer({
    durationSec: card.content.durationSec,
    speed,
    active: true,
    onComplete,
  });

  useEffect(() => {
    startedAt.current = Date.now();
    record(card.impressionId, 'VIEW_START', 0);
  }, [record, card.impressionId]);

  const elapsedRef = useRef(0);
  useEffect(() => {
    elapsedRef.current = player.elapsedSec;
  }, [player.elapsedSec]);
  const move = useCallback(
    (delta: number) => {
      if (!completed.current && Date.now() - startedAt.current < SKIP_WINDOW_MS) {
        record(card.impressionId, 'SKIP', Math.round(elapsedRef.current * 1000));
      }
      onMove(delta);
    },
    [record, card.impressionId, onMove],
  );

  useEffect(() => {
    if (player.progress < 1 || !autoNext) {
      return;
    }
    const timer = setTimeout(() => {
      move(1);
    }, AUTO_NEXT_DELAY_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [player.progress, autoNext, move]);

  const { toggle } = player;
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        move(event.key === 'ArrowDown' ? 1 : -1);
      } else if (event.key === ' ') {
        event.preventDefault();
        toggle();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('keydown', onKey);
    };
  }, [move, toggle]);

  return (
    <div
      className="relative flex h-full items-center justify-center"
      onWheel={(event) => {
        const now = Date.now();
        if (now - lastWheel.current > WHEEL_COOLDOWN_MS && Math.abs(event.deltaY) > 10) {
          lastWheel.current = now;
          move(event.deltaY > 0 ? 1 : -1);
        }
      }}
    >
      <div className="absolute top-5 right-20 flex flex-col items-end gap-2">
        <label className="flex items-center gap-2 text-xs">
          {tr.dopingle.autoNext}
          <input
            type="checkbox"
            checked={autoNext}
            onChange={() => {
              setAutoNext((value) => !value);
            }}
            className="h-4 w-8 cursor-pointer appearance-none rounded-full bg-surface/30 checked:bg-select"
          />
        </label>
      </div>

      <div className="flex items-end gap-4">
        <VideoCard
          card={card}
          progress={player.progress}
          elapsedSec={player.elapsedSec}
          playing={player.playing}
          onToggle={toggle}
        />
        <div className="mb-24 flex flex-col items-center gap-4">
          <button
            type="button"
            aria-pressed={liked}
            aria-label={tr.dopingle.like}
            onClick={() => {
              if (!liked) {
                record(card.impressionId, 'LIKE');
              }
              setLiked(true);
            }}
            className={`flex size-11 items-center justify-center rounded-full bg-surface/15 ${liked ? 'text-danger' : ''}`}
          >
            <HeartIcon filled={liked} />
          </button>
          <span className="-mt-3 text-xs">{liked ? 1 : 0}</span>
          <span
            role="img"
            aria-label={tr.dopingle.save}
            className="flex size-11 items-center justify-center rounded-full bg-surface/15"
          >
            <BookmarkIcon />
          </span>
          <span
            role="img"
            aria-label={tr.dopingle.sound}
            className="flex size-11 items-center justify-center rounded-full bg-surface/15"
          >
            <SpeakerIcon />
          </span>
        </div>
      </div>

      <div className="absolute right-5 flex flex-col gap-3">
        <button
          type="button"
          aria-label={tr.dopingle.previous}
          onClick={() => {
            move(-1);
          }}
          className="flex size-11 items-center justify-center rounded-full bg-surface/15 hover:bg-surface/25"
        >
          <ChevronUpIcon />
        </button>
        <button
          type="button"
          aria-label={tr.dopingle.next}
          onClick={() => {
            move(1);
          }}
          className="flex size-11 items-center justify-center rounded-full bg-surface/15 hover:bg-surface/25"
        >
          <ChevronDownIcon />
        </button>
      </div>

      <div className="absolute bottom-6 left-4 flex flex-col gap-1">
        <p className="font-bold">{card.content.title}</p>
        <span className="w-fit rounded-full bg-surface/20 px-3 py-0.5 text-xs">
          {card.content.teacherName}
        </span>
      </div>
    </div>
  );
}
