import { tr } from '@/shared/i18n/tr';

/** Route-level error boundary for unexpected render errors (docs/kod-standartlari.md §3). */
export function RouteError() {
  return (
    <main role="alert" className="flex min-h-screen items-center justify-center p-8">
      <p className="text-lg">{tr.errors.unexpected}</p>
    </main>
  );
}
