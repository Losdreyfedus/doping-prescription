import { useMutation, useQuery } from '@tanstack/react-query';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

export type DemoLogin = Schemas['DemoLoginResponse'];

export function useDemoUsers() {
  return useQuery({
    queryKey: queryKeys.demoUsers(),
    queryFn: async () => unwrap(await api.GET('/api/auth/demo-users')),
  });
}

export function useDemoLogin(onLoggedIn: (login: DemoLogin) => void) {
  return useMutation({
    mutationFn: async (slug: string) => unwrap(await api.POST('/api/auth/demo-login', { body: { slug } })),
    onSuccess: onLoggedIn,
  });
}
