import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { weekOf } from './week';
import { WeekCalendar } from './WeekCalendar';

describe('WeekCalendar', () => {
  it('starts the week on Monday, also when today is Sunday', () => {
    const sunday = new Date(2026, 9, 25);

    const days = weekOf(sunday).map((day) => day.getDate());

    expect(days).toEqual([19, 20, 21, 22, 23, 24, 25]);
  });

  it('shows the month and marks today', () => {
    render(<WeekCalendar today={new Date(2026, 9, 21)} />);

    expect(screen.getByRole('heading', { name: 'Ekim 2026' })).toBeInTheDocument();
    expect(screen.getByText('21')).toHaveAttribute('aria-current', 'date');
    expect(screen.getByText('20')).not.toHaveAttribute('aria-current');
  });
});
