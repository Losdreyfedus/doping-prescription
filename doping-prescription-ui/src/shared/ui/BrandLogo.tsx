import { tr } from '@/shared/i18n/tr';

/** Company logo (public/brand/logo.svg), as on Doping Hafıza's own menu. */
export function BrandLogo() {
  return <img src="/brand/logo.svg" alt={tr.app.logoAlt} width={210} height={49} />;
}
