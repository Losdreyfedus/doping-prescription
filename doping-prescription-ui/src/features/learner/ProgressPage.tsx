import { Link } from 'react-router';

import { useLearnerProfile } from '@/shared/api/learnerProfile';
import { tr } from '@/shared/i18n/tr';
import { ChevronLeftIcon } from '@/shared/ui/icons';

import { TopicMasteryRow } from './TopicMasteryRow';

/**
 * Route /app/gelisimim (docs/arayuz.md, Gelişimim) in the panel layout of Doping Hafıza's Akıllı Değerlendirme
 * Paneli: title, explanation and legend on the left, topic rows on the right. Topics arrive sorted, weakest first.
 */
export function ProgressPage() {
  const profile = useLearnerProfile();

  return (
    <div className="grid min-h-screen grid-cols-[minmax(0,1fr)_minmax(0,1.75fr)]">
      <header className="flex flex-col gap-5 border-r border-surface/10 bg-gradient-to-b from-dopingle-bg to-navy-deep px-8 py-8">
        <Link
          to="/app"
          aria-label={tr.progress.back}
          className="w-fit rounded-full p-1 text-surface hover:bg-surface/10 [&_svg]:size-6"
        >
          <ChevronLeftIcon />
        </Link>
        <h1 className="font-display text-[2.625rem] leading-tight font-bold">{tr.pages.progressTitle}</h1>
        <p className="text-surface/80">{tr.pages.progressSubtitle}</p>
        <ul aria-label={tr.progress.legend} className="flex flex-col gap-2 text-sm text-surface/80">
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
      <section className="flex flex-col gap-4 px-6 py-8">
        <h2 className="text-xl font-bold">{tr.progress.course}</h2>
        <div className="flex items-center justify-between rounded-button bg-brand-blue-ink/40 px-5 py-2 text-xs text-surface/70">
          <span>{tr.progress.topicsHeader}</span>
          <span>{tr.progress.masteryHeader}</span>
        </div>
        {profile.isPending && <p>{tr.progress.loading}</p>}
        {profile.isError && <p role="alert">{tr.errors.loadFailed}</p>}
        {profile.isSuccess && (
          <ul aria-label={tr.progress.listLabel} className="flex flex-col gap-2">
            {profile.data.topics.map((topic) => (
              <TopicMasteryRow key={topic.topicId} topic={topic} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
