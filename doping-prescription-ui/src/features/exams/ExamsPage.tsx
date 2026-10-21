import { tr } from '@/shared/i18n/tr';
import { PageHeader } from '@/shared/ui/PageHeader';

/** Placeholder until the exam module (phase 3). */
export function ExamsPage() {
  return (
    <section className="p-8">
      <PageHeader title={tr.pages.examsTitle} subtitle={tr.pages.examsSubtitle} />
      <p>{tr.pages.comingSoon}</p>
    </section>
  );
}
