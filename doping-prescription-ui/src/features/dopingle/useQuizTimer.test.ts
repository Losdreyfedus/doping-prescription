import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useQuizTimer } from './useQuizTimer';

/** Each second's timeout is scheduled by the render after the previous tick. */
function passSeconds(seconds: number) {
  for (let second = 0; second < seconds; second += 1) {
    act(() => {
      vi.advanceTimersByTime(1000);
    });
  }
}

describe('useQuizTimer', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('counts down once a second and stops at zero', () => {
    const { result } = renderHook(() => useQuizTimer(3, true));

    passSeconds(2);
    expect(result.current).toEqual({ secondsLeft: 1, timedOut: false });
    passSeconds(5);
    expect(result.current).toEqual({ secondsLeft: 0, timedOut: true });
  });

  it('freezes while not running', () => {
    const { result, rerender } = renderHook(({ running }) => useQuizTimer(60, running), {
      initialProps: { running: true },
    });

    passSeconds(10);
    rerender({ running: false });
    passSeconds(120);

    expect(result.current).toEqual({ secondsLeft: 50, timedOut: false });
  });
});
