import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useSimulatedPlayer } from './useSimulatedPlayer';

describe('useSimulatedPlayer', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('fills a 60 s video in 15 s at speed 4 and completes once', () => {
    const onComplete = vi.fn();
    const { result } = renderHook(() =>
      useSimulatedPlayer({ durationSec: 60, speed: 4, active: true, onComplete }),
    );

    act(() => {
      vi.advanceTimersByTime(7500);
    });
    expect(result.current.progress).toBeCloseTo(0.5, 2);
    expect(result.current.elapsedSec).toBeCloseTo(30, 0);

    act(() => {
      vi.advanceTimersByTime(10_000);
    });
    expect(result.current.progress).toBe(1);
    expect(result.current.playing).toBe(false);
    expect(onComplete).toHaveBeenCalledTimes(1);
  });

  it('stands still while paused or inactive', () => {
    const { result, rerender } = renderHook(
      ({ active }) => useSimulatedPlayer({ durationSec: 60, speed: 4, active, onComplete: vi.fn() }),
      { initialProps: { active: false } },
    );
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(result.current.progress).toBe(0);

    rerender({ active: true });
    act(() => {
      result.current.toggle();
    });
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(result.current.progress).toBe(0);
  });
});
