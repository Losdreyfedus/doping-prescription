import { type InfiniteData, useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useRef } from 'react';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

const FIRST_PAGE = '';

export type FeedPage = Schemas['FeedPage'];
export type FeedCard = Schemas['CardView'];
type InteractionEvent = Schemas['InteractionEvent'];

type FeedKey = ReturnType<typeof queryKeys.feed>;

/**
 * Pages of one feed session; opening Dopingle always starts a new session (docs/arayuz.md). {@code saved} shows only
 * the learner's saved videos (Kaydedilen).
 */
export function useFeed(topicId: number | undefined, saved: boolean, sessionKey: string) {
  return useInfiniteQuery({
    queryKey: queryKeys.feed(topicId, saved, sessionKey),
    // the empty cursor stands for the first page, which starts a new session
    initialPageParam: FIRST_PAGE,
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET('/api/feed', {
          params: {
            query: {
              ...(pageParam === FIRST_PAGE ? { refresh: true } : { cursor: pageParam }),
              ...(topicId === undefined ? {} : { topicId }),
              ...(saved ? { saved } : {}),
            },
          },
        }),
      ),
    getNextPageParam: (last) => (last.cards.length > 0 ? last.nextCursor : undefined),
    // A session's pages never refetch (not on focus either): a refetch would start a new session under the cards.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    gcTime: 0,
  });
}

/**
 * Saves or removes a card's video (docs/api.md, PUT/DELETE /api/feed/saved/:contentId). The bookmark changes at once in
 * the session's cached pages and goes back if the request fails.
 */
export function useSaveVideo(feedKey: FeedKey) {
  const queryClient = useQueryClient();
  const markSaved = (contentId: number, saved: boolean) => {
    queryClient.setQueryData<InfiniteData<FeedPage>>(feedKey, (data) =>
      data
        ? {
            ...data,
            pages: data.pages.map((page) => ({
              ...page,
              cards: page.cards.map((card) =>
                card.content.contentId === contentId ? { ...card, saved } : card,
              ),
            })),
          }
        : data,
    );
  };
  return useMutation({
    mutationFn: async ({ contentId, saved }: { contentId: number; saved: boolean }) => {
      const request = { params: { path: { contentId } } };
      const { response, error } = saved
        ? await api.PUT('/api/feed/saved/{contentId}', request)
        : await api.DELETE('/api/feed/saved/{contentId}', request);
      if (!response.ok) {
        unwrap({ error, response });
      }
    },
    onMutate: ({ contentId, saved }) => {
      markSaved(contentId, saved);
    },
    onError: (_error, { contentId, saved }) => {
      markSaved(contentId, !saved);
    },
  });
}

const FLUSH_INTERVAL_MS = 5000;

/**
 * Buffers interaction events and sends them every 5 seconds, when the tab is hidden and when Dopingle closes
 * (docs/api.md, POST /api/feed/interactions).
 */
export function useInteractionRecorder() {
  const buffer = useRef<InteractionEvent[]>([]);

  /** Sends the buffered events; the promise settles when they are stored (or failed: a lost batch only loses analytics). */
  const flush = useCallback(async () => {
    if (buffer.current.length === 0) {
      return;
    }
    const events = buffer.current;
    buffer.current = [];
    await api.POST('/api/feed/interactions', { body: { events } }).catch(() => undefined);
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      void flush();
    }, FLUSH_INTERVAL_MS);
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') {
        void flush();
      }
    };
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisibility);
      void flush();
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

export type RecordInteraction = ReturnType<typeof useInteractionRecorder>['record'];
