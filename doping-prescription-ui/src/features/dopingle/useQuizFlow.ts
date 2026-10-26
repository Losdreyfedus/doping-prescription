import { type RefObject, useCallback, useRef, useState } from 'react';

import { tr } from '@/shared/i18n/tr';

import type { RecordInteraction } from './api';
import { type MicroQuiz, openQuiz } from './quiz';

export type QuizState =
  | { status: 'closed' }
  | { status: 'loading' }
  | { status: 'open'; quiz: MicroQuiz }
  | { status: 'done' }
  | { status: 'error'; message: string };

interface Options {
  impressionId: string;
  record: RecordInteraction;
  flush: () => Promise<void>;
  /** Seconds of the video played so far, sent with QUIZ_SHOWN. */
  elapsedRef: RefObject<number>;
}

/**
 * The card's micro-question: opened at most once, by the "Soruyu çöz" button or when the video ends, then dismissed.
 * `quizActive` is true while it loads or is on screen; the video and the keyboard rest meanwhile.
 */
export function useQuizFlow({ impressionId, record, flush, elapsedRef }: Options) {
  const [quizState, setQuizState] = useState<QuizState>({ status: 'closed' });
  const requested = useRef(false);

  // The server checks the watched share before opening the question, so the events are sent first.
  const showQuiz = useCallback(async () => {
    // The button and the video's end can both ask for it; an opened, answered or failed question is not requested again.
    if (requested.current) {
      return;
    }
    requested.current = true;
    setQuizState({ status: 'loading' });
    record(impressionId, 'QUIZ_SHOWN', Math.round(elapsedRef.current * 1000));
    await flush();
    try {
      const quiz = await openQuiz(impressionId);
      setQuizState(quiz ? { status: 'open', quiz } : { status: 'done' });
    } catch (error) {
      setQuizState({
        status: 'error',
        message: error instanceof Error ? error.message : tr.errors.unexpected,
      });
    }
  }, [record, flush, impressionId, elapsedRef]);

  const dismiss = useCallback(() => {
    record(impressionId, 'QUIZ_DISMISSED');
    setQuizState({ status: 'done' });
  }, [record, impressionId]);

  const quizActive = quizState.status === 'loading' || quizState.status === 'open';
  const quizSettled = quizState.status === 'done' || quizState.status === 'error';
  return { quizState, quizActive, quizSettled, showQuiz, dismiss };
}
