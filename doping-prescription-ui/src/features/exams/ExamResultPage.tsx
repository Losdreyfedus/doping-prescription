import { useParams } from 'react-router';

import { useSession } from '@/app/session/sessionContext';
import { tr } from '@/shared/i18n/tr';
import { Avatar } from '@/shared/ui/Avatar';

import { useExamResult } from './api';
import { QuestionReview } from './QuestionReview';
import { ResultSummary } from './ResultSummary';
import { TopicAnalysis } from './TopicAnalysis';

/** Route /app/denemeler/:attemptId/sonuc (docs/arayuz.md, Deneme sonucu). */
export function ExamResultPage() {
  const { attemptId = '' } = useParams();
  const { session } = useSession();
  const result = useExamResult(attemptId);

  if (result.isPending) {
    return <p className="p-8">{tr.result.loading}</p>;
  }
  if (result.isError) {
    return (
      <p role="alert" className="p-8">
        {result.error.message}
      </p>
    );
  }
  const name = session?.user.displayName ?? '';
  return (
    <div>
      <header className="flex flex-col items-center gap-3 bg-navy-deep px-8 py-10 text-surface">
        <Avatar name={name} size="lg" />
        <h1 className="text-3xl">
          {tr.result.congrats(name)} <strong>{tr.result.completed}</strong>
        </h1>
        <p className="text-band-unknown">{result.data.title}</p>
        <a href="#review" className="rounded-button bg-orange px-6 py-2 font-semibold text-surface">
          {tr.result.review}
        </a>
      </header>
      <div className="flex flex-col gap-6 bg-surface-result p-8">
        <h2 className="text-center text-2xl font-bold">{tr.result.successTitle}</h2>
        <ResultSummary result={result.data} />
        <div role="status" className="flex items-center gap-3 rounded-card bg-navy px-6 py-4 text-surface">
          <span
            aria-hidden="true"
            className="size-4 animate-spin rounded-full border-2 border-surface border-t-transparent"
          />
          {tr.result.analyzing}
        </div>
        <TopicAnalysis topics={result.data.byTopic} />
        <QuestionReview questions={result.data.questions} />
      </div>
    </div>
  );
}
