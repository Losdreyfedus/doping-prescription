import { usePoints } from '@/shared/api/points';
import { tr } from '@/shared/i18n/tr';
import { useSession } from '@/shared/session/sessionContext';
import { dopingAssets } from '@/shared/ui/dopingAssets';
import { Skeleton } from '@/shared/ui/Skeleton';

import { WeekCalendar } from './WeekCalendar';

const BADGE_COUNT = 5;
/** The page's "+6 more badges" circle; decorative like the badges themselves. */
const MORE_BADGES = 6;

/**
 * Route /app (docs/arayuz.md, Ana Sayfa): Doping Hafıza's home page with its own images. The student's name, points
 * and answer streak are live; the other cards reproduce the familiar page and are not functional in the demo.
 */
export function HomePage() {
  const { session } = useSession();
  const name = session?.user.displayName ?? '';
  const points = usePoints();

  return (
    <div className="flex gap-6 p-6">
      <div className="flex min-w-0 flex-1 flex-col gap-8">
        <div className="flex gap-2.5">
          <section
            aria-label={tr.home.profile}
            className="flex flex-1 items-start gap-4 rounded-card bg-navy px-5 py-5 text-surface"
          >
            <div className="flex flex-col items-center">
              <img
                src={dopingAssets.avatarBoy}
                alt={tr.home.avatar(name)}
                className="size-24 rounded-card bg-sun/90 object-cover"
              />
              <span className="-mt-3 flex items-center gap-1 rounded-full bg-surface px-3 py-0.5 text-sm font-bold text-ink shadow">
                <img src={dopingAssets.point} alt="" aria-hidden="true" className="size-5" />
                <span className="sr-only">{tr.topBar.points}: </span>
                <LiveValue status={points.status} value={points.data?.total} />
              </span>
            </div>
            <div className="flex flex-1 flex-col gap-4 pt-1">
              <div className="flex items-start justify-between">
                <h1 className="text-[1.375rem] font-bold">{name}</h1>
                <span className="flex items-center gap-1 font-display text-[1.375rem] font-bold text-sun">
                  <img src={dopingAssets.dayCount} alt="" aria-hidden="true" className="size-6" />
                  <span className="sr-only">{tr.home.streak}: </span>
                  <LiveValue status={points.status} value={points.data?.streak.current} />
                </span>
              </div>
              {points.isError && (
                <p role="alert" className="text-xs text-sun">
                  {tr.home.pointsFailed}
                </p>
              )}
              <div aria-hidden="true" className="flex items-center gap-1.5">
                {Array.from({ length: BADGE_COUNT }, (_, index) => (
                  <img key={index} src={dopingAssets.medal} alt="" className="size-8 grayscale" />
                ))}
                <span className="flex size-8 items-center justify-center rounded-full border border-surface/60 text-xs font-bold">
                  {tr.home.moreBadges(MORE_BADGES)}
                </span>
              </div>
            </div>
          </section>
          <section
            aria-label={tr.home.dailyTasks}
            className="flex w-40 flex-col items-center gap-3 rounded-card bg-task-blue p-4 text-center text-surface"
          >
            <h2 className="text-sm font-bold">{tr.home.dailyTasks}</h2>
            <div aria-hidden="true" className="flex items-center gap-2">
              <img src={dopingAssets.chevronLeft} alt="" className="size-5 invert" />
              <img src={dopingAssets.fiftyPoint} alt="" className="size-14" />
              <img src={dopingAssets.chevronRight} alt="" className="size-5 invert" />
            </div>
            <p className="text-xs">{tr.home.dailyTaskText}</p>
          </section>
        </div>

        <section className="flex flex-col gap-4">
          <h2 className="text-[1.375rem]">{tr.home.performance}</h2>
          <div className="rounded-card bg-surface-muted p-2">
            <div className="relative flex h-48 items-center justify-center overflow-hidden rounded-card">
              <img
                src={dopingAssets.videoThumbnail}
                alt=""
                aria-hidden="true"
                className="absolute inset-0 size-full object-cover"
              />
              <span aria-hidden="true" className="absolute inset-0 bg-ink/45" />
              <p className="absolute top-3 left-4 flex items-center gap-2 rounded-button bg-surface/85 px-3 py-2 text-sm">
                <img src={dopingAssets.warning} alt="" aria-hidden="true" className="size-6" />
                {tr.home.performanceNotice}
              </p>
              <img src={dopingAssets.playIcon} alt="" aria-hidden="true" className="relative size-12" />
            </div>
          </div>
        </section>

        <div className="grid grid-cols-2 gap-6">
          <section className="flex flex-col gap-4">
            <h2 className="text-[1.375rem]">{tr.home.announcements}</h2>
            <div className="flex flex-col items-center gap-2 rounded-card bg-surface p-8 text-center">
              <img src={dopingAssets.announcement} alt="" aria-hidden="true" className="h-20" />
              <p className="font-semibold">{tr.home.noAnnouncements}</p>
              <p className="text-xs text-text-muted">{tr.home.announcementsHint}</p>
            </div>
          </section>
          <section className="flex flex-col gap-4">
            <h2 className="text-[1.375rem]">{tr.home.recentActivity}</h2>
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

      <aside className="flex w-[19.375rem] shrink-0 flex-col gap-6 overflow-hidden rounded-card bg-surface">
        <WeekCalendar today={new Date()} />
        <div className="flex flex-col gap-3 px-3">
          <h2 className="text-[1.375rem]">{tr.home.journeyTitle}</h2>
          <p className="text-sm">{tr.home.journeyText}</p>
          <span
            aria-disabled="true"
            className="w-fit rounded-button bg-brand-blue px-3 py-2 text-sm font-bold text-surface"
          >
            {tr.home.journeyButton}
          </span>
        </div>
        <img src={dopingAssets.yksJourney} alt="" aria-hidden="true" className="mt-auto w-full" />
      </aside>
    </div>
  );
}

/** A number from the points query: a placeholder while it loads and a dash if it failed, never a misleading 0. */
function LiveValue({
  status,
  value,
}: {
  status: 'pending' | 'error' | 'success';
  value: number | undefined;
}) {
  if (status === 'pending') {
    return <Skeleton label={tr.home.loading} />;
  }
  return <>{value ?? tr.home.unknownValue}</>;
}
