/** The single source of TanStack Query keys (docs/kod-standartlari.md §3). */
export const queryKeys = {
  demoUsers: (): readonly ['auth', 'demo-users'] => ['auth', 'demo-users'],
  me: (): readonly ['auth', 'me'] => ['auth', 'me'],
};
