import { tr } from '@/shared/i18n/tr';

/** Placeholder until the personalized feed (phase 5). */
export function DopinglePage() {
  return (
    <section className="flex h-full items-center justify-center p-8 text-surface">
      <div className="text-center">
        <h1 className="font-display text-[42px]">{tr.pages.dopingleTitle}</h1>
        <p className="mt-2 text-band-unknown">{tr.pages.comingSoon}</p>
      </div>
    </section>
  );
}
