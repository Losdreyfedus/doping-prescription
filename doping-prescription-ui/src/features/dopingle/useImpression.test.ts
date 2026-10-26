import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useImpression } from './useImpression';

const IMPRESSION_ID = '00000000-0000-4000-8000-000000000011';

function renderImpression() {
  const record = vi.fn();
  const elapsedRef = { current: 0 };
  const hook = renderHook(() => useImpression({ impressionId: IMPRESSION_ID, record, elapsedRef }), {
    reactStrictMode: true,
  });
  return { ...hook, record, elapsedRef };
}

describe('useImpression', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('records VIEW_START once even when StrictMode runs the effect twice', () => {
    const { record } = renderImpression();

    expect(record).toHaveBeenCalledOnce();
    expect(record).toHaveBeenCalledWith(IMPRESSION_ID, 'VIEW_START', 0);
  });

  it('records a skip when the card is left within 3 seconds', () => {
    const { result, record, elapsedRef } = renderImpression();
    elapsedRef.current = 4.5;

    act(() => {
      vi.advanceTimersByTime(2000);
      result.current.leave();
    });

    expect(record).toHaveBeenLastCalledWith(IMPRESSION_ID, 'SKIP', 4500);
  });

  it('records no skip after the skip window or once the video is complete', () => {
    const { result, record } = renderImpression();

    act(() => {
      result.current.complete(40);
      result.current.leave();
    });
    expect(record).toHaveBeenLastCalledWith(IMPRESSION_ID, 'VIEW_COMPLETE', 40_000);

    const late = renderImpression();
    act(() => {
      vi.advanceTimersByTime(3000);
      late.result.current.leave();
    });
    expect(late.record).toHaveBeenCalledOnce();
  });
});
