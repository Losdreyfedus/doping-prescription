import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

export type StudentCard = Schemas['StudentCard'];

export function useStudents() {
  return useQuery({
    queryKey: queryKeys.adminStudents(),
    queryFn: async () => unwrap(await api.GET('/api/admin/demo/students')),
  });
}

export function usePersonas() {
  return useQuery({
    queryKey: queryKeys.adminPersonas(),
    queryFn: async () => unwrap(await api.GET('/api/admin/demo/personas')),
    staleTime: Infinity,
  });
}

export function useAnswerKey() {
  return useQuery({
    queryKey: queryKeys.adminAnswerKey(),
    queryFn: async () => unwrap(await api.GET('/api/admin/demo/answer-key')),
    staleTime: Infinity,
  });
}

async function noContent(result: Promise<{ error?: unknown; response: Response }>) {
  const { error, response } = await result;
  if (!response.ok) {
    unwrap({ error, response });
  }
}

/** Reset and persona both change a student's data; the cards are reloaded afterwards. */
export function useDemoActions() {
  const queryClient = useQueryClient();
  const refresh = () => queryClient.invalidateQueries({ queryKey: queryKeys.adminStudents() });
  const reset = useMutation({
    mutationFn: (slug: string) => noContent(api.POST('/api/admin/demo/reset', { body: { slug } })),
    onSuccess: refresh,
  });
  const persona = useMutation({
    mutationFn: ({ slug, name }: { slug: string; name: string }) =>
      noContent(api.POST('/api/admin/demo/persona', { body: { slug, persona: name } })),
    onSuccess: refresh,
  });
  return { reset, persona };
}

export function useContentEffectiveness() {
  return useQuery({
    queryKey: queryKeys.adminEffectiveness(),
    queryFn: async () => unwrap(await api.GET('/api/admin/content-effectiveness')),
  });
}
