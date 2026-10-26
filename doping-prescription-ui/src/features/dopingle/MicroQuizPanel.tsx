import { useEffect, useRef, useState } from 'react';

import { tr } from '@/shared/i18n/tr';
import { AnswerBubble, type BubbleState } from '@/shared/ui/AnswerBubble';

import { type MicroQuiz, type QuizResult, useAnswerQuiz } from './quiz';
import { useQuizTimer } from './useQuizTimer';

interface Props {
  quiz: MicroQuiz;
  topicName: string;
  onContinue: () => void;
}

/**
 * The micro-question panel next to the video card (docs/arayuz.md, Mikro-soru penceresi): clicking an option sends
 * the answer at once; the result shows the solution, the points and the mastery change. It takes focus when it opens,
 * and Escape does not close the Dopingle layer behind it (the question is left with Devam).
 */
export function MicroQuizPanel({ quiz, topicName, onContinue }: Props) {
  const answer = useAnswerQuiz();
  const [selected, setSelected] = useState<string | null>(null);
  // Once an option is chosen the clock stops: the answer is in flight, answered or failed, never "timed out".
  const { secondsLeft, timedOut } = useQuizTimer(quiz.timeLimitSec, selected === null);
  const shownAt = useRef(0);
  const panelRef = useRef<HTMLElement>(null);
  const result = answer.data;
  const finished = result !== undefined || timedOut || answer.isError;

  useEffect(() => {
    shownAt.current = Date.now();
    panelRef.current?.focus();
  }, []);

  useEffect(() => {
    // Runs in the capture phase, before the layer's own Escape handler, which skips handled events.
    const keepLayerOpen = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
      }
    };
    window.addEventListener('keydown', keepLayerOpen, { capture: true });
    return () => {
      window.removeEventListener('keydown', keepLayerOpen, { capture: true });
    };
  }, []);

  const choose = (key: string) => {
    if (selected || timedOut) {
      return;
    }
    setSelected(key);
    answer.mutate({ quiz, selectedKey: key, shownAt: shownAt.current });
  };

  return (
    <section
      ref={panelRef}
      tabIndex={-1}
      aria-label={tr.quiz.title}
      className="flex w-[26.25rem] flex-col gap-4 rounded-[1.25rem] bg-surface p-6 text-ink shadow-xl focus-visible:outline-2 focus-visible:outline-select"
    >
      <header className="flex items-center justify-between">
        <span className="rounded-full bg-surface-muted px-3 py-1 text-xs font-bold">{topicName}</span>
        {selected === null && !timedOut && (
          <span className="font-display text-xl font-bold">
            <span className="sr-only">{tr.quiz.timeLeft}: </span>
            {secondsLeft}
          </span>
        )}
      </header>
      <p className="text-lg font-semibold">{quiz.stem}</p>
      <ul className="flex flex-col gap-2">
        {quiz.options.map((option) => (
          <li key={option.key}>
            <button
              type="button"
              disabled={selected !== null || timedOut}
              aria-label={`${option.key} ${option.text}`}
              onClick={() => {
                choose(option.key);
              }}
              className="flex w-full items-center gap-3 rounded-button border border-border px-3 py-2 text-left hover:bg-surface-muted-active disabled:cursor-default"
            >
              <AnswerBubble letter={option.key} state={bubbleState(option.key, selected, result)} />
              {option.text}
            </button>
          </li>
        ))}
      </ul>
      {answer.isError && <p role="alert">{answer.error.message}</p>}
      {timedOut && <p role="status">{tr.quiz.timeUp}</p>}
      {result && <ResultView result={result} />}
      {finished && (
        <button
          type="button"
          onClick={onContinue}
          className="self-end rounded-button bg-ink px-6 py-2 font-semibold text-surface"
        >
          {tr.quiz.continue}
        </button>
      )}
    </section>
  );
}

function bubbleState(key: string, selected: string | null, result: QuizResult | undefined): BubbleState {
  if (!result) {
    return key === selected ? 'selected' : 'idle';
  }
  if (key === result.correctKey) {
    return 'correct';
  }
  return key === selected ? 'wrong' : 'idle';
}

function ResultView({ result }: { result: QuizResult }) {
  const { masteryChange } = result;
  const before = Math.round(masteryChange.before * 100);
  const after = Math.round(masteryChange.after * 100);
  // the bar starts at the old value and grows to the new one
  const [width, setWidth] = useState(before);
  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      setWidth(after);
    });
    return () => {
      cancelAnimationFrame(frame);
    };
  }, [after]);
  return (
    <div role="status" className="flex flex-col gap-3">
      <p className={`font-bold ${result.isCorrect ? 'text-success-strong' : 'text-danger-strong'}`}>
        {result.isCorrect ? tr.quiz.correct : tr.quiz.wrong}
        {result.pointsAwarded > 0 && (
          <span className="ml-3 inline-block animate-bounce rounded-full bg-orange px-3 py-0.5 text-surface">
            {tr.dopingle.points(result.pointsAwarded)}
          </span>
        )}
      </p>
      <p className="text-sm">{result.explanation}</p>
      <p className="text-xs text-text-muted">
        {tr.quiz.streak(result.streak.current, result.streak.bonusAt)} · {tr.quiz.total(result.totalPoints)}
      </p>
      <div className="flex flex-col gap-1">
        <p className="text-sm font-semibold">{tr.quiz.mastery(masteryChange.name, before, after)}</p>
        <div
          role="meter"
          aria-label={tr.progress.masteryOf(masteryChange.name)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={after}
          className="h-3 overflow-hidden rounded-full bg-surface-muted"
        >
          <div
            className="h-full rounded-full bg-select transition-[width] duration-1000 ease-out"
            style={{ width: `${String(width)}%` }}
          />
        </div>
      </div>
    </div>
  );
}
