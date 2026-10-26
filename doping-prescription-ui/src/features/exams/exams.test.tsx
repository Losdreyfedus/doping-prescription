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
    http.get(`${API}/api/learner/profile`, () => HttpResponse.json(profileAfter(ATTEMPT_ID))),
  );
}

const weakTopic = (
  topicId: number,
  name: string,
  readyContentCount: number,
): Schemas['TopicProfileView'] => ({
  topicId,
  code: `TYT.MAT.${String(topicId)}`,
  name,
  mastery: 0.2,
  confidence: 0.45,
  band: 'WEAK',
  trend: 'DOWN',
  lastSignalAt: '2026-10-21T10:04:00Z',
  readyContentCount,
});

/** The profile once it reflects the given attempt (null: before the first exam). */
function profileAfter(attemptId: string | null): Schemas['ProfileView'] {
  return {
    userId: meOf('deniz').userId,
    personalizationEnabled: true,
    lastExamAttemptId: attemptId,
    updatedAt: null,
    topics: attemptId ? [weakTopic(9, 'Olasılık', 6), weakTopic(8, 'Permütasyon-Kombinasyon', 2)] : [],
  };
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

  it('polls the profile on the result screen until it reflects the attempt', async () => {
    examApi([]);
    let calls = 0;
    server.use(
      http.get(`${API}/api/exam-attempts/${ATTEMPT_ID}/result`, () => HttpResponse.json(result)),
      http.get(`${API}/api/learner/profile`, () => {
        calls += 1;
        return HttpResponse.json(profileAfter(calls < 2 ? null : ATTEMPT_ID));
      }),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp(`/app/denemeler/${ATTEMPT_ID}/sonuc`);

    expect(await screen.findByText('Eksiklerin analiz ediliyor…')).toBeInTheDocument();
    expect(
      await screen.findByText('Dopingle akışın bu sonuca göre güncellendi.', {}, { timeout: 2000 }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Olasılık ve Permütasyon-Kombinasyon için 8 video hazır\./)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Reçeteni gör' })).toHaveAttribute(
      'href',
      '/app/dopingle?refresh=1',
    );
    expect(calls).toBe(2);
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

  it('sends the submit only after a pending answer save has finished', async () => {
    const order: string[] = [];
    let finishSave: (() => void) | undefined;
    examApi([]);
    server.use(
      http.put(
        `${API}/api/exam-attempts/${ATTEMPT_ID}/answers/:questionId`,
        () =>
          new Promise<Response>((resolve) => {
            order.push('save started');
            finishSave = () => {
              order.push('save finished');
              resolve(new HttpResponse(null, { status: 204 }));
            };
          }),
      ),
      http.post(`${API}/api/exam-attempts/${ATTEMPT_ID}/submit`, () => {
        order.push('submit');
        return HttpResponse.json(result);
      }),
    );
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp(`/app/denemeler/${ATTEMPT_ID}`);

    await userEvent.click(await screen.findByRole('button', { name: /^B 24$/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Testi Bitir' }));
    const confirm = within(screen.getByRole('dialog')).getByRole('button', { name: 'Bitir' });
    await userEvent.click(confirm);

    await expect.poll(() => order).toEqual(['save started']);
    expect(confirm).toBeDisabled();
    finishSave?.();

    expect(await screen.findByText('Denemeyi Tamamladın!')).toBeInTheDocument();
    expect(order).toEqual(['save started', 'save finished', 'submit']);
  });

  it('keeps focus in the finish dialog, closes it with Escape and returns focus to the button', async () => {
    examApi([]);
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    renderApp(`/app/denemeler/${ATTEMPT_ID}`);

    const finish = await screen.findByRole('button', { name: 'Testi Bitir' });
    await userEvent.click(finish);
    const dialog = screen.getByRole('dialog');
    const cancel = within(dialog).getByRole('button', { name: 'Vazgeç' });
    const confirm = within(dialog).getByRole('button', { name: 'Bitir' });

    expect(cancel).toHaveFocus();
    await userEvent.tab();
    expect(confirm).toHaveFocus();
    await userEvent.tab();
    expect(cancel).toHaveFocus();
    await userEvent.tab({ shift: true });
    expect(confirm).toHaveFocus();

    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(finish).toHaveFocus();
  });

  it('opens the prescribed feed so that closing it returns to the result screen', async () => {
    examApi([]);
    server.use(http.get(`${API}/api/exam-attempts/${ATTEMPT_ID}/result`, () => HttpResponse.json(result)));
    writeSession({ token: 'token-deniz', user: meOf('deniz') });
    const router = renderApp(`/app/denemeler/${ATTEMPT_ID}/sonuc`);

    await userEvent.click(await screen.findByRole('link', { name: 'Reçeteni gör' }, { timeout: 2000 }));
    expect(router.state.location.pathname).toBe('/app/dopingle');
    await userEvent.click(screen.getByRole('button', { name: "Dopingle'ı kapat" }));

    expect(router.state.location.pathname).toBe(`/app/denemeler/${ATTEMPT_ID}/sonuc`);
  });
});
