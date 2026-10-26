import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { dopingAssets } from '@/shared/ui/dopingAssets';
import { BellIcon, CalendarIcon, MailIcon, SearchIcon } from '@/shared/ui/icons';

import { AccountMenu } from './AccountMenu';

function RoundIcon({ label, children }: { label: string; children: ReactNode }) {
  return (
    <span
      role="img"
      aria-label={label}
      title={tr.menu.notInDemo}
      className="flex size-12 shrink-0 items-center justify-center rounded-full bg-surface-muted text-ink [&_svg]:size-6"
    >
      {children}
    </span>
  );
}

/**
 * Top bar as on Doping Hafıza (docs/arayuz.md): search box (not functional in the demo), the pink lightning button
 * that opens Dopingle, decorative mail/calendar/bell icons and the account menu with points and logout.
 */
export function TopBar() {
  const location = useLocation();

  return (
    <header className="flex items-center gap-4 bg-surface px-6 pt-8 pb-5">
      <label className="flex h-12 flex-1 items-center gap-3 rounded-button bg-surface-muted px-4 text-text-muted focus-within:outline-2 focus-within:outline-brand-blue [&_svg]:size-6">
        <SearchIcon />
        <span className="sr-only">{tr.topBar.searchLabel}</span>
        <input
          type="search"
          placeholder={tr.topBar.searchPlaceholder}
          // the label draws the focus ring around the whole search box (focus-within)
          className="w-full bg-transparent outline-none placeholder:text-text-muted"
        />
      </label>
      <Link
        to="/app/dopingle"
        state={{ from: location.pathname }}
        aria-label={tr.topBar.openDopingle}
        title={tr.topBar.openDopingle}
        className="size-12 shrink-0 rounded-full focus-visible:outline-2 focus-visible:outline-brand-blue"
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
      <AccountMenu />
    </header>
  );
}
