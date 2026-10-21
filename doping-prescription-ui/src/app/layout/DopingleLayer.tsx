import { useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { BookmarkIcon, CloseIcon, FilterIcon } from '@/shared/ui/icons';

import { Sidebar } from './Sidebar';

function returnPath(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'from' in state && typeof state.from === 'string') {
    return state.from;
  }
  return '/app';
}

/**
 * Full-screen dark Dopingle layer (docs/arayuz.md, Dopingle): icon rail on the left, keyboard hint and the Filtre /
 * Kaydedilen pills top left, a close button top right. Escape closes it.
 */
export function DopingleLayer() {
  const location = useLocation();
  const navigate = useNavigate();
  const back = returnPath(location.state);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        void navigate(back);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('keydown', onKey);
    };
  }, [navigate, back]);

  return (
    <div className="fixed inset-0 flex bg-dopingle-bg text-surface">
      <Sidebar collapsed />
      <div className="relative flex-1">
        <div className="absolute top-5 left-4 z-10 flex flex-col gap-2 text-xs">
          <p className="text-band-unknown">{tr.dopingle.keyboardHint}</p>
          {[
            { label: tr.dopingle.filter, icon: <FilterIcon /> },
            { label: tr.dopingle.saved, icon: <BookmarkIcon /> },
          ].map((pill) => (
            <span
              key={pill.label}
              aria-disabled="true"
              className="flex w-fit items-center gap-2 rounded-full border border-surface/30 bg-surface/10 px-3 py-1.5 font-bold"
            >
              {pill.icon}
              {pill.label}
            </span>
          ))}
        </div>
        <button
          type="button"
          aria-label={tr.dopingle.close}
          onClick={() => void navigate(back)}
          className="absolute top-5 right-5 z-10 flex size-10 items-center justify-center rounded-full bg-surface/15 hover:bg-surface/25 focus-visible:outline-2 focus-visible:outline-select"
        >
          <CloseIcon />
        </button>
        <main className="h-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
