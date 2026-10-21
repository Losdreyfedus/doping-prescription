/** Round badge with the first letter of the name (docs/arayuz.md: no 3D avatars). */
export function Avatar({ name, size = 'md' }: { name: string; size?: 'md' | 'lg' }) {
  const dimensions = size === 'lg' ? 'size-16 text-2xl' : 'size-9 text-base';
  return (
    <span
      aria-hidden="true"
      className={`${dimensions} inline-flex items-center justify-center rounded-full bg-brand-blue font-bold text-surface`}
    >
      {name.charAt(0).toLocaleUpperCase('tr')}
    </span>
  );
}
