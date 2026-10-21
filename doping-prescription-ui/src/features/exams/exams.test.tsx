import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import type { Schemas } from '@/shared/api/client';
import { writeSession } from '@/shared/lib/session';
import { renderApp } from '@/test/renderApp';
import { API, meOf, server } from '@/test/server';

const ATTEMPT_ID = '3a1f0000-0000-4000-8000-000000000001';

const options = (texts: string[]) => texts.map((text, index) => ({ key: 'ABCDE'.charAt(index), text }));

const attempt: Schemas['AttemptView'] = {
  attemptId: ATTEMPT_ID,
  examId: 1,
  title: 'TYT Matematik Mini Deneme 1',
  durationMin: 20,
  startedAt: '2026-10-21T10:00:00Z',
  questions: [],
  answers: [],
};
const first = {
  questionId: 101,
  orderNo: 1,
  stem: 'Birinci soru?',
  options: options(['23', '24', '25', '26', '27']),
};
const second = {
  questionId: 102,
  orderNo: 2,
  stem: 'İkinci soru?',
  options: options(['9', '10', '11', '12', '13']),
};
attempt.questions = [first, second];

const result: Schemas['ExamResult'] = {
  attemptId: ATTEMPT_ID,
  examId: 1,
  title: 'TYT Matematik Mini Deneme 1',
  correct: 1,
  wrong: 0,
  blank: 1,
  net: 1,
  byTopic: [
    { topicId: 1, name: 'Temel Kavramlar', total: 1, correct: 1, wrong: 0, blank: 0 },
    { topicId: 2, name: 'Sayı Basamakları', total: 1, correct: 0, wrong: 0, blank: 1 },
  ],
  questions: [
    {
      ...first,
      selectedKey: 'B',
      correctKey: 'B',
      result: 'CORRECT',
      explanation: '9 + 8 + 7',
    },
    {
      ...second,
      selectedKey: null,
      correctKey: 'D',
      result: 'BLANK',
      explanation: '132 / 11',
    },
  ],
  profileUpdate: { status: 'PENDING' },
};

function examApi(savedBodies: unknown[]) {
  server.use(
    http.get(`${API}/api/exams`, () =>
      HttpResponse.json([
        {
          examId: 1,
          title: 'TYT Matematik Mini Deneme 1',
          durationMin: 20,
          questionCount: 12,
          inProgressAttemptId: null,
          lastResult: null,
        },
      ]),
    ),
    http.post(`${API}/api/exams/1/attempts`, () => HttpResponse.json(attempt, { status: 201 })),
    http.get(`${API}/api/exam-attempts/${ATTEMPT_ID}`, () => HttpResponse.json(attempt)),
    http.put(`${API}/api/exam-attempts/${ATTEMPT_ID}/answers/:questionId`, async ({ request, params }) => {
      savedBodies.push({ questionId: params.questionId, body: await request.json() });
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(`${API}/api/exam-attempts/${ATTEMPT_ID}/submit`, () => HttpResponse.json(result)),
  );
}

describe('exam flow', () => {
  it('starts an exam, saves answers, confirms the blank count and shows the result', async () => {
    const saved: unknown[] = [];
    examApi(saved);
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    const router = renderApp('/app/denemeler');

    await userEvent.click(await screen.findByRole('button', { name: 'Başla' }));
    expect(await screen.findByText('Birinci soru?')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /^B 24$/ }));
    expect(screen.getByText('1 Soru Çözüldü')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Sonraki Soru' }));
    await userEvent.click(screen.getByRole('button', { name: 'Sınavı Bitir' }));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('1 soru boş. Denemeyi bitirmek istiyor musun?')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Bitir' }));

    expect(await screen.findByText('Denemeyi Tamamladın!')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/app/denemeler/${ATTEMPT_ID}/sonuc`);
    expect(saved).toEqual([{ questionId: '101', body: { selectedKey: 'B', timeSpentSec: null } }]);
    const weakRow = screen.getByText('Sayı Basamakları').closest('tr');
    expect(weakRow).toHaveAttribute('data-weak', 'true');
    expect(screen.getByText('Temel Kavramlar').closest('tr')).toHaveAttribute('data-weak', 'false');
  });

  it('clears an answer when the selected option is clicked again', async () => {
    const saved: unknown[] = [];
    examApi(saved);
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp('/app/denemeler');

    await userEvent.click(await screen.findByRole('button', { name: 'Başla' }));
    const optionB = await screen.findByRole('button', { name: /^B 24$/ });
    await userEvent.click(optionB);
    await userEvent.click(optionB);

    expect(optionB).toHaveAttribute('aria-pressed', 'false');
    expect(saved).toEqual([
      { questionId: '101', body: { selectedKey: 'B', timeSpentSec: null } },
      { questionId: '101', body: { selectedKey: null, timeSpentSec: null } },
    ]);
  });
});
