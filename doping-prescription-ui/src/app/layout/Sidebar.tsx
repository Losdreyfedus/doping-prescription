import { type ReactNode, useState } from 'react';
import { NavLink } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { BrandLogo } from '@/shared/ui/BrandLogo';
import { dopingAssets } from '@/shared/ui/dopingAssets';
import {
  BookIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ExamIcon,
  HelpIcon,
  HomeIcon,
  LiveIcon,
  PhoneIcon,
  PlayCircleIcon,
  PulseIcon,
  ScreenIcon,
  TargetIcon,
} from '@/shared/ui/icons';

interface MenuItem {
  label: string;
  /** A coloured tile with a white icon, or Doping's own image for the item. */
  icon: { tile: string; glyph: ReactNode } | { image: string };
  /** Only the screens of the demo have a route; the others are shown for a familiar menu. */
  to?: string;
  children?: { label: string; image: string }[];
}

/** Doping Hafıza's menu (docs/arayuz.md, Uygulama kabuğu) plus Eksiklerim; Dopingle opens from the top bar. */
const MENU: MenuItem[] = [
  { to: '/app', label: tr.menu.home, icon: { tile: 'bg-menu-home', glyph: <HomeIcon /> } },
  { label: tr.menu.lessons, icon: { tile: 'bg-menu-lessons', glyph: <BookIcon /> } },
  { to: '/app/denemeler', label: tr.menu.exams, icon: { tile: 'bg-menu-exams', glyph: <ExamIcon /> } },
  {
    to: '/app/eksiklerim',
    label: tr.menu.weakTopics,
    icon: { tile: 'bg-menu-weak-topics', glyph: <TargetIcon /> },
  },
  { label: tr.menu.way, icon: { image: dopingAssets.colorfulBird } },
  { label: tr.menu.guidance, icon: { tile: 'bg-menu-guidance', glyph: <ScreenIcon /> } },
  { label: tr.menu.stats, icon: { tile: 'bg-menu-stats', glyph: <PulseIcon /> } },
  { label: tr.menu.panel, icon: { image: dopingAssets.assessment } },
  {
    label: tr.menu.oneToOne,
    icon: { tile: 'bg-menu-one-to-one', glyph: <PhoneIcon /> },
    children: [
      { label: tr.menu.cozucu, image: dopingAssets.cozucu },
      { label: tr.menu.simdiAnladim, image: dopingAssets.simdiAnladim },
      { label: tr.menu.kocumYanimda, image: dopingAssets.kocumYanimda },
    ],
  },
  { label: tr.menu.help, icon: { tile: 'bg-menu-help', glyph: <HelpIcon /> } },
  { label: tr.menu.live, icon: { tile: 'bg-menu-live', glyph: <LiveIcon /> } },
  { label: tr.menu.stories, icon: { tile: 'bg-menu-stories', glyph: <PlayCircleIcon /> } },
];

const ITEM = 'flex w-full items-center gap-3 rounded-button px-2 py-2 text-left text-[15px]';

function ItemIcon({ icon }: { icon: MenuItem['icon'] }) {
  if ('image' in icon) {
    return <img src={icon.image} alt="" aria-hidden="true" className="size-7 rounded-lg object-contain" />;
  }
  return (
    <span
      className={`${icon.tile} flex size-7 items-center justify-center rounded-lg text-surface [&_svg]:size-4`}
    >
      {icon.glyph}
    </span>
  );
}

/** Left menu; an icon rail inside the Dopingle layer (docs/arayuz.md). */
export function Sidebar({ collapsed = false }: { collapsed?: boolean }) {
  const [openGroup, setOpenGroup] = useState<string | null>(null);

  return (
    <aside
      className={`${collapsed ? 'w-[92px] items-center' : 'w-[275px]'} flex shrink-0 flex-col gap-8 bg-surface px-3 py-8`}
    >
      {collapsed ? <span className="sr-only">{tr.app.logoAlt}</span> : <BrandLogo />}
      <nav aria-label={tr.menu.navigation}>
        <ul className={`flex flex-col ${collapsed ? 'gap-4' : 'gap-0.5'}`}>
          {MENU.map((item) => {
            const content = (
              <>
                <ItemIcon icon={item.icon} />
                {!collapsed && <span>{item.label}</span>}
              </>
            );
            if (item.to) {
              return (
                <li key={item.label}>
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
                </li>
              );
            }
            if (item.children && !collapsed) {
              const open = openGroup === item.label;
              return (
                <li key={item.label}>
                  <button
                    type="button"
                    aria-expanded={open}
                    onClick={() => {
                      setOpenGroup(open ? null : item.label);
                    }}
                    className={`${ITEM} hover:bg-surface-muted-active`}
                  >
                    {content}
                    <span className="[&_svg]:size-4">{open ? <ChevronUpIcon /> : <ChevronDownIcon />}</span>
                  </button>
                  {open && (
                    <ul className="flex flex-col gap-0.5 py-1 pl-10">
                      {item.children.map((child) => (
                        <li key={child.label}>
                          <span
                            aria-disabled="true"
                            title={tr.menu.notInDemo}
                            className={`${ITEM} cursor-default`}
                          >
                            <img src={child.image} alt="" aria-hidden="true" className="size-7" />
                            {child.label}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              );
            }
            return (
              <li key={item.label}>
                <span
                  aria-disabled="true"
                  title={collapsed ? item.label : tr.menu.notInDemo}
                  className={`${ITEM} cursor-default`}
                >
                  {content}
                </span>
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
}
