import { Outlet, useLocation } from 'react-router';

import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';

/** Student layout: left menu and top bar around the page (docs/arayuz.md, Uygulama kabuğu). */
export function AppShell() {
  const { pathname } = useLocation();
  const onDopingle = pathname.startsWith('/app/dopingle');

  return (
    <div className="flex min-h-screen">
      <Sidebar collapsed={onDopingle} />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar />
        <main className={`flex-1 ${onDopingle ? 'bg-dopingle-bg' : 'bg-surface'}`}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
