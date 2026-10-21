import { useNavigate } from 'react-router';

import { useSession } from '@/app/session/sessionContext';
import { tr } from '@/shared/i18n/tr';
import { Avatar } from '@/shared/ui/Avatar';
import { LogoutIcon, SearchIcon } from '@/shared/ui/icons';

/** Top bar: search box (not functional in the demo), the student's name and logout. Points join in phase 6. */
export function TopBar() {
  const { session, endSession } = useSession();
  const navigate = useNavigate();

  return (
    <header className="flex items-center gap-6 border-b border-border bg-surface px-8 py-4">
      <label className="flex flex-1 items-center gap-2 rounded-button bg-surface-muted px-4 py-3 text-text-muted">
        <SearchIcon />
        <span className="sr-only">{tr.topBar.searchLabel}</span>
        <input
          type="search"
          placeholder={tr.topBar.searchPlaceholder}
          className="w-full bg-transparent outline-none placeholder:text-text-muted"
        />
      </label>
      {session && (
        <div className="flex items-center gap-3">
          <Avatar name={session.user.displayName} />
          <span className="font-semibold">{session.user.displayName}</span>
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
