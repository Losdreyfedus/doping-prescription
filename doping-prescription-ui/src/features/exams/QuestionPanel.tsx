import { tr } from '@/shared/i18n/tr';
import { AnswerBubble } from '@/shared/ui/AnswerBubble';

import type { AttemptView } from './api';

type Question = AttemptView['questions'][number];

/** The current question: stem and five options with letter bubbles; clicking an option answers it. */
export function QuestionPanel({
  question,
  selectedKey,
  onChoose,
}: {
  question: Question;
  selectedKey: string | undefined;
  onChoose: (key: string) => void;
}) {
  return (
    <article className="flex-1 rounded-card bg-surface p-8">
      <p className="mb-2 text-sm font-semibold text-text-muted">{tr.solve.question(question.orderNo)}</p>
      <h2 className="mb-6 text-xl leading-relaxed font-bold">{question.stem}</h2>
      <ul className="flex flex-col gap-3">
        {question.options.map((option) => (
          <li key={option.key}>
            <button
              type="button"
              onClick={() => {
                onChoose(option.key);
              }}
              aria-pressed={selectedKey === option.key}
              aria-label={`${option.key} ${option.text}`}
              className="flex w-full items-center gap-4 rounded-button p-3 text-left hover:bg-surface-muted focus-visible:outline-2 focus-visible:outline-brand-blue"
            >
              <AnswerBubble letter={option.key} state={selectedKey === option.key ? 'selected' : 'idle'} />
              <span className="text-lg">{option.text}</span>
            </button>
          </li>
        ))}
      </ul>
    </article>
  );
}
