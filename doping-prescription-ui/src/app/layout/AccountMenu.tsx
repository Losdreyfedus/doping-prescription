import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';

import { useSession } from '@/app/session/sessionContext';
import { usePoints } from '@/shared/api/points';
import { tr } from '@/shared/i18n/tr';
import { Avatar } from '@/shared/ui/Avatar';
import { dopingAssets } from '@/shared/ui/dopingAssets';
import { ChevronDownIcon, ChevronRightIcon, LogoutIcon, SettingsIcon, UserIcon } from '@/shared/ui/icons';

const ROW = 'flex w-full items-center gap-4 rounded-button px-3 py-2.5 text-left text-sm';

/**
 * The avatar of Doping Hafıza's top bar opens the account menu: the student, their points (AGENTS.md:
 * points in the top bar), Profilim and Ayarlar (not in the demo) and logout.
 */
export function AccountMenu() {
  const { session, endSession } = useSession();
  const points = usePoints();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const close = (event: MouseEvent | KeyboardEvent) => {
      const outside =
        event instanceof MouseEvent &&
        event.target instanceof Node &&
        !container.current?.contains(event.target);
      if (outside || (event instanceof KeyboardEvent && event.key === 'Escape')) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', close);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', close);
    };
  }, [open]);

  if (!session) {
    return null;
  }
  const name = session.user.displayName;

  return (
    <div ref={container} className="relative flex items-center gap-3">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={tr.topBar.account(name)}
        onClick={() => {
          setOpen((value) => !value);
        }}
        className="rounded-full focus-visible:outline-2 focus-visible:outline-brand-blue"
      >
        <Avatar name={name} size="bar" />
      </button>
      {/* The class selector of Doping Hafıza; a single class in the demo */}
      <span
        aria-disabled="true"
        title={tr.menu.notInDemo}
        className="flex h-12 items-center gap-1 rounded-button bg-surface px-3 text-sm text-text-muted shadow-md [&_svg]:size-4"
      >
        {tr.topBar.studentClass}
        <ChevronDownIcon />
      </span>
      {open && (
        <div
          role="menu"
          className="absolute top-full right-0 z-20 mt-2 flex w-72 flex-col gap-1 rounded-card bg-surface p-3 text-ink shadow-xl"
        >
          <div className="flex items-center gap-3 px-3 py-2">
            <Avatar name={name} />
            <div className="flex flex-1 flex-col">
              <span className="font-bold">{name}</span>
              <span className="text-xs text-text-muted">{tr.topBar.studentClass}</span>
            </div>
            <span className="[&_svg]:size-5">
              <ChevronRightIcon />
            </span>
          </div>
          <div className="flex items-center gap-2 px-3 pb-2 text-sm font-bold">
            <img src={dopingAssets.point} alt="" aria-hidden="true" className="size-5" />
            <span aria-label={tr.topBar.points}>{tr.points.label(points.data?.total ?? 0)}</span>
          </div>
          <span
            role="menuitem"
            aria-disabled="true"
            title={tr.menu.notInDemo}
            className={`${ROW} cursor-default`}
          >
            <UserIcon />
            {tr.topBar.profile}
          </span>
          <span
            role="menuitem"
            aria-disabled="true"
            title={tr.menu.notInDemo}
            className={`${ROW} cursor-default`}
          >
            <SettingsIcon />
            {tr.topBar.settings}
          </span>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              endSession();
              void navigate('/');
            }}
            className={`${ROW} hover:bg-surface-muted`}
          >
            <LogoutIcon />
            {tr.topBar.logout}
          </button>
        </div>
      )}
    </div>
  );
}
