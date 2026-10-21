import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

export type MicroQuiz = Schemas['MicroQuiz'];
export type QuizResult = Schemas['QuizResult'];

const NO_CONTENT = 204;

/** The card's micro-question, or null if it has none left for this learner (docs/api.md: 204). */
export async function openQuiz(impressionId: string): Promise<MicroQuiz | null> {
  const result = await api.GET('/api/feed/cards/{impressionId}/quiz', {
    params: { path: { impressionId } },
  });
  if (result.response.status === NO_CONTENT) {
    return null;
  }
  return unwrap(result);
}

export function useAnswerQuiz() {
  const queryClient = useQueryClient();
  return useMutation({
    // shownAt: when the question appeared (Date.now()); the response time is measured when the answer is sent
    mutationFn: async ({
      quiz,
      selectedKey,
      shownAt,
    }: {
      quiz: MicroQuiz;
      selectedKey: string;
      shownAt: number;
    }) =>
      unwrap(
        await api.POST('/api/quiz/{microQuestionId}/answer', {
          params: { path: { microQuestionId: quiz.microQuestionId } },
          body: { quizToken: quiz.quizToken, selectedKey, responseMs: Date.now() - shownAt },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.points() });
      void queryClient.invalidateQueries({ queryKey: queryKeys.learnerProfile() });
    },
  });
}
