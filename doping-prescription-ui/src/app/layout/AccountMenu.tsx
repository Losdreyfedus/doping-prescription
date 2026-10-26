import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';

import { usePoints } from '@/shared/api/points';
import { tr } from '@/shared/i18n/tr';
import { useSession } from '@/shared/session/sessionContext';
import { dopingAssets } from '@/shared/ui/dopingAssets';
import { ChevronDownIcon, ChevronRightIcon, LogoutIcon, SettingsIcon, UserIcon } from '@/shared/ui/icons';
import { useMenuNavigation } from '@/shared/ui/useMenuNavigation';

const ROW = 'flex w-full items-center gap-4 rounded-button px-3 py-2.5 text-left text-sm';

/**
 * The avatar of Doping Hafıza's top bar opens the account menu: the student, their points (AGENTS.md:
 * points in the top bar), Profilim and Ayarlar (not in the demo) and logout. Keyboard: see useMenuNavigation.
 */
export function AccountMenu() {
  const { session, endSession } = useSession();
  const points = usePoints();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const container = useRef<HTMLDivElement>(null);
  const close = useCallback(() => {
    setOpen(false);
  }, []);
  const { menuRef, triggerRef, onKeyDown } = useMenuNavigation<HTMLDivElement, HTMLButtonElement>(
    open,
    close,
  );

  useEffect(() => {
    if (!open) {
      return;
    }
    const closeOutside = (event: MouseEvent) => {
      if (event.target instanceof Node && !container.current?.contains(event.target)) {
        close();
      }
    };
    document.addEventListener('mousedown', closeOutside);
    return () => {
      document.removeEventListener('mousedown', closeOutside);
    };
  }, [open, close]);

  if (!session) {
    return null;
  }
  const name = session.user.displayName;

  return (
    <div ref={container} className="relative flex items-center gap-3">
      <button
        ref={triggerRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={tr.topBar.account(name)}
        onClick={() => {
          setOpen((value) => !value);
        }}
        className="rounded-full focus-visible:outline-2 focus-visible:outline-brand-blue"
      >
        <img src={dopingAssets.avatarBoy} alt="" className="size-12 rounded-full bg-sun/90 object-cover" />
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
          ref={menuRef}
          role="menu"
          tabIndex={-1}
          aria-label={tr.topBar.account(name)}
          onKeyDown={onKeyDown}
          className="absolute top-full right-0 z-20 mt-2 flex w-72 flex-col gap-1 rounded-card bg-surface p-3 text-ink shadow-xl"
        >
          <div className="flex items-center gap-3 px-3 py-2">
            <img
              src={dopingAssets.avatarBoy}
              alt=""
              aria-hidden="true"
              className="size-9 rounded-full bg-sun/90 object-cover"
            />
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
            <span>
              <span className="sr-only">{tr.topBar.points}: </span>
              {points.isSuccess ? tr.points.label(points.data.total) : tr.points.unknown}
            </span>
          </div>
          <button
            type="button"
            role="menuitem"
            tabIndex={-1}
            aria-disabled="true"
            title={tr.menu.notInDemo}
            className={`${ROW} cursor-default focus-visible:outline-2 focus-visible:outline-brand-blue`}
          >
            <UserIcon />
            {tr.topBar.profile}
          </button>
          <button
            type="button"
            role="menuitem"
            tabIndex={-1}
            aria-disabled="true"
            title={tr.menu.notInDemo}
            className={`${ROW} cursor-default focus-visible:outline-2 focus-visible:outline-brand-blue`}
          >
            <SettingsIcon />
            {tr.topBar.settings}
          </button>
          <button
            type="button"
            role="menuitem"
            tabIndex={-1}
            onClick={() => {
              endSession();
              void navigate('/');
            }}
            className={`${ROW} hover:bg-surface-muted focus-visible:outline-2 focus-visible:outline-brand-blue`}
          >
            <LogoutIcon />
            {tr.topBar.logout}
          </button>
        </div>
      )}
    </div>
  );
}
