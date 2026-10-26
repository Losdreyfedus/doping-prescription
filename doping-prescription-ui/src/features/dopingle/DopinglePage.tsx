import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';

import { queryKeys } from '@/shared/api/queryKeys';
import { tr } from '@/shared/i18n/tr';

import { type FeedCard, useFeed, useSaveVideo } from './api';
import { Player } from './Player';

const DEFAULT_SPEED = 4;
/** docs/arayuz.md: the next page loads when 3 cards are left. */
const PREFETCH_REMAINING = 3;

function toTopicId(value: string | null): number | undefined {
  const parsed = Number(value);
  return value && Number.isInteger(parsed) ? parsed : undefined;
}

/**
 * Route /app/dopingle inside the Dopingle layer (docs/arayuz.md, Dopingle). `?saved=1` shows only the saved videos
 * (Kaydedilen), `?topicId=` one topic's videos.
 */
export function DopinglePage() {
  const [params] = useSearchParams();
  const topicId = toTopicId(params.get('topicId'));
  const savedOnly = params.get('saved') === '1';
  // Every opening is a new session; ?refresh=1 needs nothing extra (docs/api.md).
  const [sessionKey] = useState(() => crypto.randomUUID());
  const feed = useFeed(topicId, savedOnly, sessionKey);
  const saveVideo = useSaveVideo(queryKeys.feed(topicId, savedOnly, sessionKey));
  const [muted, setMuted] = useState(false);
  const cards = feed.data?.pages.flatMap((page) => page.cards) ?? [];
  const speed = feed.data?.pages[0]?.demoVideoSpeed ?? DEFAULT_SPEED;
  const [index, setIndex] = useState(0);
  const current = cards.at(index);

  const { hasNextPage, isFetchingNextPage, isFetchNextPageError, fetchNextPage } = feed;
  useEffect(() => {
    // A failed next page is not retried in a loop; the cards already loaded keep playing.
    if (
      cards.length > 0 &&
      cards.length - index <= PREFETCH_REMAINING &&
      hasNextPage &&
      !isFetchingNextPage &&
      !isFetchNextPageError
    ) {
      void fetchNextPage();
    }
  }, [cards.length, index, hasNextPage, isFetchingNextPage, isFetchNextPageError, fetchNextPage]);

  const go = (delta: number) => {
    setIndex((position) => Math.max(0, Math.min(cards.length - 1, position + delta)));
  };

  if (feed.isPending) {
    return <p className="flex h-full items-center justify-center text-band-unknown">{tr.dopingle.loading}</p>;
  }
  if (feed.isError && cards.length === 0) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-4">
        <p role="alert" className="flex flex-col items-center gap-1 text-center">
          <strong>{tr.dopingle.loadFailed}</strong>
          {feed.error.message}
        </p>
        <button
          type="button"
          disabled={feed.isFetching}
          onClick={() => {
            void feed.refetch();
          }}
          className="rounded-button bg-surface px-5 py-2 font-semibold text-ink focus-visible:outline-2 focus-visible:outline-select disabled:opacity-60"
        >
          {tr.dopingle.retry}
        </button>
      </div>
    );
  }
  if (!current) {
    return (
      <p className="flex h-full items-center justify-center text-band-unknown">
        {savedOnly ? tr.dopingle.savedEmpty : tr.dopingle.empty}
      </p>
    );
  }
  return (
    <>
      <Player
        key={current.impressionId}
        card={current}
        speed={speed}
        onMove={go}
        muted={muted}
        onMutedChange={setMuted}
        saving={saveVideo.isPending}
        onToggleSave={(card: FeedCard) => {
          saveVideo.mutate({ contentId: card.content.contentId, saved: !card.saved });
        }}
      />
      {saveVideo.isError && (
        <p
          role="alert"
          className="absolute top-5 left-1/2 -translate-x-1/2 rounded-button bg-surface/15 px-4 py-2"
        >
          {tr.dopingle.saveFailed}
        </p>
      )}
    </>
  );
}
