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
  saved: false,
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

  it('saves a video from its bookmark and opens the saved videos from Kaydedilen', async () => {
    const saved: string[] = [];
    const feedRequests: URL[] = [];
    server.use(
      http.get(`${API}/api/feed`, ({ request }) => {
        const url = new URL(request.url);
        feedRequests.push(url);
        if (url.searchParams.has('cursor')) {
          return HttpResponse.json({ ...page, cards: [] });
        }
        return HttpResponse.json(
          url.searchParams.get('saved') === 'true'
            ? { ...page, cards: [{ ...page.cards[0], saved: true }] }
            : page,
        );
      }),
      http.put(`${API}/api/feed/saved/:contentId`, ({ params }) => {
        saved.push(String(params.contentId));
        return new HttpResponse(null, { status: 204 });
      }),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle');

    const bookmark = await screen.findByRole('button', { name: 'Kaydet' });
    expect(bookmark).toHaveAttribute('aria-pressed', 'false');
    await userEvent.click(bookmark);

    expect(await screen.findByRole('button', { name: 'Kaydedilenlerden çıkar' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(saved).toEqual(['30']);

    await userEvent.click(screen.getByRole('link', { name: 'Kaydedilen' }));

    expect(await screen.findByRole('link', { name: 'Kaydedilen' })).toHaveAttribute('aria-current', 'page');
    await expect.poll(() => feedRequests.at(-1)?.searchParams.get('saved')).toBe('true');
    expect(await screen.findByRole('button', { name: 'Kaydedilenlerden çıkar' })).toBeInTheDocument();
  });

  it('puts the bookmark back when saving fails', async () => {
    server.use(
      http.get(`${API}/api/feed`, ({ request }) =>
        HttpResponse.json(new URL(request.url).searchParams.has('cursor') ? { ...page, cards: [] } : page),
      ),
      http.put(`${API}/api/feed/saved/:contentId`, () =>
        HttpResponse.json({ status: 500, detail: 'Sunucu hatası' }, { status: 500 }),
      ),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle');

    await userEvent.click(await screen.findByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByText('Video kaydedilemedi, tekrar dene.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kaydet' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('has no sound to turn on for a simulated video', async () => {
    server.use(
      http.get(`${API}/api/feed`, ({ request }) =>
        HttpResponse.json(new URL(request.url).searchParams.has('cursor') ? { ...page, cards: [] } : page),
      ),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle');

    expect(await screen.findByRole('button', { name: 'Bu videoda ses yok' })).toBeDisabled();
  });

  it('shows how to save when there are no saved videos', async () => {
    server.use(http.get(`${API}/api/feed`, () => HttpResponse.json({ ...page, cards: [] })));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle?saved=1');

    expect(await screen.findByText(/Henüz kaydettiğin video yok/)).toBeInTheDocument();
  });

  it('says so when there is nothing to show', async () => {
    server.use(http.get(`${API}/api/feed`, () => HttpResponse.json({ ...page, cards: [] })));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle');

    expect(await screen.findByText('Şu an gösterilecek video yok.')).toBeInTheDocument();
  });

  it('offers a retry when the feed cannot be loaded', async () => {
    let calls = 0;
    server.use(
      http.get(`${API}/api/feed`, () => {
        calls += 1;
        return calls === 1
          ? HttpResponse.json({ status: 500, detail: 'Sunucu hatası' }, { status: 500 })
          : HttpResponse.json(page);
      }),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/dopingle');

    expect(await screen.findByRole('alert')).toHaveTextContent('Akış yüklenemedi.');
    await userEvent.click(screen.getByRole('button', { name: 'Tekrar dene' }));

    expect(await screen.findByText('Son denemende Olasılık: 3 soruda 0 doğru')).toBeInTheDocument();
  });
});
