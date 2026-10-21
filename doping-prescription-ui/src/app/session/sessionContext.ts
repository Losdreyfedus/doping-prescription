import { createContext, useContext } from 'react';

import type { Session } from '@/shared/lib/session';

export interface SessionState {
  session: Session | null;
  startSession: (session: Session) => void;
  endSession: () => void;
}

export const SessionContext = createContext<SessionState | null>(null);

/** The logged-in user; the only global client state (docs/kod-standartlari.md §3). */
export function useSession(): SessionState {
  const state = useContext(SessionContext);
  if (state === null) {
    throw new Error('useSession must be used inside SessionProvider');
  }
  return state;
}
