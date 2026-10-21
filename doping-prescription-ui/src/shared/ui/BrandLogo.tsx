import { tr } from '@/shared/i18n/tr';

/** Company logo (public/brand/logo.svg) with the demo label under it, as described in docs/arayuz.md. */
export function BrandLogo() {
  return (
    <div className="flex flex-col gap-1">
      <img src="/brand/logo.svg" alt={tr.app.logoAlt} width={224} height={52} />
      <span className="text-xs text-band-unknown">{tr.app.subtitle}</span>
    </div>
  );
}
