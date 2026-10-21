import type { ReactNode } from 'react';
import { NavLink } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { BrandLogo } from '@/shared/ui/BrandLogo';
import {
  BoardIcon,
  BookIcon,
  ExamIcon,
  HelpIcon,
  HomeIcon,
  LiveIcon,
  PhoneIcon,
  PlayCircleIcon,
  PulseIcon,
  ScreenIcon,
  SparkIcon,
  TargetIcon,
} from '@/shared/ui/icons';

interface MenuItem {
  label: string;
  icon: ReactNode;
  tile: string;
  /** Only the screens of the demo have a route; the others are shown for a familiar menu. */
  to?: string;
}

/** Doping Hafıza's menu (docs/arayuz.md, Uygulama kabuğu) plus Eksiklerim; Dopingle opens from the top bar. */
const MENU: MenuItem[] = [
  { to: '/app', label: tr.menu.home, icon: <HomeIcon />, tile: 'bg-menu-home' },
  { label: tr.menu.lessons, icon: <BookIcon />, tile: 'bg-menu-lessons' },
  { to: '/app/denemeler', label: tr.menu.exams, icon: <ExamIcon />, tile: 'bg-menu-exams' },
  { to: '/app/eksiklerim', label: tr.menu.weakTopics, icon: <TargetIcon />, tile: 'bg-menu-weak-topics' },
  { label: tr.menu.way, icon: <SparkIcon />, tile: 'bg-menu-way' },
  { label: tr.menu.guidance, icon: <ScreenIcon />, tile: 'bg-menu-guidance' },
  { label: tr.menu.stats, icon: <PulseIcon />, tile: 'bg-menu-stats' },
  { label: tr.menu.panel, icon: <BoardIcon />, tile: 'bg-menu-panel' },
  { label: tr.menu.oneToOne, icon: <PhoneIcon />, tile: 'bg-menu-one-to-one' },
  { label: tr.menu.help, icon: <HelpIcon />, tile: 'bg-menu-help' },
  { label: tr.menu.live, icon: <LiveIcon />, tile: 'bg-menu-live' },
  { label: tr.menu.stories, icon: <PlayCircleIcon />, tile: 'bg-menu-stories' },
];

const ITEM = 'flex items-center gap-3 rounded-button px-2 py-1.5 text-[15px]';

/** Left menu; an icon rail inside the Dopingle layer (docs/arayuz.md). */
export function Sidebar({ collapsed = false }: { collapsed?: boolean }) {
  return (
    <aside
      className={`${collapsed ? 'w-[92px] items-center' : 'w-[275px]'} flex shrink-0 flex-col gap-8 bg-surface px-3 py-8`}
    >
      {collapsed ? <span className="sr-only">{tr.app.logoAlt}</span> : <BrandLogo />}
      <nav aria-label={tr.menu.navigation}>
        <ul className={`flex flex-col ${collapsed ? 'gap-4' : 'gap-1'}`}>
          {MENU.map((item) => {
            const content = (
              <>
                <span
                  className={`${item.tile} flex size-7 items-center justify-center rounded-lg text-surface`}
                >
                  {item.icon}
                </span>
                {!collapsed && <span>{item.label}</span>}
              </>
            );
            return (
              <li key={item.label}>
                {item.to ? (
                  <NavLink
                    to={item.to}
                    end={item.to === '/app'}
                    title={collapsed ? item.label : undefined}
                    aria-label={collapsed ? item.label : undefined}
                    className={({ isActive }) =>
                      `${ITEM} focus-visible:outline-2 focus-visible:outline-brand-blue ${isActive ? 'bg-surface-muted' : 'hover:bg-surface-muted-active'}`
                    }
                  >
                    {content}
                  </NavLink>
                ) : (
                  <span aria-disabled="true" title={tr.menu.notInDemo} className={`${ITEM} cursor-default`}>
                    {content}
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
}
