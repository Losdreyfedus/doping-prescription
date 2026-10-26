import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Schemas } from '@/shared/api/client';
import { API, meOf, server } from '@/test/server';

import { PROFILE_POLL_INTERVAL_MS, PROFILE_POLL_MAX_ATTEMPTS, useProfilePolling } from './learnerProfile';
import { queryKeys } from './queryKeys';

const ATTEMPT_ID = '3a1f0000-0000-4000-8000-000000000001';
const PRIOR_FETCHES = 15;

function profileAfter(attemptId: string | null): Schemas['ProfileView'] {
  return {
    userId: meOf('deniz').userId,
    personalizationEnabled: true,
    lastExamAttemptId: attemptId,
    updatedAt: null,
    topics: [],
  };
}

/** A query client whose profile entry was already fetched many times, as after a rehearsal without logout. */
function busyQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  for (let fetch = 0; fetch < PRIOR_FETCHES; fetch += 1) {
    queryClient.setQueryData(queryKeys.learnerProfile(), profileAfter(null));
  }
  return queryClient;
}

function renderPolling(queryClient: QueryClient) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return renderHook(() => useProfilePolling(ATTEMPT_ID), { wrapper });
}

/** Lets the pending request settle (on real time), then moves the fake clock one poll interval on. */
async function nextPoll() {
  await act(async () => {
    await new Promise((resolve) => {
      setTimeout(resolve, 0);
    });
    await vi.advanceTimersByTimeAsync(PROFILE_POLL_INTERVAL_MS);
  });
}

async function runPolls(count: number) {
  for (let poll = 0; poll < count; poll += 1) {
    await nextPoll();
  }
}

describe('useProfilePolling', () => {
  beforeEach(() => {
    // Only the poll interval is fake; the mocked network and the settling wait above run on real timers.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('counts its attempts from mount, so earlier fetches of the shared query do not stop the poll', async () => {
    let calls = 0;
    server.use(
      http.get(`${API}/api/learner/profile`, () => {
        calls += 1;
        return HttpResponse.json(profileAfter(calls < 3 ? null : ATTEMPT_ID));
      }),
    );
    const { result } = renderPolling(busyQueryClient());

    expect(result.current.gaveUp).toBe(false);
    await runPolls(3);

    expect(result.current.updated).toBe(true);
    expect(result.current.gaveUp).toBe(false);
    expect(calls).toBe(3);
  });

  it('gives up after the maximum number of attempts', async () => {
    let calls = 0;
    server.use(
      http.get(`${API}/api/learner/profile`, () => {
        calls += 1;
        return HttpResponse.json(profileAfter(null));
      }),
    );
    const { result } = renderPolling(busyQueryClient());

    await runPolls(PROFILE_POLL_MAX_ATTEMPTS + 2);

    expect(result.current.updated).toBe(false);
    expect(result.current.gaveUp).toBe(true);
    expect(calls).toBe(PROFILE_POLL_MAX_ATTEMPTS);
  });
});
