import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

export type LearnerProfile = Schemas['ProfileView'];
export type TopicProfile = Schemas['TopicProfileView'];

/** docs/api.md: poll every 500 ms, at most 10 times, until the profile reflects the submitted attempt. */
export const PROFILE_POLL_INTERVAL_MS = 500;
export const PROFILE_POLL_MAX_ATTEMPTS = 10;

async function fetchProfile() {
  return unwrap(await api.GET('/api/learner/profile'));
}

/** The profile for Gelişimim; refetched when the tab regains focus, e.g. after an admin reset in another tab. */
export function useLearnerProfile() {
  return useQuery({
    queryKey: queryKeys.learnerProfile(),
    queryFn: fetchProfile,
    refetchOnWindowFocus: true,
  });
}

/**
 * Polls the profile until its lastExamAttemptId is the given attempt. `updated` is false while waiting and after
 * giving up, so the caller can tell the two apart with `gaveUp`.
 */
export function useProfilePolling(attemptId: string) {
  const queryClient = useQueryClient();
  // The profile query is shared with other screens, so its lifetime fetch count is offset by what it had at mount.
  const [fetchesBefore] = useState(
    () => queryClient.getQueryState(queryKeys.learnerProfile())?.dataUpdateCount ?? 0,
  );
  const query = useQuery({
    queryKey: queryKeys.learnerProfile(),
    queryFn: fetchProfile,
    refetchInterval: (current) => {
      const reflected = current.state.data?.lastExamAttemptId === attemptId;
      const fetches = current.state.dataUpdateCount - fetchesBefore;
      return reflected || fetches >= PROFILE_POLL_MAX_ATTEMPTS ? false : PROFILE_POLL_INTERVAL_MS;
    },
    // An unchanged profile still counts as an attempt, so every fetch re-renders (dataUpdatedAt changes).
    notifyOnChangeProps: ['data', 'dataUpdatedAt', 'isError'],
    // Only the interval fetches: a focus refetch would use up one of the attempts.
    refetchOnWindowFocus: false,
  });
  const updated = query.data?.lastExamAttemptId === attemptId;
  const fetches =
    (queryClient.getQueryState(queryKeys.learnerProfile())?.dataUpdateCount ?? 0) - fetchesBefore;
  const gaveUp = !updated && (query.isError || fetches >= PROFILE_POLL_MAX_ATTEMPTS);
  return { profile: query.data, updated, gaveUp };
}
