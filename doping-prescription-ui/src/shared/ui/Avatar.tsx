/** Round badge with the first letter of the name (docs/arayuz.md: no 3D avatars). */
const DIMENSIONS = { md: 'size-9 text-base', bar: 'size-12 text-lg', lg: 'size-16 text-2xl' };

export function Avatar({ name, size = 'md' }: { name: string; size?: keyof typeof DIMENSIONS }) {
  const dimensions = DIMENSIONS[size];
  return (
    <span
      aria-hidden="true"
      className={`${dimensions} inline-flex items-center justify-center rounded-full bg-brand-blue font-bold text-surface`}
    >
      {name.charAt(0).toLocaleUpperCase('tr')}
    </span>
  );
}
