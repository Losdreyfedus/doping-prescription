import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { API, server } from '@/test/server';

import type { MicroQuiz } from './quiz';
import { useQuizFlow } from './useQuizFlow';

const IMPRESSION_ID = '00000000-0000-4000-8000-000000000011';

const quiz: MicroQuiz = {
  microQuestionId: 37,
  impressionId: IMPRESSION_ID,
  stem: 'Bir zar atılıyor. Gelen sayının çift olduğu bilindiğine göre 4 olma olasılığı kaçtır?',
  options: [
    { key: 'A', text: '1/6' },
    { key: 'B', text: '1/3' },
  ],
  timeLimitSec: 60,
  pointsAvailable: 15,
  quizToken: 'token',
};

function renderQuizFlow() {
  const record = vi.fn();
  const flush = vi.fn(() => Promise.resolve());
  const hook = renderHook(() =>
    useQuizFlow({ impressionId: IMPRESSION_ID, record, flush, elapsedRef: { current: 42 } }),
  );
  return { ...hook, record, flush };
}

describe('useQuizFlow', () => {
  it('opens the question after sending the events, then does not reopen it once dismissed', async () => {
    let requests = 0;
    server.use(
      http.get(`${API}/api/feed/cards/:impressionId/quiz`, () => {
        requests += 1;
        return HttpResponse.json(quiz);
      }),
    );
    const { result, record, flush } = renderQuizFlow();

    await act(() => result.current.showQuiz());
    expect(result.current.quizState).toEqual({ status: 'open', quiz });
    expect(result.current.quizActive).toBe(true);
    expect(record).toHaveBeenCalledWith(IMPRESSION_ID, 'QUIZ_SHOWN', 42_000);
    expect(flush).toHaveBeenCalledOnce();

    act(() => {
      result.current.dismiss();
    });
    // the video ends after the student solved the question from the "Soruyu çöz" button
    await act(() => result.current.showQuiz());

    expect(result.current.quizState).toEqual({ status: 'done' });
    expect(result.current.quizSettled).toBe(true);
    expect(requests).toBe(1);
    expect(record).toHaveBeenCalledTimes(2);
  });

  it('settles with the error message and is not requested again', async () => {
    let requests = 0;
    server.use(
      http.get(`${API}/api/feed/cards/:impressionId/quiz`, () => {
        requests += 1;
        return HttpResponse.json({ detail: 'Videonun yeterli kısmını izlemedin.' }, { status: 409 });
      }),
    );
    const { result } = renderQuizFlow();

    await act(() => result.current.showQuiz());
    await act(() => result.current.showQuiz());

    await waitFor(() => {
      expect(result.current.quizState.status).toBe('error');
    });
    expect(result.current.quizSettled).toBe(true);
    expect(requests).toBe(1);
  });
});
