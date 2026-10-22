import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

interface SeedQuestion {
  id: number;
  options: string[];
  correctKey: string;
}

const seedFile = fileURLToPath(
  new URL('../../doping-prescription-core/src/main/resources/seed/micro-questions.json', import.meta.url),
);
const questions = (JSON.parse(readFileSync(seedFile, 'utf8')) as { items: SeedQuestion[] }).items;

/**
 * The text of a micro-question's correct option, from the seed (docs/seed-verisi.md). Options are shuffled and
 * relabelled for every learner, so the test finds the correct one by its text, the way a student would.
 */
export function correctOptionText(microQuestionId: number): string {
  const question = questions.find((candidate) => candidate.id === microQuestionId);
  if (!question) {
    throw new Error(`micro-question ${String(microQuestionId)} is not in the seed`);
  }
  const index = question.correctKey.charCodeAt(0) - 'A'.charCodeAt(0);
  const text = question.options[index];
  if (text === undefined) {
    throw new Error(`micro-question ${String(microQuestionId)} has no option ${question.correctKey}`);
  }
  return text;
}
