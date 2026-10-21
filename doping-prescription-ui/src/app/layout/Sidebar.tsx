import type { ReactNode } from 'react';
import { NavLink } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { BrandLogo } from '@/shared/ui/BrandLogo';
import { ExamIcon, PlayIcon, TargetIcon } from '@/shared/ui/icons';

interface MenuItem {
  to: string;
  label: string;
  icon: ReactNode;
  tile: string;
}

/** MVP menu (AGENTS.md §7, phase 2): Ana Sayfa and Puanlarım are deferred. */
const MENU: MenuItem[] = [
  { to: '/app/dopingle', label: tr.menu.dopingle, icon: <PlayIcon />, tile: 'bg-menu-dopingle' },
  { to: '/app/denemeler', label: tr.menu.exams, icon: <ExamIcon />, tile: 'bg-menu-exams' },
  { to: '/app/eksiklerim', label: tr.menu.weakTopics, icon: <TargetIcon />, tile: 'bg-menu-weak-topics' },
];

/** Left menu; collapses to icons with tooltips on the Dopingle page (docs/arayuz.md, Uygulama kabuğu). */
export function Sidebar({ collapsed }: { collapsed: boolean }) {
  return (
    <aside
      className={`${collapsed ? 'w-[72px] items-center' : 'w-[300px]'} flex shrink-0 flex-col gap-8 border-r border-border bg-surface px-4 py-6`}
    >
      {collapsed ? <span className="sr-only">{tr.app.logoAlt}</span> : <BrandLogo />}
      <nav aria-label={tr.menu.navigation}>
        <ul className="flex flex-col gap-2">
          {MENU.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                title={collapsed ? item.label : undefined}
                aria-label={collapsed ? item.label : undefined}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-button p-2 font-semibold focus-visible:outline-2 focus-visible:outline-brand-blue ${isActive ? 'bg-surface-muted' : 'hover:bg-surface-muted-active'}`
                }
              >
                <span
                  className={`${item.tile} flex size-10 items-center justify-center rounded-button text-surface`}
                >
                  {item.icon}
                </span>
                {!collapsed && <span>{item.label}</span>}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
}
