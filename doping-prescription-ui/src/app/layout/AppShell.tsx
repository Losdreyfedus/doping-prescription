import { Outlet } from 'react-router';

import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';

/**
 * Student layout (docs/arayuz.md, Uygulama kabuğu): Doping Hafıza's menu on the left and the top bar. Dopingle (a
 * full-screen layer) and Gelişimim (the dark panel layout) have their own layouts in the route tree (routes.tsx).
 */
export function AppShell() {
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
