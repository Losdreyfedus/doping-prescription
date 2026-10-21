import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { API, server } from '@/test/server';

import { MicroQuizPanel } from './MicroQuizPanel';
import type { MicroQuiz, QuizResult } from './quiz';

const quiz: MicroQuiz = {
  microQuestionId: 37,
  impressionId: '00000000-0000-4000-8000-000000000011',
  stem: 'Bir zar atılıyor. Gelen sayının çift olduğu bilindiğine göre 4 olma olasılığı kaçtır?',
  options: [
    { key: 'A', text: '1/6' },
    { key: 'B', text: '1/3' },
    { key: 'C', text: '1/2' },
    { key: 'D', text: '2/3' },
  ],
  timeLimitSec: 60,
  pointsAvailable: 15,
  quizToken: 'token',
};

const correct: QuizResult = {
  isCorrect: true,
  correctKey: 'B',
  explanation: 'Çift sayılar 2, 4, 6; bunlardan biri 4 olduğu için 1/3.',
  pointsAwarded: 15,
  totalPoints: 15,
  streak: { current: 1, bonusAt: 5 },
  masteryChange: { topicId: 9, name: 'Olasılık', before: 0.172, after: 0.271 },
};

function renderPanel(onContinue = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <MicroQuizPanel quiz={quiz} topicName="Olasılık" onContinue={onContinue} />
    </QueryClientProvider>,
  );
  return onContinue;
}

describe('MicroQuizPanel', () => {
  it('sends the clicked option and shows points, solution and the mastery change', async () => {
    const bodies: unknown[] = [];
    server.use(
      http.post(`${API}/api/quiz/37/answer`, async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json(correct);
      }),
    );
    const onContinue = renderPanel();

    await userEvent.click(screen.getByRole('button', { name: 'B 1/3' }));

    expect(await screen.findByText('Olasılık ustalığın %17 → %27')).toBeInTheDocument();
    expect(screen.getByText('+15 puan')).toBeInTheDocument();
    expect(screen.getByText(correct.explanation)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'A 1/6' })).toBeDisabled();
    expect(bodies).toEqual([expect.objectContaining({ quizToken: 'token', selectedKey: 'B' })]);

    await userEvent.click(screen.getByRole('button', { name: 'Devam' }));
    expect(onContinue).toHaveBeenCalledOnce();
  });

  it('marks a wrong choice and the correct option', async () => {
    server.use(
      http.post(`${API}/api/quiz/37/answer`, () =>
        HttpResponse.json({
          ...correct,
          isCorrect: false,
          pointsAwarded: 0,
          masteryChange: { ...correct.masteryChange, after: 0.12 },
        }),
      ),
    );
    renderPanel();

    await userEvent.click(screen.getByRole('button', { name: 'D 2/3' }));

    expect(await screen.findByText('Yanlış.')).toBeInTheDocument();
    expect(screen.queryByText(/^\+\d+ puan$/)).not.toBeInTheDocument();
  });
});
