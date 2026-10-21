/** The single source of TanStack Query keys (docs/kod-standartlari.md §3). */
export const queryKeys = {
  demoUsers: (): readonly ['auth', 'demo-users'] => ['auth', 'demo-users'],
  me: (): readonly ['auth', 'me'] => ['auth', 'me'],
  exams: (): readonly ['exams'] => ['exams'],
  attempt: (attemptId: string): readonly ['exam-attempt', string] => ['exam-attempt', attemptId],
  result: (attemptId: string): readonly ['exam-result', string] => ['exam-result', attemptId],
  learnerProfile: (): readonly ['learner', 'profile'] => ['learner', 'profile'],
  points: (): readonly ['points'] => ['points'],
  feed: (topicId: number | undefined, sessionKey: string): readonly ['feed', number | undefined, string] => [
    'feed',
    topicId,
    sessionKey,
  ],
};
