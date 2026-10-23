import { Outlet } from 'react-router';

import { Sidebar } from './Sidebar';

/**
 * Full-screen dark layout of Doping Hafıza's Akıllı Değerlendirme Paneli, used by Gelişimim: the menu narrows to a dark
 * icon rail and there is no top bar (docs/arayuz.md, Gelişimim).
 */
export function PanelLayout() {
  return (
    <div className="flex min-h-screen bg-navy-deep text-surface">
      <Sidebar collapsed dark />
      <main className="min-w-0 flex-1">
        <Outlet />
      </main>
    </div>
  );
}
