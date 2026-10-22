import type { ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';

import { useSession } from '@/app/session/sessionContext';
import { usePoints } from '@/shared/api/points';
import { tr } from '@/shared/i18n/tr';
import { Avatar } from '@/shared/ui/Avatar';
import { dopingAssets } from '@/shared/ui/dopingAssets';
import { BellIcon, CalendarIcon, ChevronDownIcon, LogoutIcon, MailIcon, SearchIcon } from '@/shared/ui/icons';

function RoundIcon({ label, children }: { label: string; children: ReactNode }) {
  return (
    <span
      role="img"
      aria-label={label}
      title={tr.menu.notInDemo}
      className="flex size-12 items-center justify-center rounded-full bg-surface-muted text-ink"
    >
      {children}
    </span>
  );
}

function PointsPill() {
  const points = usePoints();
  return (
    <span
      aria-label={tr.topBar.points}
      className="flex items-center gap-2 rounded-full bg-surface-muted px-3 py-2 text-sm font-bold"
    >
      <img src={dopingAssets.point} alt="" aria-hidden="true" className="size-5" />
      {tr.points.label(points.data?.total ?? 0)}
    </span>
  );
}

/**
 * Top bar (docs/arayuz.md): search box (not functional in the demo), the pink lightning button that opens Dopingle,
 * decorative mail/calendar/bell icons, the student and logout.
 */
export function TopBar() {
  const { session, endSession } = useSession();
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <header className="flex items-center gap-4 bg-surface px-6 py-4">
      <label className="flex flex-1 items-center gap-3 rounded-button bg-surface-muted px-4 py-3 text-text-muted">
        <SearchIcon />
        <span className="sr-only">{tr.topBar.searchLabel}</span>
        <input
          type="search"
          placeholder={tr.topBar.searchPlaceholder}
          className="w-full bg-transparent outline-none placeholder:text-text-muted"
        />
      </label>
      <Link
        to="/app/dopingle"
        state={{ from: location.pathname }}
        aria-label={tr.topBar.openDopingle}
        title={tr.topBar.openDopingle}
        className="size-12 shrink-0 rounded-full shadow-md focus-visible:outline-2 focus-visible:outline-brand-blue"
      >
        <img src={dopingAssets.shortVideo} alt="" className="size-12" />
      </Link>
      <RoundIcon label={tr.topBar.messages}>
        <MailIcon />
      </RoundIcon>
      <RoundIcon label={tr.topBar.calendar}>
        <CalendarIcon />
      </RoundIcon>
      <RoundIcon label={tr.topBar.notifications}>
        <BellIcon />
      </RoundIcon>
      <PointsPill />
      {session && (
        <div className="flex items-center gap-3">
          <Avatar name={session.user.displayName} />
          <span className="flex items-center gap-1 rounded-button bg-surface px-3 py-2 text-sm text-text-muted shadow-sm [&_svg]:size-4">
            <span className="sr-only">{session.user.displayName}</span>
            {tr.topBar.studentClass}
            <ChevronDownIcon />
          </span>
        </div>
      )}
      <button
        type="button"
        onClick={() => {
          endSession();
          void navigate('/');
        }}
        className="flex items-center gap-2 rounded-button px-3 py-2 text-text-muted hover:bg-surface-muted focus-visible:outline-2 focus-visible:outline-brand-blue"
      >
        <LogoutIcon />
        {tr.topBar.logout}
      </button>
    </header>
  );
}
