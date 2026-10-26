import { act, renderHook } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Schemas } from '@/shared/api/client';
import { API, server } from '@/test/server';

import { useInteractionRecorder } from './api';

const IMPRESSION_ID = '00000000-0000-4000-8000-000000000011';

/** Collects the batches the recorder posts. */
function captureBatches() {
  const batches: Schemas['InteractionBatch'][] = [];
  server.use(
    http.post(`${API}/api/feed/interactions`, async ({ request }) => {
      const body = await request.json();
      if (typeof body === 'object' && body !== null && 'events' in body && Array.isArray(body.events)) {
        batches.push({ events: body.events.filter(isInteractionEvent) });
      }
      return new HttpResponse(null, { status: 202 });
    }),
  );
  return batches;
}

function isInteractionEvent(value: unknown): value is Schemas['InteractionEvent'] {
  return typeof value === 'object' && value !== null && 'event' in value && 'impressionId' in value;
}

function setVisibility(state: DocumentVisibilityState) {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => state });
  document.dispatchEvent(new Event('visibilitychange'));
}

describe('useInteractionRecorder', () => {
  beforeEach(() => {
    // Only the flush interval is faked; MSW needs real promises and timeouts to answer.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
  });
  afterEach(() => {
    vi.useRealTimers();
    setVisibility('visible');
  });

  it('sends the buffered events as one batch every 5 seconds', async () => {
    const batches = captureBatches();
    const { result } = renderHook(() => useInteractionRecorder());

    act(() => {
      result.current.record(IMPRESSION_ID, 'VIEW_START', 0);
      result.current.record(IMPRESSION_ID, 'LIKE');
    });
    vi.advanceTimersByTime(4999);
    expect(batches).toHaveLength(0);

    vi.advanceTimersByTime(1);
    await vi.waitFor(() => {
      expect(batches).toHaveLength(1);
    });
    expect(batches[0]?.events.map((event) => [event.event, event.watchMs])).toEqual([
      ['VIEW_START', 0],
      ['LIKE', null],
    ]);
    expect(new Set(batches[0]?.events.map((event) => event.clientEventId)).size).toBe(2);
  });

  it('sends nothing when the buffer is empty', async () => {
    const batches = captureBatches();
    const { result } = renderHook(() => useInteractionRecorder());

    vi.advanceTimersByTime(15_000);
    await act(() => result.current.flush());

    expect(batches).toHaveLength(0);
  });

  it('flushes when the tab is hidden and when Dopingle closes', async () => {
    const batches = captureBatches();
    const { result, unmount } = renderHook(() => useInteractionRecorder());

    act(() => {
      result.current.record(IMPRESSION_ID, 'SKIP', 1200);
      setVisibility('hidden');
    });
    await vi.waitFor(() => {
      expect(batches).toHaveLength(1);
    });

    act(() => {
      result.current.record(IMPRESSION_ID, 'VIEW_COMPLETE', 60_000);
    });
    unmount();
    await vi.waitFor(() => {
      expect(batches).toHaveLength(2);
    });
    expect(batches.map((batch) => batch.events.map((event) => event.event))).toEqual([
      ['SKIP'],
      ['VIEW_COMPLETE'],
    ]);
  });
});
