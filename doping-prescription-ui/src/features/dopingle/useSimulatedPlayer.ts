import { useCallback, useEffect, useRef, useState } from 'react';

const TICK_MS = 100;

interface Options {
  /** Real length of the video. */
  durationSec: number;
  /** The simulated video fills in durationSec / speed seconds (prescription.feed.demo-video-speed). */
  speed: number;
  /** Only the card on screen plays. */
  active: boolean;
  onComplete: () => void;
}

/**
 * The simulated player of docs/arayuz.md: progress advances on a timer, faster than real time by `speed`, while the
 * card is active and not paused. `elapsedSec` is in real video seconds.
 */
export function useSimulatedPlayer({ durationSec, speed, active, onComplete }: Options) {
  const [progress, setProgress] = useState(0);
  const [paused, setPaused] = useState(false);
  const completed = useRef(false);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  const playing = active && !paused && progress < 1;

  useEffect(() => {
    if (!playing) {
      return;
    }
    const step = ((TICK_MS / 1000) * speed) / durationSec;
    const timer = setInterval(() => {
      setProgress((current) => Math.min(1, current + step));
    }, TICK_MS);
    return () => {
      clearInterval(timer);
    };
  }, [playing, speed, durationSec]);

  useEffect(() => {
    if (progress >= 1 && !completed.current) {
      completed.current = true;
      onCompleteRef.current();
    }
  }, [progress]);

  const toggle = useCallback(() => {
    setPaused((current) => !current);
  }, []);

  return { progress, playing, elapsedSec: progress * durationSec, toggle };
}
