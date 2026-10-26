import { Navigate, useNavigate } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { useSession } from '@/shared/session/sessionContext';
import { Avatar } from '@/shared/ui/Avatar';
import { BrandLogo } from '@/shared/ui/BrandLogo';

import { type DemoLogin, useDemoLogin, useDemoUsers } from './api';

const HOME = { STUDENT: '/app', ADMIN: '/admin' };

/** Demo login: pick a student card, or the small admin link (docs/arayuz.md, route /). */
export function LoginPage() {
  const { session, startSession } = useSession();
  const navigate = useNavigate();
  const users = useDemoUsers();
  const login = useDemoLogin((result: DemoLogin) => {
    startSession({ token: result.token, user: result.user });
    void navigate(HOME[result.user.role]);
  });

  if (session) {
    return <Navigate to={HOME[session.user.role]} replace />;
  }

  const students = users.data?.filter((user) => user.role === 'STUDENT') ?? [];

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-10 bg-surface-muted p-8">
      <BrandLogo />
      <div className="text-center">
        <h1 className="font-display text-4xl">{tr.login.title}</h1>
        <p className="mt-2 text-text-muted">{tr.login.subtitle}</p>
      </div>
      {users.isPending && <p>{tr.login.loading}</p>}
      {users.isError && <p role="alert">{tr.errors.loadFailed}</p>}
      {users.isSuccess && students.length === 0 && <p>{tr.login.empty}</p>}
      <ul className="flex flex-wrap justify-center gap-6">
        {students.map((student) => (
          <li key={student.slug}>
            <button
              type="button"
              disabled={login.isPending}
              onClick={() => {
                login.mutate(student.slug);
              }}
              className="flex w-56 flex-col items-center gap-3 rounded-card border border-border bg-surface p-8 transition hover:border-brand-blue focus-visible:outline-2 focus-visible:outline-brand-blue"
            >
              <Avatar name={student.displayName} size="lg" />
              <span className="text-lg font-bold">{student.displayName}</span>
              <span className="text-sm text-text-muted">{tr.login.student}</span>
            </button>
          </li>
        ))}
      </ul>
      {login.isError && <p role="alert">{login.error.message}</p>}
      <button
        type="button"
        onClick={() => {
          login.mutate('admin');
        }}
        className="text-sm text-text-muted underline"
      >
        {tr.login.adminLink}
      </button>
    </main>
  );
}
