import { useQuery } from '@tanstack/react-query';

import { api, type Schemas, unwrap } from './client';
import { queryKeys } from './queryKeys';

export type PointsSummary = Schemas['Summary'];

/** The learner's points for the top bar; refreshed after every micro-question answer. */
export function usePoints() {
  return useQuery({
    queryKey: queryKeys.points(),
    queryFn: async () => unwrap(await api.GET('/api/points')),
  });
}
