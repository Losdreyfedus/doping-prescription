import type { ReactNode } from 'react';
import { Navigate } from 'react-router';

import { useSession } from '@/shared/session/sessionContext';

/** Shows its children only to a logged-in user with the given role; everyone else goes to the login screen. */
export function RequireRole({ allow, children }: { allow: 'STUDENT' | 'ADMIN'; children: ReactNode }) {
  const { session } = useSession();
  if (session?.user.role !== allow) {
    return <Navigate to="/" replace />;
  }
  return children;
}
