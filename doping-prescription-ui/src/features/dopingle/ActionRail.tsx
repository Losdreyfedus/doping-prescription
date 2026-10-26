import { useState } from 'react';

import { tr } from '@/shared/i18n/tr';
import { BookmarkIcon, HeartIcon, SpeakerIcon } from '@/shared/ui/icons';

interface Props {
  onLike: () => void;
  /** Whether the learner saved the video; the bookmark toggles it on the server. */
  saved: boolean;
  onToggleSave: () => void;
  saving: boolean;
  muted: boolean;
  /** False for the simulated player, which has no sound. */
  soundAvailable: boolean;
  onToggleSound: () => void;
}

const ACTION = 'flex size-11 items-center justify-center rounded-full bg-surface/15 hover:bg-surface/25';
const FOCUS = 'focus-visible:outline-2 focus-visible:outline-select';

/**
 * The card's side actions as in Doping's short videos: like (recorded once), save (kept on the server, listed under
 * Kaydedilen) and sound. The demo has no like counts, so only the toggled heart is shown.
 */
export function ActionRail({
  onLike,
  saved,
  onToggleSave,
  saving,
  muted,
  soundAvailable,
  onToggleSound,
}: Props) {
  const [liked, setLiked] = useState(false);
  const soundLabel = !soundAvailable ? tr.dopingle.noSound : muted ? tr.dopingle.unmute : tr.dopingle.mute;
  return (
    <div className="mt-40 flex flex-col items-center gap-4">
      <button
        type="button"
        aria-pressed={liked}
        aria-label={tr.dopingle.like}
        onClick={() => {
          if (!liked) {
            onLike();
          }
          setLiked(true);
        }}
        className={`${ACTION} ${FOCUS} ${liked ? 'text-danger' : ''}`}
      >
        <HeartIcon filled={liked} />
      </button>
      <button
        type="button"
        aria-pressed={saved}
        aria-label={saved ? tr.dopingle.unsave : tr.dopingle.save}
        title={saved ? tr.dopingle.unsave : tr.dopingle.save}
        disabled={saving}
        onClick={onToggleSave}
        className={`${ACTION} ${FOCUS} disabled:opacity-60 ${saved ? 'text-select' : ''}`}
      >
        <BookmarkIcon filled={saved} />
      </button>
      <button
        type="button"
        aria-label={soundLabel}
        title={soundLabel}
        disabled={!soundAvailable}
        onClick={onToggleSound}
        className={`${ACTION} ${FOCUS} disabled:cursor-not-allowed disabled:opacity-40`}
      >
        <SpeakerIcon muted={muted || !soundAvailable} />
      </button>
    </div>
  );
}
