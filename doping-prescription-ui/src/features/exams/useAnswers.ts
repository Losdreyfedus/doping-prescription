import { useState } from 'react';

import { type AttemptView, useSaveAnswer } from './api';

/**
 * The solve screen's answers: an optimistic local copy that is saved to the server on every change. Clicking the
 * selected option again clears it.
 */
export function useAnswers(attempt: AttemptView) {
  const [answers, setAnswers] = useState<Record<number, string>>(() => initialAnswers(attempt));
  const save = useSaveAnswer(attempt.attemptId);

  function choose(questionId: number, key: string) {
    const next = answers[questionId] === key ? null : key;
    setAnswers((current) => {
      const others = Object.fromEntries(
        Object.entries(current).filter(([answeredId]) => Number(answeredId) !== questionId),
      );
      return next === null ? others : { ...others, [questionId]: next };
    });
    save.mutate({ questionId, selectedKey: next });
  }

  return { answers, choose, saveFailed: save.isError };
}

function initialAnswers(attempt: AttemptView): Record<number, string> {
  const answers: Record<number, string> = {};
  for (const answer of attempt.answers) {
    if (answer.selectedKey) {
      answers[answer.questionId] = answer.selectedKey;
    }
  }
  return answers;
}
