import { tr } from '@/shared/i18n/tr';
import { AnswerBubble } from '@/shared/ui/AnswerBubble';

import type { ExamResult } from './api';

/** Question review list: the learner's and the correct option, and the explanation. */
export function QuestionReview({ questions }: { questions: ExamResult['questions'] }) {
  return (
    <section id="review" className="rounded-card bg-surface p-6">
      <h2 className="mb-4 text-xl font-bold">{tr.result.reviewTitle}</h2>
      <ol className="flex flex-col gap-6">
        {questions.map((question) => (
          <li key={question.questionId} className="border-b border-border pb-4">
            <p className="mb-3 font-semibold">
              {tr.solve.question(question.orderNo)} · {question.stem}
            </p>
            <div className="mb-2 flex items-center gap-6 text-sm">
              <span className="flex items-center gap-2">
                {tr.result.yourAnswer}:
                {question.selectedKey ? (
                  <AnswerBubble
                    size="sm"
                    letter={question.selectedKey}
                    state={question.result === 'CORRECT' ? 'correct' : 'wrong'}
                  />
                ) : (
                  <span className="font-semibold">{tr.result.leftBlank}</span>
                )}
              </span>
              <span className="flex items-center gap-2">
                {tr.result.correctAnswer}:
                <AnswerBubble size="sm" letter={question.correctKey} state="correct" />
              </span>
            </div>
            <p className="text-text-muted">{question.explanation}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}
