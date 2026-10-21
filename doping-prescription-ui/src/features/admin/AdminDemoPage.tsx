import { TopBar } from '@/app/layout/TopBar';
import { tr } from '@/shared/i18n/tr';
import { PageHeader } from '@/shared/ui/PageHeader';

/** Placeholder until the minimal admin (phase 7). */
export function AdminDemoPage() {
  return (
    <div className="min-h-screen bg-surface-muted">
      <TopBar />
      <section className="p-8">
        <PageHeader title={tr.admin.demoTitle} />
        <p>{tr.pages.comingSoon}</p>
      </section>
    </div>
  );
}
