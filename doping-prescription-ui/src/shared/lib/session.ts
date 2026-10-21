import { z } from 'zod';

/**
 * The demo session, kept in sessionStorage so a page reload keeps the user logged in, while the admin tab and the
 * student tab of the presentation stay independent.
 */
const sessionSchema = z.object({
  token: z.string().min(1),
  user: z.object({
    userId: z.string(),
    slug: z.string(),
    displayName: z.string(),
    role: z.enum(['STUDENT', 'ADMIN']),
  }),
});

export type Session = z.infer<typeof sessionSchema>;

const STORAGE_KEY = 'prescription.session';

export function readSession(): Session | null {
  const stored = sessionStorage.getItem(STORAGE_KEY);
  if (stored === null) {
    return null;
  }
  const parsed = sessionSchema.safeParse(safeJson(stored));
  return parsed.success ? parsed.data : null;
}

export function writeSession(session: Session): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
