import type { Response } from '@playwright/test';

/** The parts of a feed page (GET /api/feed, doping-prescription-ui/openapi.json FeedPage) the scenario checks. */
export interface FeedPage {
  cards: { reasonText: string; topic: { name: string } }[];
}

/** The part of a micro-question (GET /api/feed/cards/{impressionId}/quiz) the scenario needs. */
export interface MicroQuiz {
  microQuestionId: number;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isCard(value: unknown): value is FeedPage['cards'][number] {
  return (
    isRecord(value) &&
    typeof value.reasonText === 'string' &&
    isRecord(value.topic) &&
    typeof value.topic.name === 'string'
  );
}

function isFeedPage(value: unknown): value is FeedPage {
  return isRecord(value) && Array.isArray(value.cards) && value.cards.every(isCard);
}

function isMicroQuiz(value: unknown): value is MicroQuiz {
  return isRecord(value) && typeof value.microQuestionId === 'number';
}

/** True for the request that opens a new feed session: the first page of every Dopingle opening. */
export function isFirstFeedPage(response: Response): boolean {
  const url = new URL(response.url());
  return (
    response.request().method() === 'GET' &&
    url.pathname === '/api/feed' &&
    url.searchParams.get('refresh') === 'true'
  );
}

export async function readFeedPage(response: Response): Promise<FeedPage> {
  const body: unknown = await response.json();
  if (!isFeedPage(body)) {
    throw new Error(`unexpected feed page from ${response.url()}`);
  }
  return body;
}

export async function readMicroQuiz(response: Response): Promise<MicroQuiz> {
  const body: unknown = await response.json();
  if (!isMicroQuiz(body)) {
    throw new Error(`unexpected micro-question from ${response.url()}`);
  }
  return body;
}
