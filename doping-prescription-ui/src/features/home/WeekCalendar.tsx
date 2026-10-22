import { useState } from 'react';

import { tr } from '@/shared/i18n/tr';
import { ChevronLeftIcon, ChevronRightIcon } from '@/shared/ui/icons';

import { weekOf } from './week';

const monthTitle = new Intl.DateTimeFormat('tr-TR', { month: 'long', year: 'numeric' });
const DAYS_IN_WEEK = 7;

/** The week strip of the home page's right column: arrows move by a week, "Bugün" comes back; today is highlighted. */
export function WeekCalendar({ today }: { today: Date }) {
  const [weekOffset, setWeekOffset] = useState(0);
  const shown = new Date(today);
  shown.setDate(today.getDate() + weekOffset * DAYS_IN_WEEK);
  const title = monthTitle.format(shown);

  return (
    <section aria-label={tr.home.calendar} className="flex flex-col gap-3 px-3 pt-4">
      <div className="flex items-center justify-between">
        <h2 className="text-[22px]">{title.charAt(0).toLocaleUpperCase('tr') + title.slice(1)}</h2>
        <div className="flex items-center gap-1 [&_svg]:size-5">
          <button
            type="button"
            aria-label={tr.home.previousWeek}
            onClick={() => {
              setWeekOffset((offset) => offset - 1);
            }}
            className="rounded-full p-1 hover:bg-surface-muted"
          >
            <ChevronLeftIcon />
          </button>
          <button
            type="button"
            onClick={() => {
              setWeekOffset(0);
            }}
            className="rounded-button bg-surface-muted px-3 py-1 text-sm"
          >
            {tr.home.today}
          </button>
          <button
            type="button"
            aria-label={tr.home.nextWeek}
            onClick={() => {
              setWeekOffset((offset) => offset + 1);
            }}
            className="rounded-full p-1 hover:bg-surface-muted"
          >
            <ChevronRightIcon />
          </button>
        </div>
      </div>
      <ol className="grid grid-cols-7 text-center">
        {weekOf(shown).map((day, index) => {
          const isToday = day.toDateString() === today.toDateString();
          return (
            <li key={day.toISOString()} className="flex flex-col items-center gap-2">
              <span className="text-xs text-text-muted">{tr.home.weekDays[index]}</span>
              <span
                aria-current={isToday ? 'date' : undefined}
                className={`flex size-9 items-center justify-center rounded-full text-lg ${isToday ? 'bg-pin font-bold text-surface' : ''}`}
              >
                {day.getDate()}
              </span>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
