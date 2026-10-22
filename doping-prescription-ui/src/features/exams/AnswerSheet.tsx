import { tr } from '@/shared/i18n/tr';
import { AnswerBubble } from '@/shared/ui/AnswerBubble';

import type { AttemptView } from './api';

const LETTERS = ['A', 'B', 'C', 'D', 'E'];

/** The optical answer sheet: one row per question, bubbles mark answers, a row click goes to the question. */
export function AnswerSheet({
  attempt,
  answers,
  currentIndex,
  onSelectQuestion,
  onChoose,
}: {
  attempt: AttemptView;
  answers: Record<number, string>;
  currentIndex: number;
  onSelectQuestion: (index: number) => void;
  onChoose: (questionId: number, key: string) => void;
}) {
  const answered = Object.keys(answers).length;
  return (
    <aside
      aria-label={tr.solve.answerSheet}
      className="w-[26.875rem] shrink-0 rounded-card bg-surface-sheet p-5"
    >
      <p className="font-bold">{attempt.title}</p>
      <p className="mb-4 text-sm">
        {tr.solve.sheetTotal(attempt.questions.length)} /{' '}
        <span className="text-select">{tr.solve.sheetAnswered(answered)}</span>
      </p>
      <ol className="flex flex-col gap-1">
        {attempt.questions.map((question, index) => (
          <li
            key={question.questionId}
            className={`flex items-center gap-3 rounded-button px-2 py-1 ${index === currentIndex ? 'bg-select-row' : ''}`}
          >
            <button
              type="button"
              onClick={() => {
                onSelectQuestion(index);
              }}
              className="w-8 text-left font-semibold"
            >
              {question.orderNo}
            </button>
            {LETTERS.map((letter) => (
              <button
                key={letter}
                type="button"
                aria-label={tr.solve.option(question.orderNo, letter)}
                aria-pressed={answers[question.questionId] === letter}
                onClick={() => {
                  onChoose(question.questionId, letter);
                }}
                className="rounded-full focus-visible:outline-2 focus-visible:outline-brand-blue"
              >
                <AnswerBubble
                  size="sm"
                  letter={letter}
                  state={answers[question.questionId] === letter ? 'selected' : 'idle'}
                />
              </button>
            ))}
          </li>
        ))}
      </ol>
    </aside>
  );
}
