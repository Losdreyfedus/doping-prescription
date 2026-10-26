import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { useRealVideo } from './useRealVideo';

/** A video element as a recorded WebM file presents it: duration Infinity until the end. */
function recordedVideo() {
  const video = document.createElement('video');
  let time = 0;
  Object.defineProperty(video, 'duration', { value: Number.POSITIVE_INFINITY });
  Object.defineProperty(video, 'currentTime', {
    get: () => time,
    set: (value: number) => {
      time = value;
    },
  });
  const play = vi.fn(() => Promise.resolve());
  const pause = vi.fn();
  video.play = play;
  video.pause = pause;
  return { video, play, pause };
}

describe('useRealVideo', () => {
  it('measures progress against the content duration when the file reports none and completes once on ended', () => {
    const { video, play } = recordedVideo();
    const onComplete = vi.fn();
    const videoRef = { current: video };
    const { result } = renderHook(() =>
      useRealVideo({
        videoRef,
        durationSec: 40,
        active: true,
        muted: false,
        onMutedByBrowser: vi.fn(),
        onComplete,
      }),
    );
    expect(play).toHaveBeenCalled();

    act(() => {
      video.currentTime = 10;
      video.dispatchEvent(new Event('timeupdate'));
    });
    expect(result.current.progress).toBe(0.25);
    expect(result.current.elapsedSec).toBe(10);

    act(() => {
      video.dispatchEvent(new Event('ended'));
      video.dispatchEvent(new Event('ended'));
    });
    expect(result.current.progress).toBe(1);
    expect(result.current.playing).toBe(false);
    expect(onComplete).toHaveBeenCalledOnce();
  });

  it('follows the sound choice and reports when the browser refuses autoplay with sound', async () => {
    const { video, play } = recordedVideo();
    play.mockRejectedValueOnce(new Error('NotAllowedError'));
    const videoRef = { current: video };
    const onMutedByBrowser = vi.fn();
    const { rerender } = renderHook(
      ({ muted }) =>
        useRealVideo({
          videoRef,
          durationSec: 40,
          active: true,
          muted,
          onMutedByBrowser,
          onComplete: vi.fn(),
        }),
      { initialProps: { muted: false } },
    );

    await vi.waitFor(() => {
      expect(onMutedByBrowser).toHaveBeenCalledOnce();
    });
    expect(video.muted).toBe(true);
    expect(play).toHaveBeenCalledTimes(2);

    // the page records the browser's choice, then the learner turns the sound on
    rerender({ muted: true });
    rerender({ muted: false });
    expect(video.muted).toBe(false);
  });

  it('pauses while inactive or toggled off', () => {
    const { video, pause } = recordedVideo();
    const videoRef = { current: video };
    const onComplete = vi.fn();
    const { result, rerender } = renderHook(
      ({ active }) =>
        useRealVideo({
          videoRef,
          durationSec: 40,
          active,
          muted: false,
          onMutedByBrowser: vi.fn(),
          onComplete,
        }),
      { initialProps: { active: false } },
    );
    expect(pause).toHaveBeenCalled();

    rerender({ active: true });
    expect(result.current.playing).toBe(true);
    act(() => {
      result.current.toggle();
    });
    expect(result.current.playing).toBe(false);
  });
});
