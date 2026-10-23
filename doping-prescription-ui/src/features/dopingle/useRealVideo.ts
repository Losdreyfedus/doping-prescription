import { type RefObject, useCallback, useEffect, useRef, useState } from 'react';

interface Options {
  videoRef: RefObject<HTMLVideoElement | null>;
  /** The content's length; used when the file reports no finite duration (recorded WebM files report Infinity). */
  durationSec: number;
  /** Only the card on screen plays. */
  active: boolean;
  onComplete: () => void;
}

/**
 * Playback state of a real video file with the same shape as {@link useSimulatedPlayer}: progress from the element's
 * time, completion from its `ended` event. Real videos always play at normal speed.
 */
export function useRealVideo({ videoRef, durationSec, active, onComplete }: Options) {
  const [progress, setProgress] = useState(0);
  const [paused, setPaused] = useState(false);
  const completed = useRef(false);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) {
      return;
    }
    const length = () =>
      Number.isFinite(video.duration) && video.duration > 0 ? video.duration : durationSec;
    const onTime = () => {
      setProgress(Math.min(1, video.currentTime / length()));
    };
    const onEnded = () => {
      setProgress(1);
      if (!completed.current) {
        completed.current = true;
        onCompleteRef.current();
      }
    };
    video.addEventListener('timeupdate', onTime);
    video.addEventListener('ended', onEnded);
    return () => {
      video.removeEventListener('timeupdate', onTime);
      video.removeEventListener('ended', onEnded);
    };
  }, [videoRef, durationSec]);

  const shouldPlay = active && !paused && progress < 1;

  useEffect(() => {
    const video = videoRef.current;
    if (!video) {
      return;
    }
    if (shouldPlay) {
      // Autoplay with sound can be refused; the video then plays muted (the sound button is not functional in the demo).
      Promise.resolve(video.play()).catch(() => {
        video.muted = true;
        return Promise.resolve(video.play()).catch(() => undefined);
      });
    } else {
      video.pause();
    }
  }, [shouldPlay, videoRef]);

  const toggle = useCallback(() => {
    setPaused((current) => !current);
  }, []);

  return { progress, playing: shouldPlay, elapsedSec: progress * durationSec, toggle };
}
