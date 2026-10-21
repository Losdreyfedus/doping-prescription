import { tr } from '@/shared/i18n/tr';
import { PageHeader } from '@/shared/ui/PageHeader';

/** Placeholder until the learner profile (phase 4). */
export function WeakTopicsPage() {
  return (
    <section className="p-8">
      <PageHeader title={tr.pages.weakTopicsTitle} subtitle={tr.pages.weakTopicsSubtitle} />
      <p>{tr.pages.comingSoon}</p>
    </section>
  );
}
