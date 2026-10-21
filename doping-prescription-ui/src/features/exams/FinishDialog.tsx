import { tr } from '@/shared/i18n/tr';

/** Confirmation before submitting: "1 soru boş. Denemeyi bitirmek istiyor musun?" (docs/arayuz.md). */
export function FinishDialog({
  blank,
  busy,
  onConfirm,
  onCancel,
}: {
  blank: number;
  busy: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-ink/50">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="finish-title"
        className="w-[420px] rounded-card bg-surface p-6"
      >
        <h2 id="finish-title" className="mb-2 text-xl font-bold">
          {tr.solve.confirmTitle}
        </h2>
        <p className="mb-6">{tr.solve.confirmBlank(blank)}</p>
        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-button bg-surface-muted px-5 py-2 font-semibold"
          >
            {tr.solve.cancel}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onConfirm}
            className="rounded-button bg-success-strong px-5 py-2 font-semibold text-surface disabled:opacity-60"
          >
            {tr.solve.confirm}
          </button>
        </div>
      </div>
    </div>
  );
}
