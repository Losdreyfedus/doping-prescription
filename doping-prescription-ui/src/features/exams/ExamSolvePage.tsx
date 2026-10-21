import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';

import { tr } from '@/shared/i18n/tr';

import { AnswerSheet } from './AnswerSheet';
import { type AttemptView, useAttempt, useSubmitAttempt } from './api';
import { FinishDialog } from './FinishDialog';
import { QuestionPanel } from './QuestionPanel';
import { useAnswers } from './useAnswers';

/** Route /app/denemeler/:attemptId: loads the attempt, then shows the solve screen. */
export function ExamSolvePage() {
  const { attemptId = '' } = useParams();
  const attempt = useAttempt(attemptId);

  if (attempt.isPending) {
    return <p className="p-8">{tr.solve.loading}</p>;
  }
  if (attempt.isError) {
    return (
      <p role="alert" className="p-8">
        {attempt.error.message}
      </p>
    );
  }
  return <SolveScreen attempt={attempt.data} />;
}

function SolveScreen({ attempt }: { attempt: AttemptView }) {
  const navigate = useNavigate();
  const [index, setIndex] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const { answers, choose, saveFailed } = useAnswers(attempt);
  const submit = useSubmitAttempt(attempt.attemptId, () => {
    void navigate(`/app/denemeler/${attempt.attemptId}/sonuc`);
  });
  const question = attempt.questions[index];
  const isLast = index === attempt.questions.length - 1;
  const blank = attempt.questions.length - Object.keys(answers).length;

  if (!question) {
    return null;
  }
  return (
    <section className="flex flex-col gap-6 bg-surface-muted p-8">
      <header className="flex items-center justify-between">
        <Link to="/app/denemeler" className="text-lg font-bold">
          ← {attempt.title}
        </Link>
        <button
          type="button"
          onClick={() => {
            setConfirming(true);
          }}
          className="rounded-button bg-danger-strong px-5 py-2 font-semibold text-surface"
        >
          {tr.solve.finishTest}
        </button>
      </header>
      {saveFailed && <p role="alert">{tr.solve.saveFailed}</p>}
      <div className="flex gap-6">
        <QuestionPanel
          question={question}
          selectedKey={answers[question.questionId]}
          onChoose={(key) => {
            choose(question.questionId, key);
          }}
        />
        <AnswerSheet
          attempt={attempt}
          answers={answers}
          currentIndex={index}
          onSelectQuestion={setIndex}
          onChoose={choose}
        />
      </div>
      <footer className="flex justify-between">
        <button
          type="button"
          disabled={index === 0}
          onClick={() => {
            setIndex(index - 1);
          }}
          className="rounded-button bg-surface px-5 py-2 font-semibold disabled:opacity-50"
        >
          {tr.solve.previous}
        </button>
        <button
          type="button"
          onClick={() => {
            if (isLast) {
              setConfirming(true);
            } else {
              setIndex(index + 1);
            }
          }}
          className={`rounded-button px-5 py-2 font-semibold ${isLast ? 'bg-success-strong text-surface' : 'bg-surface'}`}
        >
          {isLast ? tr.solve.finishExam : tr.solve.next}
        </button>
      </footer>
      {submit.isError && <p role="alert">{submit.error.message}</p>}
      {confirming && (
        <FinishDialog
          blank={blank}
          busy={submit.isPending}
          onConfirm={() => {
            submit.mutate();
          }}
          onCancel={() => {
            setConfirming(false);
          }}
        />
      )}
    </section>
  );
}
