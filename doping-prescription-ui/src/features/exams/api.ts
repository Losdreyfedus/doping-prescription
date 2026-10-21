import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, type Schemas, unwrap } from '@/shared/api/client';
import { queryKeys } from '@/shared/api/queryKeys';

export type ExamSummary = Schemas['ExamSummary'];
export type AttemptView = Schemas['AttemptView'];
export type ExamResult = Schemas['ExamResult'];

export function useExams() {
  return useQuery({
    queryKey: queryKeys.exams(),
    queryFn: async () => unwrap(await api.GET('/api/exams')),
  });
}

/** Starts the exam or resumes the attempt in progress. */
export function useStartAttempt(onStarted: (attempt: AttemptView) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (examId: number) =>
      unwrap(await api.POST('/api/exams/{examId}/attempts', { params: { path: { examId } } })),
    onSuccess: (attempt) => {
      queryClient.setQueryData(queryKeys.attempt(attempt.attemptId), attempt);
      void queryClient.invalidateQueries({ queryKey: queryKeys.exams() });
      onStarted(attempt);
    },
  });
}

export function useAttempt(attemptId: string) {
  return useQuery({
    queryKey: queryKeys.attempt(attemptId),
    queryFn: async () =>
      unwrap(await api.GET('/api/exam-attempts/{attemptId}', { params: { path: { attemptId } } })),
  });
}

/** Saves (or clears with null) one answer; the solve screen keeps its own optimistic copy. */
export function useSaveAnswer(attemptId: string) {
  return useMutation({
    mutationFn: async ({ questionId, selectedKey }: { questionId: number; selectedKey: string | null }) => {
      const { response, error } = await api.PUT('/api/exam-attempts/{attemptId}/answers/{questionId}', {
        params: { path: { attemptId, questionId } },
        body: { selectedKey, timeSpentSec: null },
      });
      if (!response.ok) {
        unwrap({ error, response });
      }
    },
  });
}

export function useSubmitAttempt(attemptId: string, onSubmitted: (result: ExamResult) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.POST('/api/exam-attempts/{attemptId}/submit', { params: { path: { attemptId } } })),
    onSuccess: (result) => {
      queryClient.setQueryData(queryKeys.result(attemptId), result);
      queryClient.removeQueries({ queryKey: queryKeys.attempt(attemptId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.exams() });
      onSubmitted(result);
    },
  });
}

export function useExamResult(attemptId: string) {
  return useQuery({
    queryKey: queryKeys.result(attemptId),
    queryFn: async () =>
      unwrap(await api.GET('/api/exam-attempts/{attemptId}/result', { params: { path: { attemptId } } })),
  });
}
