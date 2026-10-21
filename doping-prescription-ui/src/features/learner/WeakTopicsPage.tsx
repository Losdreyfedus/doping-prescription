import { useLearnerProfile } from '@/shared/api/learnerProfile';
import { tr } from '@/shared/i18n/tr';
import { PageHeader } from '@/shared/ui/PageHeader';

import { TopicMasteryRow } from './TopicMasteryRow';

/** Route /app/eksiklerim (docs/arayuz.md, Eksiklerim). Topics arrive sorted, weakest first. */
export function WeakTopicsPage() {
  const profile = useLearnerProfile();

  return (
    <section className="flex flex-col gap-6 p-8">
      <PageHeader title={tr.pages.weakTopicsTitle} subtitle={tr.pages.weakTopicsSubtitle} />
      {profile.isPending && <p>{tr.weakTopics.loading}</p>}
      {profile.isError && <p role="alert">{tr.errors.loadFailed}</p>}
      {profile.isSuccess && (
        <ul aria-label={tr.weakTopics.listLabel} className="flex flex-col gap-3">
          {profile.data.topics.map((topic) => (
            <TopicMasteryRow key={topic.topicId} topic={topic} />
          ))}
        </ul>
      )}
    </section>
  );
}
