import { useNavigate } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { formatDate, formatNet } from '@/shared/lib/format';
import { ExamIcon } from '@/shared/ui/icons';
import { PageHeader } from '@/shared/ui/PageHeader';

import { type ExamSummary, useExams, useStartAttempt } from './api';

/** Testler ve Denemeler: one category (Mini Denemeler) and its exams (docs/arayuz.md). */
export function ExamsPage() {
  const navigate = useNavigate();
  const exams = useExams();
  const start = useStartAttempt((attempt) => {
    void navigate(`/app/denemeler/${attempt.attemptId}`);
  });

  return (
    <section className="p-8">
      <PageHeader title={tr.pages.examsTitle} subtitle={tr.pages.examsSubtitle} />
      <div className="flex gap-8">
        <div className="flex w-72 shrink-0 items-center gap-3 self-start rounded-card bg-ink p-5 text-surface">
          <ExamIcon />
          <div>
            <p className="font-bold">{tr.exams.category}</p>
            <p className="text-sm text-band-unknown">{tr.exams.categorySubtitle}</p>
          </div>
        </div>
        <div className="flex-1">
          <div className="mb-4 rounded-card bg-linear-to-r from-header-tint-start to-header-tint-end p-5">
            <p className="font-bold">{tr.exams.category}</p>
            <p className="text-sm text-text-muted">{tr.exams.categorySubtitle}</p>
          </div>
          {exams.isPending && <p>{tr.exams.loading}</p>}
          {exams.isError && <p role="alert">{tr.errors.loadFailed}</p>}
          {exams.data && (
            <>
              <p className="mb-3 text-sm text-text-muted">{tr.exams.count(exams.data.length)}</p>
              {exams.data.length === 0 && <p>{tr.exams.empty}</p>}
              <ul className="flex flex-col gap-3">
                {exams.data.map((exam) => (
                  <ExamRow
                    key={exam.examId}
                    exam={exam}
                    busy={start.isPending}
                    onStart={() => {
                      start.mutate(exam.examId);
                    }}
                  />
                ))}
              </ul>
            </>
          )}
          {start.isError && <p role="alert">{start.error.message}</p>}
        </div>
      </div>
    </section>
  );
}

function ExamRow({ exam, busy, onStart }: { exam: ExamSummary; busy: boolean; onStart: () => void }) {
  const label = exam.inProgressAttemptId
    ? tr.exams.resume
    : exam.lastResult
      ? tr.exams.retake
      : tr.exams.start;
  return (
    <li className="flex items-center justify-between rounded-card border border-border bg-surface p-5">
      <div>
        <p className="font-bold">{exam.title}</p>
        <p className="text-sm text-text-muted">
          {tr.exams.questionCount(exam.questionCount)}
          {exam.lastResult &&
            ` · ${tr.exams.lastResult(formatNet(exam.lastResult.net), formatDate(exam.lastResult.submittedAt))}`}
        </p>
      </div>
      <button
        type="button"
        disabled={busy}
        onClick={onStart}
        className="rounded-button bg-ink px-5 py-2 font-semibold text-surface focus-visible:outline-2 focus-visible:outline-brand-blue disabled:opacity-60"
      >
        {label}
      </button>
    </li>
  );
}
