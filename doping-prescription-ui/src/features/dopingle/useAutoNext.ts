import { useCallback, useEffect, useState } from 'react';

const AUTO_NEXT_DELAY_MS = 800;

interface Options {
  /** The video has ended and its micro-question, if any, is settled. */
  finished: boolean;
  move: (delta: number) => void;
}

/** The "Otomatik sonraki video" switch: while it is on, the next card follows shortly after the current one is finished. */
export function useAutoNext({ finished, move }: Options) {
  const [autoNext, setAutoNext] = useState(true);

  useEffect(() => {
    if (!finished || !autoNext) {
      return;
    }
    const timer = setTimeout(() => {
      move(1);
    }, AUTO_NEXT_DELAY_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [finished, autoNext, move]);

  const toggleAutoNext = useCallback(() => {
    setAutoNext((value) => !value);
  }, []);

  return { autoNext, toggleAutoNext };
}
