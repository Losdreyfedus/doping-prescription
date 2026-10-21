const DAYS_IN_WEEK = 7;

/** Monday-first days of the week that contains the given date. */
export function weekOf(today: Date): Date[] {
  const mondayOffset = (today.getDay() + DAYS_IN_WEEK - 1) % DAYS_IN_WEEK;
  return Array.from({ length: DAYS_IN_WEEK }, (_, index) => {
    const day = new Date(today);
    day.setDate(today.getDate() - mondayOffset + index);
    return day;
  });
}
