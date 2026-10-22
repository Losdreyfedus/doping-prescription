import { useLearnerProfile } from '@/shared/api/learnerProfile';
import { tr } from '@/shared/i18n/tr';

import { TopicMasteryRow } from './TopicMasteryRow';

/**
 * Route /app/gelisimim (docs/arayuz.md, Gelişimim): the learner's topic mastery in the dark tones of Doping Hafıza's
 * Akıllı Değerlendirme Paneli. Topics arrive sorted, weakest first.
 */
export function ProgressPage() {
  const profile = useLearnerProfile();

  return (
    <div className="p-6">
      <section className="grid grid-cols-[minmax(0,1fr)_minmax(0,2.2fr)] gap-10 rounded-card bg-navy-deep p-8 text-surface">
        <header className="flex flex-col gap-4">
          <h1 className="font-display text-[2.625rem] leading-tight font-bold">{tr.pages.progressTitle}</h1>
          <p className="text-surface/70">{tr.pages.progressSubtitle}</p>
          <ul aria-label={tr.progress.legend} className="mt-2 flex flex-col gap-2 text-sm text-surface/80">
            <li className="flex items-center gap-2">
              <span aria-hidden="true" className="size-3 rounded-full bg-band-weak" />
              {tr.progress.legendWeak}
            </li>
            <li className="flex items-center gap-2">
              <span aria-hidden="true" className="size-3 rounded-full bg-band-medium" />
              {tr.progress.legendMedium}
            </li>
            <li className="flex items-center gap-2">
              <span aria-hidden="true" className="size-3 rounded-full bg-band-strong" />
              {tr.progress.legendStrong}
            </li>
          </ul>
        </header>
        <div className="rounded-card bg-navy p-4">
          {profile.isPending && <p className="p-2">{tr.progress.loading}</p>}
          {profile.isError && (
            <p role="alert" className="p-2">
              {tr.errors.loadFailed}
            </p>
          )}
          {profile.isSuccess && (
            <ul aria-label={tr.progress.listLabel} className="flex flex-col">
              {profile.data.topics.map((topic) => (
                <TopicMasteryRow key={topic.topicId} topic={topic} />
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}
