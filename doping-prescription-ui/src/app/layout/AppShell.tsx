import { Outlet, useLocation } from 'react-router';

import { DopingleLayer } from './DopingleLayer';
import { PanelLayout } from './PanelLayout';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';

/**
 * Student layout (docs/arayuz.md, Uygulama kabuğu). Dopingle is not a menu page: the lightning button opens it as a
 * full-screen layer with an icon rail, and closing it returns to the page it was opened from. Gelişimim uses the
 * dark panel layout.
 */
export function AppShell() {
  const { pathname } = useLocation();

  if (pathname.startsWith('/app/dopingle')) {
    return <DopingleLayer />;
  }
  if (pathname.startsWith('/app/gelisimim')) {
    return <PanelLayout />;
  }
  return (
    <div className="flex min-h-screen bg-surface">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar />
        <main className="flex-1 bg-surface-sheet">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
