import type { SVGProps } from 'react';

/** Simple stroke icons drawn for the demo (no downloaded assets, AGENTS.md rule 11). */
function Icon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="size-5"
      {...props}
    />
  );
}

export function PlayIcon() {
  return (
    <Icon>
      <rect x="5" y="2" width="14" height="20" rx="3" />
      <path d="M10 9l5 3-5 3z" />
    </Icon>
  );
}

export function ExamIcon() {
  return (
    <Icon>
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path d="M9 8h6M9 12h6M9 16h3" />
    </Icon>
  );
}

export function TargetIcon() {
  return (
    <Icon>
      <circle cx="12" cy="12" r="8" />
      <circle cx="12" cy="12" r="3" />
    </Icon>
  );
}

export function SearchIcon() {
  return (
    <Icon>
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-3.5-3.5" />
    </Icon>
  );
}

export function LogoutIcon() {
  return (
    <Icon>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
    </Icon>
  );
}

export function HomeIcon() {
  return (
    <Icon>
      <rect x="4" y="4" width="7" height="7" rx="1.5" />
      <rect x="13" y="4" width="7" height="7" rx="1.5" />
      <rect x="4" y="13" width="7" height="7" rx="1.5" />
      <rect x="13" y="13" width="7" height="7" rx="1.5" />
    </Icon>
  );
}

export function BookIcon() {
  return (
    <Icon>
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path d="M5 16h14" />
    </Icon>
  );
}

export function SparkIcon() {
  return (
    <Icon>
      <path d="M12 3v18M5 8l7 4 7-4M5 16l7-4 7 4" />
    </Icon>
  );
}

export function ScreenIcon() {
  return (
    <Icon>
      <rect x="3" y="4" width="18" height="13" rx="2" />
      <path d="M10 9l4 2.5-4 2.5z" />
    </Icon>
  );
}

export function PulseIcon() {
  return (
    <Icon>
      <path d="M3 12h4l2-6 4 12 2-6h6" />
    </Icon>
  );
}

export function BoardIcon() {
  return (
    <Icon>
      <rect x="3" y="3" width="18" height="13" rx="2" />
      <path d="M8 12V9M12 12V7M16 12v-2M9 21l3-5 3 5" />
    </Icon>
  );
}

export function PhoneIcon() {
  return (
    <Icon>
      <rect x="7" y="2" width="10" height="20" rx="2" />
      <path d="M11 18h2" />
    </Icon>
  );
}

export function HelpIcon() {
  return (
    <Icon>
      <circle cx="12" cy="12" r="9" />
      <path d="M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6M12 17h.01" />
    </Icon>
  );
}

export function LiveIcon() {
  return (
    <Icon>
      <rect x="3" y="6" width="13" height="12" rx="2" />
      <path d="M16 10l5-3v10l-5-3" />
    </Icon>
  );
}

export function PlayCircleIcon() {
  return (
    <Icon>
      <circle cx="12" cy="12" r="9" />
      <path d="M10 8.5l5 3.5-5 3.5z" />
    </Icon>
  );
}

export function BoltIcon() {
  return (
    <Icon>
      <path d="M13 2L4 14h7l-1 8 9-12h-7z" />
    </Icon>
  );
}

export function MailIcon() {
  return (
    <Icon>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M3 7l9 6 9-6" />
    </Icon>
  );
}

export function CalendarIcon() {
  return (
    <Icon>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18M8 3v4M16 3v4M9 15l2 2 4-4" />
    </Icon>
  );
}

export function BellIcon() {
  return (
    <Icon>
      <path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4" />
    </Icon>
  );
}

export function ChevronDownIcon() {
  return (
    <Icon>
      <path d="M6 9l6 6 6-6" />
    </Icon>
  );
}

export function CloseIcon() {
  return (
    <Icon>
      <path d="M6 6l12 12M18 6L6 18" />
    </Icon>
  );
}

export function FilterIcon() {
  return (
    <Icon>
      <path d="M6 4v16M12 4v16M18 4v16M4 9h4M10 15h4M16 8h4" />
    </Icon>
  );
}

export function BookmarkIcon() {
  return (
    <Icon>
      <path d="M6 3h12v18l-6-4-6 4z" />
    </Icon>
  );
}

export function MegaphoneIcon() {
  return (
    <Icon>
      <path d="M3 10v4h4l8 5V5L7 10zM18 9a4 4 0 0 1 0 6" />
    </Icon>
  );
}

export function HeartIcon({ filled = false }: { filled?: boolean }) {
  return (
    <Icon fill={filled ? 'currentColor' : 'none'}>
      <path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z" />
    </Icon>
  );
}

export function SpeakerIcon() {
  return (
    <Icon>
      <path d="M4 9v6h4l5 4V5L8 9zM17 9l4 6M21 9l-4 6" />
    </Icon>
  );
}

export function ChevronUpIcon() {
  return (
    <Icon>
      <path d="M6 15l6-6 6 6" />
    </Icon>
  );
}

export function ChevronLeftIcon() {
  return (
    <Icon>
      <path d="M15 6l-6 6 6 6" />
    </Icon>
  );
}

export function ChevronRightIcon() {
  return (
    <Icon>
      <path d="M9 6l6 6-6 6" />
    </Icon>
  );
}

export function UserIcon() {
  return (
    <Icon>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </Icon>
  );
}

export function SettingsIcon() {
  return (
    <Icon>
      <circle cx="12" cy="12" r="3" />
      <path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1" />
    </Icon>
  );
}
