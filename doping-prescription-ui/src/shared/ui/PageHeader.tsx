/** Large condensed page title with a grey subtitle, as on the Doping list pages (docs/arayuz.md). */
export function PageHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-8">
      <h1 className="font-display text-[2.625rem] leading-tight">{title}</h1>
      {subtitle && <p className="mt-1 text-text-muted">{subtitle}</p>}
    </div>
  );
}
