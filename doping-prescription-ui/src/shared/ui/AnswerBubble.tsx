export type BubbleState = 'idle' | 'selected' | 'correct' | 'wrong';

const STATE_CLASSES: Record<BubbleState, string> = {
  idle: 'border-border bg-surface text-ink',
  selected: 'border-select bg-select text-surface',
  correct: 'border-success bg-success text-surface',
  wrong: 'border-danger bg-danger text-surface',
};

/**
 * The A-E answer bubble shared by the exam screen, the answer sheet and the micro-question panel (docs/arayuz.md).
 * Rendered as a span; the surrounding button carries the click and the accessible name.
 */
export function AnswerBubble({
  letter,
  state,
  size = 'md',
}: {
  letter: string;
  state: BubbleState;
  size?: 'sm' | 'md';
}) {
  const dimensions = size === 'sm' ? 'size-7 text-sm' : 'size-10 text-base';
  return (
    <span
      aria-hidden="true"
      className={`${dimensions} ${STATE_CLASSES[state]} inline-flex shrink-0 items-center justify-center rounded-full border-2 font-bold`}
    >
      {letter}
    </span>
  );
}
