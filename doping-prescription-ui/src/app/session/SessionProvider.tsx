import { useQueryClient } from '@tanstack/react-query';
import { type ReactNode, useCallback, useMemo, useState } from 'react';

import { clearSession, readSession, type Session, writeSession } from '@/shared/lib/session';

import { SessionContext, type SessionState } from './sessionContext';

/** Keeps the session in sessionStorage so a reload keeps the user logged in. */
export function SessionProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session | null>(readSession);

  const startSession = useCallback((next: Session) => {
    writeSession(next);
    setSession(next);
  }, []);

  const endSession = useCallback(() => {
    clearSession();
    queryClient.clear();
    setSession(null);
  }, [queryClient]);

  const value = useMemo<SessionState>(
    () => ({ session, startSession, endSession }),
    [session, startSession, endSession],
  );

  return <SessionContext value={value}>{children}</SessionContext>;
}
