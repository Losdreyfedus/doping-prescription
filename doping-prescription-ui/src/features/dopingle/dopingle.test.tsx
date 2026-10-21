import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import type { Schemas } from '@/shared/api/client';
import { writeSession } from '@/shared/lib/session';
import { renderApp } from '@/test/renderApp';
import { API, meOf, server } from '@/test/server';

const card = (
  position: number,
  topicId: number,
  topicName: string,
  reasonText: string,
): Schemas['CardView'] => ({
  impressionId: `00000000-0000-4000-8000-0000000000${String(10 + position)}`,
  position,
  content: {
    contentId: 30 + position,
    title: `Video ${String(position)}`,
    teacherName: 'Ayşe Öğretmen',
    videoUrl: null,
    durationSec: 60,
    contentKind: 'CONCEPT',
  },
  topic: { topicId, code: `TYT.MAT.${String(topicId)}`, name: topicName },
  pool: 'WEAK_TOPIC',
  reasonText,
  hasMicroQuiz: true,
  pointsAvailable: 15,
});

const page: Schemas['FeedPage'] = {
  sessionId: '00000000-0000-4000-8000-000000000099',
  personalized: true,
  algoVersion: 'v1',
  nextCursor: 'next',
  demoVideoSpeed: 4,
  cards: [
    card(0, 9, 'Olasılık', 'Son denemende Olasılık: 3 soruda 0 doğru'),
    card(1, 10, 'Fonksiyonlar', "TYT'de sık çıkan konu: Fonksiyonlar"),
  ],
};

describe('Dopingle', () => {
  it('shows the card with its reason and records a skip when moving on quickly', async () => {
    const requests: URL[] = [];
    const recorded: unknown[] = [];
    server.use(
      http.get(`${API}/api/feed`, ({ request }) => {
        const url = new URL(request.url);
        requests.push(url);
        return HttpResponse.json(url.searchParams.has('cursor') ? { ...page, cards: [] } : page);
      }),
      http.post(`${API}/api/feed/interactions`, async ({ request }) => {
        recorded.push(await request.json());
        return new HttpResponse(null, { status: 202 });
      }),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle?topicId=9');

    expect(await screen.findByText('Son denemende Olasılık: 3 soruda 0 doğru')).toBeInTheDocument();
    expect(screen.getByText('+15 puan')).toBeInTheDocument();
    expect(requests[0]?.searchParams.get('topicId')).toBe('9');
    expect(requests[0]?.searchParams.get('refresh')).toBe('true');

    await userEvent.keyboard('{ArrowDown}');

    expect(await screen.findByText("TYT'de sık çıkan konu: Fonksiyonlar")).toBeInTheDocument();
    await expect.poll(() => JSON.stringify(recorded)).toContain('"event":"SKIP"');
    expect(JSON.stringify(recorded)).toContain('"event":"VIEW_START"');
  });

  it('says so when there is nothing to show', async () => {
    server.use(http.get(`${API}/api/feed`, () => HttpResponse.json({ ...page, cards: [] })));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle');

    expect(await screen.findByText('Şu an gösterilecek video yok.')).toBeInTheDocument();
  });
});
