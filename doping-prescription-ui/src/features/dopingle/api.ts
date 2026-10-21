import { useInfiniteQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useRef } from 'react';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

const FIRST_PAGE = '';

export type FeedPage = Schemas['FeedPage'];
export type FeedCard = Schemas['CardView'];
type InteractionEvent = Schemas['InteractionEvent'];

/** Pages of one feed session; opening Dopingle always starts a new session (docs/arayuz.md). */
export function useFeed(topicId: number | undefined, sessionKey: string) {
  return useInfiniteQuery({
    queryKey: queryKeys.feed(topicId, sessionKey),
    // the empty cursor stands for the first page, which starts a new session
    initialPageParam: FIRST_PAGE,
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET('/api/feed', {
          params: {
            query: {
              ...(pageParam === FIRST_PAGE ? { refresh: true } : { cursor: pageParam }),
              ...(topicId === undefined ? {} : { topicId }),
            },
          },
        }),
      ),
    getNextPageParam: (last) => (last.cards.length > 0 ? last.nextCursor : undefined),
    staleTime: Infinity,
    gcTime: 0,
  });
}

const FLUSH_INTERVAL_MS = 5000;

/**
 * Buffers interaction events and sends them every 5 seconds, when the tab is hidden and when Dopingle closes
 * (docs/api.md, POST /api/feed/interactions).
 */
export function useInteractionRecorder() {
  const buffer = useRef<InteractionEvent[]>([]);

  const flush = useCallback(() => {
    if (buffer.current.length === 0) {
      return;
    }
    const events = buffer.current;
    buffer.current = [];
    // Fire and forget: a lost batch only loses analytics, never the learner's progress.
    void api.POST('/api/feed/interactions', { body: { events } }).catch(() => undefined);
  }, []);

  useEffect(() => {
    const timer = setInterval(flush, FLUSH_INTERVAL_MS);
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') {
        flush();
      }
    };
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisibility);
      flush();
    };
  }, [flush]);

  const record = useCallback((impressionId: string, event: InteractionEvent['event'], watchMs?: number) => {
    buffer.current.push({
      clientEventId: crypto.randomUUID(),
      impressionId,
      event,
      watchMs: watchMs ?? null,
      clientTs: new Date().toISOString(),
    });
  }, []);

  return { record, flush };
}
