import { useCallback, useEffect, useRef, type WheelEvent } from 'react';

const WHEEL_COOLDOWN_MS = 600;
/** Wheel deltas up to this (touchpad inertia, a light nudge) do not change the video. */
const WHEEL_DELTA_THRESHOLD = 10;

interface Options {
  /** Moves the feed by one card: -1 up, 1 down. */
  onMove: (delta: number) => void;
  /** Runs before every move, while the current card is still on screen (skip detection). */
  onLeave: () => void;
  /** Plays or pauses the video. */
  toggle: () => void;
  /** The keyboard rests while the micro-question is open. */
  keyboardEnabled: boolean;
}

/** Dopingle's navigation (docs/arayuz.md): ↑/↓ and the wheel change the video, Space plays or pauses it. */
export function useFeedNavigation({ onMove, onLeave, toggle, keyboardEnabled }: Options) {
  const lastWheel = useRef(0);

  const move = useCallback(
    (delta: number) => {
      onLeave();
      onMove(delta);
    },
    [onLeave, onMove],
  );

  useEffect(() => {
    if (!keyboardEnabled) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        move(event.key === 'ArrowDown' ? 1 : -1);
      } else if (event.key === ' ') {
        event.preventDefault();
        toggle();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('keydown', onKey);
    };
  }, [move, toggle, keyboardEnabled]);

  const onWheel = useCallback(
    (event: WheelEvent) => {
      const now = Date.now();
      if (now - lastWheel.current > WHEEL_COOLDOWN_MS && Math.abs(event.deltaY) > WHEEL_DELTA_THRESHOLD) {
        lastWheel.current = now;
        move(event.deltaY > 0 ? 1 : -1);
      }
    },
    [move],
  );

  return { move, onWheel };
}
