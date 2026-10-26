import { screen, within } from '@testing-library/react';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import type { Schemas } from '@/shared/api/client';
import { writeSession } from '@/shared/lib/session';
import { renderApp } from '@/test/renderApp';
import { API, meOf, server } from '@/test/server';

const points: Schemas['Summary'] = {
  total: 45,
  today: 45,
  dailyCap: 200,
  streak: { current: 3, best: 3 },
  recent: [],
};

function profileCard() {
  return within(screen.getByRole('region', { name: 'Profil' }));
}

describe('Ana Sayfa', () => {
  it('shows placeholders instead of zeros while the points load, then the points and streak', async () => {
    server.use(
      http.get(`${API}/api/points`, async () => {
        await delay(50);
        return HttpResponse.json(points);
      }),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app');

    await screen.findByRole('heading', { name: 'Deniz' });
    expect(profileCard().getAllByText('Yükleniyor…')).toHaveLength(2);
    expect(profileCard().queryByText('0')).not.toBeInTheDocument();

    expect(await profileCard().findByText('45')).toBeInTheDocument();
    expect(profileCard().getByText('Günlük seri:', { exact: false }).parentElement).toHaveTextContent('3');
  });

  it('says the points could not be loaded instead of showing 0', async () => {
    server.use(http.get(`${API}/api/points`, () => HttpResponse.json({}, { status: 500 })));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app');

    expect(await profileCard().findByRole('alert')).toHaveTextContent('Puanların yüklenemedi.');
    expect(profileCard().queryByText('0')).not.toBeInTheDocument();
  });
});
