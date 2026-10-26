import { useEffect } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { BookmarkIcon, CloseIcon, FilterIcon } from '@/shared/ui/icons';

import { Sidebar } from './Sidebar';

function returnPath(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'from' in state && typeof state.from === 'string') {
    return state.from;
  }
  return '/app';
}

const PILL = 'flex w-fit items-center gap-2 rounded-full border border-surface/30 px-3 py-1.5 font-bold';

/**
 * Full-screen dark Dopingle layer (docs/arayuz.md, Dopingle): icon rail on the left, keyboard hint and the Filtre /
 * Kaydedilen pills top left, a close button top right. Kaydedilen switches between the feed and the saved videos;
 * Filtre is not part of the demo. Escape closes it unless the micro-question is open.
 */
export function DopingleLayer() {
  const location = useLocation();
  const navigate = useNavigate();
  const back = returnPath(location.state);
  const savedOnly = new URLSearchParams(location.search).get('saved') === '1';

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      // An open micro-question handles Escape itself (marks it handled) so the layer stays open behind it.
      if (event.key === 'Escape' && !event.defaultPrevented) {
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
          <span aria-disabled="true" className={`${PILL} bg-surface/10`}>
            <FilterIcon />
            {tr.dopingle.filter}
          </span>
          <Link
            to={savedOnly ? '/app/dopingle' : '/app/dopingle?saved=1'}
            state={{ from: back }}
            aria-current={savedOnly ? 'page' : undefined}
            className={`${PILL} focus-visible:outline-2 focus-visible:outline-select ${savedOnly ? 'bg-surface text-ink' : 'bg-surface/10 hover:bg-surface/20'}`}
          >
            <BookmarkIcon filled={savedOnly} />
            {tr.dopingle.saved}
          </Link>
        </div>
        <button
          type="button"
          aria-label={tr.dopingle.close}
          onClick={() => void navigate(back)}
          className="absolute top-5 right-5 z-10 flex size-10 items-center justify-center rounded-full bg-surface/15 hover:bg-surface/25 focus-visible:outline-2 focus-visible:outline-select"
        >
          <CloseIcon />
        </button>
        {/* Switching between the feed and Kaydedilen starts a new session from the first card */}
        <main key={location.search} className="h-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
