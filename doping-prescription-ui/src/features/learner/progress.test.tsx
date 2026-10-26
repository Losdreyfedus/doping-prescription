import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import type { LearnerProfile, TopicProfile } from '@/shared/api/learnerProfile';
import { writeSession } from '@/shared/lib/session';
import { renderApp } from '@/test/renderApp';
import { API, meOf, server } from '@/test/server';

const topic = (overrides: Partial<TopicProfile> & Pick<TopicProfile, 'topicId' | 'name'>): TopicProfile => ({
  code: `TYT.MAT.${String(overrides.topicId)}`,
  mastery: 0.5,
  confidence: 0,
  band: 'UNKNOWN',
  trend: 'FLAT',
  lastSignalAt: null,
  readyContentCount: 4,
  ...overrides,
});

const profile: LearnerProfile = {
  userId: meOf('deniz').userId,
  personalizationEnabled: true,
  lastExamAttemptId: '3a1f0000-0000-4000-8000-000000000001',
  updatedAt: '2026-10-21T10:04:01Z',
  topics: [
    topic({
      topicId: 9,
      name: 'Olasılık',
      mastery: 0.172,
      band: 'WEAK',
      trend: 'DOWN',
      readyContentCount: 6,
    }),
    topic({ topicId: 1, name: 'Temel Kavramlar', mastery: 0.85, band: 'STRONG', trend: 'UP' }),
    topic({ topicId: 10, name: 'Fonksiyonlar' }),
  ],
};

describe('Gelişimim', () => {
  it('lists topics in API order with band, trend, ready videos and a study link', async () => {
    server.use(http.get(`${API}/api/learner/profile`, () => HttpResponse.json(profile)));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/gelisimim');

    const list = await screen.findByRole('list', { name: 'Konu ustalıkları' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows.map((row) => row.getAttribute('data-band'))).toEqual(['WEAK', 'STRONG', 'UNKNOWN']);

    const weak = rowAt(rows, 0);
    expect(weak.getByRole('meter', { name: 'Olasılık ustalığı' })).toHaveAttribute('aria-valuenow', '17');
    expect(weak.getByText('Düşüyor')).toBeInTheDocument();
    expect(weak.getByText('Bu konudan 6 video hazır')).toBeInTheDocument();
    expect(weak.getByRole('link', { name: 'Olasılık çalış' })).toHaveAttribute(
      'href',
      '/app/dopingle?topicId=9',
    );

    const unknown = rowAt(rows, 2);
    expect(unknown.getByText('Henüz veri yok, bir deneme çöz')).toBeInTheDocument();
    expect(unknown.queryByRole('meter')).not.toBeInTheDocument();
  });

  it('says so when the profile has no topics yet', async () => {
    server.use(http.get(`${API}/api/learner/profile`, () => HttpResponse.json({ ...profile, topics: [] })));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/gelisimim');

    expect(
      await screen.findByText('Henüz konu bilgisi yok. Bir deneme çözünce konuların burada görünür.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Konu ustalıkları' })).not.toBeInTheDocument();
  });

  it('opens Dopingle for a topic and returns to Gelişimim when it closes', async () => {
    server.use(http.get(`${API}/api/learner/profile`, () => HttpResponse.json(profile)));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    const router = renderApp('/app/gelisimim');

    await userEvent.click(await screen.findByRole('link', { name: 'Olasılık çalış' }));
    expect(router.state.location.pathname).toBe('/app/dopingle');
    await userEvent.click(screen.getByRole('button', { name: "Dopingle'ı kapat" }));

    expect(router.state.location.pathname).toBe('/app/gelisimim');
  });

  it('shows an error when the profile cannot be loaded', async () => {
    server.use(http.get(`${API}/api/learner/profile`, () => HttpResponse.json({}, { status: 500 })));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/gelisimim');

    expect(await screen.findByRole('alert')).toHaveTextContent('Veriler yüklenemedi.');
  });
});

function rowAt(rows: HTMLElement[], index: number) {
  const row = rows[index];
  if (!row) {
    throw new Error(`no row at ${String(index)}`);
  }
  return within(row);
}
