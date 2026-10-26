import { type RefObject, useCallback, useEffect, useRef } from 'react';

import type { RecordInteraction } from './api';

/** docs/arayuz.md: changing video within 3 seconds (wall clock) of starting counts as a skip. */
const SKIP_WINDOW_MS = 3000;

interface Options {
  impressionId: string;
  record: RecordInteraction;
  /** Seconds of the video played so far, read when the student leaves the card. */
  elapsedRef: RefObject<number>;
}

/**
 * The view events of one card: VIEW_START when it appears, VIEW_COMPLETE when the video ends and SKIP when the student
 * leaves it within the skip window without finishing it.
 */
export function useImpression({ impressionId, record, elapsedRef }: Options) {
  const startedFor = useRef<string | null>(null);
  const startedAt = useRef(0);
  const completed = useRef(false);

  useEffect(() => {
    // StrictMode runs effects twice in development; one card on screen is one VIEW_START.
    if (startedFor.current === impressionId) {
      return;
    }
    startedFor.current = impressionId;
    startedAt.current = Date.now();
    record(impressionId, 'VIEW_START', 0);
  }, [record, impressionId]);

  const complete = useCallback(
    (durationSec: number) => {
      completed.current = true;
      record(impressionId, 'VIEW_COMPLETE', durationSec * 1000);
    },
    [record, impressionId],
  );

  const leave = useCallback(() => {
    if (!completed.current && Date.now() - startedAt.current < SKIP_WINDOW_MS) {
      record(impressionId, 'SKIP', Math.round(elapsedRef.current * 1000));
    }
  }, [record, impressionId, elapsedRef]);

  return { complete, leave };
}
