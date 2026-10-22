import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { writeSession } from '@/shared/lib/session';
import { renderApp } from '@/test/renderApp';
import { API, meOf, server } from '@/test/server';

describe('admin demo control', () => {
  it('resets a student, loads a persona and shows the answer key', async () => {
    const calls: { path: string; body: unknown }[] = [];
    const record = async ({ request }: { request: Request }) => {
      calls.push({ path: new URL(request.url).pathname, body: await request.json() });
      return new HttpResponse(null, { status: 204 });
    };
    server.use(
      http.get(`${API}/api/admin/demo/students`, () =>
        HttpResponse.json([
          {
            slug: 'deniz',
            displayName: 'Deniz',
            personalizationEnabled: true,
            totalPoints: 15,
            hasExam: true,
          },
        ]),
      ),
      http.get(`${API}/api/admin/demo/personas`, () => HttpResponse.json(['NEW', 'SCRIPTED_IN_PROGRESS'])),
      http.get(`${API}/api/admin/demo/answer-key`, () =>
        HttpResponse.json([
          { orderNo: 1, topic: 'Temel Kavramlar', correctKey: 'B', scenarioKey: 'B' },
          { orderNo: 8, topic: 'Permütasyon-Kombinasyon', correctKey: 'C', scenarioKey: null },
        ]),
      ),
      http.get(`${API}/api/admin/content-effectiveness`, () =>
        HttpResponse.json([
          {
            contentId: 33,
            title: 'Olasılığın temeli',
            topic: 'Olasılık',
            answers: 3,
            correct: 2,
            correctRate: 2 / 3,
          },
        ]),
      ),
      http.post(`${API}/api/admin/demo/reset`, record),
      http.post(`${API}/api/admin/demo/persona`, record),
    );
    writeSession({ token: 'token-admin', user: meOf('admin') });
    renderApp('/admin');

    const card = within(await screen.findByRole('listitem'));
    expect(card.getByText('Deniz')).toBeInTheDocument();
    expect(card.getByText('15')).toBeInTheDocument();

    await userEvent.click(card.getByRole('button', { name: 'Sıfırla' }));
    await userEvent.click(card.getByRole('button', { name: 'Persona yükle' }));

    await expect.poll(() => calls.length).toBe(2);
    expect(calls).toEqual([
      { path: '/api/admin/demo/reset', body: { slug: 'deniz' } },
      { path: '/api/admin/demo/persona', body: { slug: 'deniz', persona: 'SCRIPTED_IN_PROGRESS' } },
    ]);
    expect(screen.getByText('Sunum cevap anahtarı')).toBeInTheDocument();
    expect(screen.getByText('Permütasyon-Kombinasyon').closest('tr')).toHaveTextContent('Boş');
    const effectiveness = screen.getByRole('table', { name: 'Video etkinliği' });
    expect(within(effectiveness).getByText('Olasılığın temeli').closest('tr')).toHaveTextContent('%67');
  });
});
