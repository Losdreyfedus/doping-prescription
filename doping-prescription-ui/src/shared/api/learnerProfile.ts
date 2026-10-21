import { useQuery, useQueryClient } from '@tanstack/react-query';

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

export function useLearnerProfile() {
  return useQuery({ queryKey: queryKeys.learnerProfile(), queryFn: fetchProfile });
}

/**
 * Polls the profile until its lastExamAttemptId is the given attempt. `updated` is false while waiting and after
 * giving up, so the caller can tell the two apart with `gaveUp`.
 */
export function useProfileUpdate(attemptId: string) {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: queryKeys.learnerProfile(),
    queryFn: fetchProfile,
    refetchInterval: (current) => {
      const reflected = current.state.data?.lastExamAttemptId === attemptId;
      return reflected || current.state.dataUpdateCount >= PROFILE_POLL_MAX_ATTEMPTS
        ? false
        : PROFILE_POLL_INTERVAL_MS;
    },
  });
  const updated = query.data?.lastExamAttemptId === attemptId;
  const fetches = queryClient.getQueryState(queryKeys.learnerProfile())?.dataUpdateCount ?? 0;
  const gaveUp = !updated && (query.isError || fetches >= PROFILE_POLL_MAX_ATTEMPTS);
  return { profile: query.data, updated, gaveUp };
}
