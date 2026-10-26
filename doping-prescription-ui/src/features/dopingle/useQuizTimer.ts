import { useEffect, useState } from 'react';

const SECOND_MS = 1000;

/**
 * The micro-question's countdown (docs/arayuz.md, Mikro-soru penceresi). It ticks once a second while `running` and
 * stops for good once it reaches zero; the panel stops it as soon as an option is chosen, so "Süre doldu" can never
 * appear next to a result or an error.
 */
export function useQuizTimer(limitSec: number, running: boolean) {
  const [secondsLeft, setSecondsLeft] = useState(limitSec);

  useEffect(() => {
    if (!running || secondsLeft === 0) {
      return;
    }
    const timer = setTimeout(() => {
      setSecondsLeft((seconds) => seconds - 1);
    }, SECOND_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [running, secondsLeft]);

  return { secondsLeft, timedOut: secondsLeft === 0 };
}
