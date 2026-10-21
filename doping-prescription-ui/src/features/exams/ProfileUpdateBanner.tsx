import { Link } from 'react-router';

import { useProfileUpdate } from '@/shared/api/learnerProfile';
import { tr } from '@/shared/i18n/tr';

const topicList = new Intl.ListFormat('tr', { style: 'long', type: 'conjunction' });

/**
 * The update band on the result screen (docs/arayuz.md): a spinner until the profile reflects this attempt, then the
 * prescribed topics and a link to the refreshed feed.
 */
export function ProfileUpdateBanner({ attemptId }: { attemptId: string }) {
  const { profile, updated, gaveUp } = useProfileUpdate(attemptId);

  if (!updated && !gaveUp) {
    return (
      <div role="status" className="flex items-center gap-3 rounded-card bg-navy px-6 py-4 text-surface">
        <span
          aria-hidden="true"
          className="size-4 animate-spin rounded-full border-2 border-surface border-t-transparent"
        />
        {tr.result.analyzing}
      </div>
    );
  }

  const weakTopics = updated ? (profile?.topics ?? []).filter((topic) => topic.band === 'WEAK') : [];
  const videoCount = weakTopics.reduce((sum, topic) => sum + topic.readyContentCount, 0);
  return (
    <div role="status" className="flex items-center gap-6 rounded-card bg-navy px-6 py-4 text-surface">
      <p className="flex-1">
        <strong>{updated ? tr.result.feedUpdated : tr.result.feedDelayed}</strong>
        {weakTopics.length > 0 && (
          <> {tr.result.videosReady(topicList.format(weakTopics.map((topic) => topic.name)), videoCount)}</>
        )}
      </p>
      <Link
        to="/app/dopingle?refresh=1"
        className="rounded-button bg-orange px-6 py-2 font-semibold whitespace-nowrap text-surface"
      >
        {tr.result.seePrescription}
      </Link>
    </div>
  );
}
