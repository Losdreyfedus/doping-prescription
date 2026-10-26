import { tr } from '@/shared/i18n/tr';
import { formatNet } from '@/shared/lib/format';

import type { ExamResult } from './api';

/** The four cards under "Başarı Durumun": success ring, correct, wrong, blank (docs/arayuz.md). */
export function ResultSummary({ result }: { result: ExamResult }) {
  const total = result.correct + result.wrong + result.blank;
  const successPercent = total === 0 ? 0 : Math.round((result.correct / total) * 100);
  return (
    <div className="flex flex-wrap justify-center gap-4">
      <div className="flex w-44 flex-col items-center gap-2 rounded-card bg-surface p-5">
        <p className="font-semibold">{tr.result.successTitle}</p>
        <SuccessRing percent={successPercent} />
        <p className="text-sm text-text-muted">{tr.result.net(formatNet(result.net))}</p>
      </div>
      <StatCard label={tr.result.correct} value={result.correct} className="bg-success text-surface" />
      <StatCard label={tr.result.wrong} value={result.wrong} className="bg-danger text-surface" />
      <StatCard label={tr.result.blank} value={result.blank} className="bg-surface" />
    </div>
  );
}

function StatCard({ label, value, className }: { label: string; value: number; className: string }) {
  return (
    <div
      role="group"
      aria-label={label}
      className={`${className} flex w-44 flex-col items-center justify-center gap-1 rounded-card p-5`}
    >
      <p className="font-semibold">{label}</p>
      <p className="text-5xl font-bold">{value}</p>
      <p>{tr.result.questions}</p>
    </div>
  );
}

const RING_RADIUS = 34;
const RING_LENGTH = 2 * Math.PI * RING_RADIUS;

function SuccessRing({ percent }: { percent: number }) {
  return (
    <svg viewBox="0 0 80 80" className="size-24" role="img" aria-label={`%${String(percent)}`}>
      <circle cx="40" cy="40" r={RING_RADIUS} fill="none" strokeWidth="8" className="stroke-danger" />
      <circle
        cx="40"
        cy="40"
        r={RING_RADIUS}
        fill="none"
        strokeWidth="8"
        className="stroke-success"
        strokeDasharray={`${String((RING_LENGTH * percent) / 100)} ${String(RING_LENGTH)}`}
        transform="rotate(-90 40 40)"
      />
      <text x="40" y="45" textAnchor="middle" className="fill-ink text-base font-bold">
        %{percent}
      </text>
    </svg>
  );
}
