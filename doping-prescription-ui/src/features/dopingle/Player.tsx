import { useCallback, useEffect, useRef, useState } from 'react';

import { tr } from '@/shared/i18n/tr';
import { ChevronDownIcon, ChevronUpIcon } from '@/shared/ui/icons';

import { ActionRail } from './ActionRail';
import { type FeedCard, useInteractionRecorder } from './api';
import { MicroQuizPanel } from './MicroQuizPanel';
import { useAutoNext } from './useAutoNext';
import { useFeedNavigation } from './useFeedNavigation';
import { useImpression } from './useImpression';
import { useQuizFlow } from './useQuizFlow';
import { useRealVideo } from './useRealVideo';
import { useSimulatedPlayer } from './useSimulatedPlayer';
import { VideoCard } from './VideoCard';

/** docs/arayuz.md: the "Soruyu çöz" button appears once 70% of the video has played. */
const QUIZ_BUTTON_AT = 0.7;

interface Props {
  card: FeedCard;
  speed: number;
  onMove: (delta: number) => void;
  /** Sound is chosen once for the whole Dopingle session, not per card. */
  muted: boolean;
  onMutedChange: (muted: boolean) => void;
  onToggleSave: (card: FeedCard) => void;
  saving: boolean;
}

/** One card on screen: playback, keyboard and wheel navigation, actions and interaction events. */
export function Player({ card, speed, onMove, muted, onMutedChange, onToggleSave, saving }: Props) {
  const { record, flush } = useInteractionRecorder();
  const elapsedRef = useRef(0);
  const impression = useImpression({ impressionId: card.impressionId, record, elapsedRef });
  const quiz = useQuizFlow({ impressionId: card.impressionId, record, flush, elapsedRef });
  const { showQuiz } = quiz;

  const { complete } = impression;
  const onComplete = useCallback(() => {
    elapsedRef.current = card.content.durationSec;
    complete(card.content.durationSec);
    if (card.hasMicroQuiz) {
      void showQuiz();
    }
  }, [complete, card.content.durationSec, card.hasMicroQuiz, showQuiz]);

  // A real file plays when the card has one and it loads; otherwise the simulated player stands in (also in CI).
  const [videoFailed, setVideoFailed] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const realVideo = Boolean(card.content.videoUrl) && !videoFailed;
  const onMutedByBrowser = useCallback(() => {
    onMutedChange(true);
  }, [onMutedChange]);
  const simulated = useSimulatedPlayer({
    durationSec: card.content.durationSec,
    speed,
    active: !quiz.quizActive && !realVideo,
    onComplete,
  });
  const real = useRealVideo({
    videoRef,
    durationSec: card.content.durationSec,
    active: !quiz.quizActive && realVideo,
    muted,
    onMutedByBrowser,
    onComplete,
  });
  const player = realVideo ? real : simulated;

  useEffect(() => {
    elapsedRef.current = player.elapsedSec;
  }, [player.elapsedSec]);

  const { move, onWheel } = useFeedNavigation({
    onMove,
    onLeave: impression.leave,
    toggle: player.toggle,
    keyboardEnabled: !quiz.quizActive,
  });
  const { autoNext, toggleAutoNext } = useAutoNext({
    finished: player.progress >= 1 && (!card.hasMicroQuiz || quiz.quizSettled),
    move,
  });

  return (
    <div className="relative flex h-full items-center justify-center" onWheel={onWheel}>
      <div className="absolute top-5 right-20 flex flex-col items-end gap-2">
        <label className="flex items-center gap-2 text-xs">
          {tr.dopingle.autoNext}
          <input
            type="checkbox"
            checked={autoNext}
            onChange={toggleAutoNext}
            className="h-4 w-8 cursor-pointer appearance-none rounded-full bg-surface/30 checked:bg-select"
          />
        </label>
      </div>

      <div className="flex items-center gap-4">
        <div className={`relative transition-opacity ${quiz.quizActive ? 'opacity-40' : ''}`}>
          <VideoCard
            card={card}
            progress={player.progress}
            elapsedSec={player.elapsedSec}
            playing={player.playing}
            onToggle={player.toggle}
            videoRef={realVideo ? videoRef : undefined}
            onVideoError={() => {
              setVideoFailed(true);
            }}
          />
          {card.hasMicroQuiz && quiz.quizState.status === 'closed' && player.progress >= QUIZ_BUTTON_AT && (
            <button
              type="button"
              onClick={() => {
                void showQuiz();
              }}
              className="absolute -bottom-12 left-1/2 -translate-x-1/2 rounded-button bg-orange px-5 py-2 font-semibold"
            >
              {tr.quiz.open}
            </button>
          )}
        </div>
        <ActionRail
          onLike={() => {
            record(card.impressionId, 'LIKE');
          }}
          saved={card.saved}
          saving={saving}
          onToggleSave={() => {
            onToggleSave(card);
          }}
          muted={muted}
          soundAvailable={realVideo}
          onToggleSound={() => {
            onMutedChange(!muted);
          }}
        />
        {quiz.quizState.status === 'open' && (
          <MicroQuizPanel
            quiz={quiz.quizState.quiz}
            topicName={card.topic.name}
            onContinue={() => {
              quiz.dismiss();
              move(1);
            }}
          />
        )}
      </div>

      {quiz.quizState.status === 'error' && (
        <p
          role="alert"
          className="absolute bottom-6 left-1/2 -translate-x-1/2 rounded-button bg-surface/15 px-4 py-2"
        >
          {quiz.quizState.message}
        </p>
      )}

      <NavigationButtons move={move} />

      <div className="absolute bottom-6 left-4 flex flex-col gap-1">
        <p className="font-bold">{card.content.title}</p>
        <span className="w-fit rounded-full bg-surface/20 px-3 py-0.5 text-xs">
          {card.content.teacherName}
        </span>
      </div>
    </div>
  );
}

/** The ↑/↓ buttons beside the card, for mouse users (the keyboard and the wheel do the same). */
function NavigationButtons({ move }: { move: (delta: number) => void }) {
  return (
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
  );
}
