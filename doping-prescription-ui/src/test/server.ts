import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

import type { Schemas } from '@/shared/api/client';

export const API = 'http://localhost:8080';

export const demoUsers: Schemas['DemoUserResponse'][] = [
  { slug: 'deniz', displayName: 'Deniz', role: 'STUDENT' },
  { slug: 'mert', displayName: 'Mert', role: 'STUDENT' },
  { slug: 'selin', displayName: 'Selin', role: 'STUDENT' },
  { slug: 'admin', displayName: 'Admin', role: 'ADMIN' },
];

export function meOf(slug: string) {
  const user = demoUsers.find((candidate) => candidate.slug === slug);
  if (!user) {
    throw new Error(`unknown demo user ${slug}`);
  }
  return {
    userId: `00000000-0000-4000-8000-00000000000${String(demoUsers.indexOf(user) + 1)}`,
    slug: user.slug,
    displayName: user.displayName,
    role: user.role,
    personalizationEnabled: true,
    examDate: null,
  };
}

/** Default API handlers; tests override them with server.use(...). */
export const server = setupServer(
  http.get(`${API}/api/auth/demo-users`, () => HttpResponse.json(demoUsers)),
  http.post(`${API}/api/auth/demo-login`, async ({ request }) => {
    const body = await request.json();
    const slug = typeof body === 'object' && body !== null && 'slug' in body ? String(body.slug) : '';
    return HttpResponse.json({ token: `token-${slug}`, user: meOf(slug) });
  }),
  http.get(`${API}/api/learner/profile`, () =>
    HttpResponse.json({ userId: meOf('deniz').userId, personalizationEnabled: true, topics: [] }),
  ),
);
