import { tr } from '@/shared/i18n/tr';

import { weekOf } from './week';

const monthTitle = new Intl.DateTimeFormat('tr-TR', { month: 'long', year: 'numeric' });

/** The current week strip of the home page's right column; today is highlighted. */
export function WeekCalendar({ today }: { today: Date }) {
  const title = monthTitle.format(today);
  return (
    <section aria-label={tr.home.calendar} className="flex flex-col gap-3 px-3 pt-4">
      <h2 className="text-2xl">{title.charAt(0).toLocaleUpperCase('tr') + title.slice(1)}</h2>
      <ol className="grid grid-cols-7 text-center">
        {weekOf(today).map((day, index) => {
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
