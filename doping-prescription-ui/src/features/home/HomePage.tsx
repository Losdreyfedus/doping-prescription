import { useSession } from '@/app/session/sessionContext';
import { usePoints } from '@/shared/api/points';
import { tr } from '@/shared/i18n/tr';
import { Avatar } from '@/shared/ui/Avatar';
import { MegaphoneIcon, PlayCircleIcon } from '@/shared/ui/icons';

import { WeekCalendar } from './WeekCalendar';

const BADGE_COUNT = 5;

/**
 * Route /app (docs/arayuz.md, Ana Sayfa): Doping Hafıza's home page. Only the student's name is live; the cards around
 * it reproduce the familiar layout with CSS drawings instead of images (AGENTS.md rule 11).
 */
export function HomePage() {
  const { session } = useSession();
  const name = session?.user.displayName ?? '';
  const points = usePoints();

  return (
    <div className="flex gap-6 p-6">
      <div className="flex min-w-0 flex-1 flex-col gap-8">
        <div className="flex gap-3">
          <section
            aria-label={tr.home.profile}
            className="flex flex-1 items-start gap-4 rounded-card bg-navy p-5 text-surface"
          >
            <div className="flex flex-col items-center">
              <span className="rounded-card bg-sun/80 p-3">
                <Avatar name={name} size="lg" />
              </span>
              <span className="-mt-3 rounded-full bg-surface px-3 py-0.5 text-sm font-bold text-ink shadow">
                <span aria-hidden="true" className="mr-1 inline-block size-3 rounded-full bg-sun" />
                {points.data?.total ?? 0}
              </span>
            </div>
            <div className="flex flex-1 flex-col gap-3">
              <h1 className="text-2xl font-bold">{name}</h1>
              <div aria-hidden="true" className="flex items-center gap-1.5">
                {Array.from({ length: BADGE_COUNT }, (_, index) => (
                  <span
                    key={index}
                    className="size-7 rounded-full border-2 border-surface/60 bg-surface/20"
                  />
                ))}
                <span className="flex size-8 items-center justify-center rounded-full border border-surface/60 text-xs font-bold">
                  +6
                </span>
              </div>
            </div>
          </section>
          <section
            aria-label={tr.home.dailyTasks}
            className="flex w-40 flex-col items-center gap-3 rounded-card bg-task-blue p-4 text-center text-surface"
          >
            <h2 className="text-sm font-bold">{tr.home.dailyTasks}</h2>
            <span className="flex size-12 items-center justify-center rounded-full border-4 border-surface/80 bg-surface font-display text-xl font-bold text-task-blue">
              50
            </span>
            <p className="text-xs">{tr.home.dailyTaskText}</p>
          </section>
        </div>

        <section className="flex flex-col gap-4">
          <h2 className="text-2xl">{tr.home.performance}</h2>
          <div className="rounded-card bg-surface-muted p-2">
            <div className="relative flex h-48 items-center justify-center rounded-card bg-gradient-to-r from-navy via-navy-end to-brand-blue-ink">
              <p className="absolute top-3 left-4 rounded-button bg-surface/85 px-4 py-2 text-sm">
                {tr.home.performanceNotice}
              </p>
              <span className="flex size-12 items-center justify-center rounded-full bg-surface/40 text-surface [&_svg]:size-8">
                <PlayCircleIcon />
              </span>
            </div>
          </div>
        </section>

        <div className="grid grid-cols-2 gap-6">
          <section className="flex flex-col gap-4">
            <h2 className="text-2xl">{tr.home.announcements}</h2>
            <div className="flex flex-col items-center gap-2 rounded-card bg-surface p-8 text-center">
              <span className="text-accent-pink [&_svg]:size-14">
                <MegaphoneIcon />
              </span>
              <p className="font-semibold">{tr.home.noAnnouncements}</p>
              <p className="text-xs text-text-muted">{tr.home.announcementsHint}</p>
            </div>
          </section>
          <section className="flex flex-col gap-4">
            <h2 className="text-2xl">{tr.home.recentActivity}</h2>
            <div className="flex flex-col gap-4 rounded-card bg-surface p-5">
              <p className="text-sm text-brand-blue-ink">{tr.home.recentActivityHint}</p>
              {[0, 1].map((row) => (
                <div key={row} aria-hidden="true" className="flex items-center gap-4">
                  <span className="size-10 rounded-button bg-surface-muted" />
                  <span className="flex flex-1 flex-col gap-2">
                    <span className="h-3 w-1/3 rounded bg-surface-muted" />
                    <span className="h-3 w-1/2 rounded bg-surface-muted" />
                  </span>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>

      <aside className="flex w-80 shrink-0 flex-col gap-6 overflow-hidden rounded-card bg-surface">
        <WeekCalendar today={new Date()} />
        <div className="flex flex-col gap-3 px-3">
          <h2 className="text-2xl">{tr.home.journeyTitle}</h2>
          <p className="text-sm">{tr.home.journeyText}</p>
          <span
            aria-disabled="true"
            className="w-fit rounded-button bg-brand-blue px-3 py-2 text-sm font-bold text-surface"
          >
            {tr.home.journeyButton}
          </span>
        </div>
        <div aria-hidden="true" className="relative mt-auto h-72 bg-gradient-to-b from-surface to-sky">
          <span className="absolute top-6 right-[-40px] size-40 rounded-full bg-sun" />
          <span className="absolute bottom-0 left-[-30%] h-40 w-[160%] rounded-t-[50%] bg-grass" />
          <span className="absolute bottom-0 left-[-10%] h-24 w-[90%] rounded-t-[50%] bg-grass-dark" />
        </div>
      </aside>
    </div>
  );
}
