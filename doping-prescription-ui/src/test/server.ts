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

export function meOf(slug: string): Schemas['MeResponse'] {
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

// The default responses are typed with the generated schema, so a changed API contract breaks the mocks at compile time.
const emptyFeed: Schemas['FeedPage'] = {
  sessionId: '00000000-0000-4000-8000-000000000099',
  personalized: false,
  algoVersion: 'v1',
  nextCursor: 'end',
  demoVideoSpeed: 4,
  cards: [],
};

const noPoints: Schemas['Summary'] = {
  total: 0,
  today: 0,
  dailyCap: 200,
  streak: { current: 0, best: 0 },
  recent: [],
};

const emptyProfile = (): Schemas['ProfileView'] => ({
  userId: meOf('deniz').userId,
  personalizationEnabled: true,
  lastExamAttemptId: null,
  updatedAt: null,
  topics: [],
});

/** Default API handlers; tests override them with server.use(...). */
export const server = setupServer(
  http.get(`${API}/api/auth/demo-users`, () => HttpResponse.json(demoUsers)),
  http.post(`${API}/api/auth/demo-login`, async ({ request }) => {
    const body = await request.json();
    const slug = typeof body === 'object' && body !== null && 'slug' in body ? String(body.slug) : '';
    const login: Schemas['DemoLoginResponse'] = { token: `token-${slug}`, user: meOf(slug) };
    return HttpResponse.json(login);
  }),
  http.get(`${API}/api/feed`, () => HttpResponse.json(emptyFeed)),
  http.get(`${API}/api/points`, () => HttpResponse.json(noPoints)),
  http.post(`${API}/api/feed/interactions`, () => new HttpResponse(null, { status: 202 })),
  http.get(`${API}/api/learner/profile`, () => HttpResponse.json(emptyProfile())),
);
