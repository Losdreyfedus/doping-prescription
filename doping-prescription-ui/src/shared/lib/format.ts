const TURKISH = 'tr-TR';

/** 7.25 -> "7,25" (Turkish decimal comma). */
export function formatNet(net: number): string {
  return net.toLocaleString(TURKISH, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** ISO timestamp -> "21.10.2026". */
export function formatDate(isoTimestamp: string): string {
  return new Date(isoTimestamp).toLocaleDateString(TURKISH);
}
