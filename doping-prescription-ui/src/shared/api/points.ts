import { useQuery } from '@tanstack/react-query';

import { api, unwrap } from './client';
import { queryKeys } from './queryKeys';

/**
 * The learner's points for the top bar; refreshed after every micro-question answer and when the tab regains focus,
 * so a reset from the admin panel in another tab shows up (focus refetch is off by default, see App.tsx).
 */
export function usePoints() {
  return useQuery({
    queryKey: queryKeys.points(),
    queryFn: async () => unwrap(await api.GET('/api/points')),
    refetchOnWindowFocus: true,
  });
}
