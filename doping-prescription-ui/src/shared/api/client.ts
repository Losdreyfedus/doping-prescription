import createClient, { type Middleware } from 'openapi-fetch';

import { tr } from '@/shared/i18n/tr';
import { clearSession, readSession } from '@/shared/lib/session';

import type { components, paths } from './generated/schema';

export type Schemas = components['schemas'];

/** A problem detail returned by the API (docs/api.md). */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;

  constructor(status: number, detail: string, code?: string) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

const UNAUTHORIZED = 401;

const authMiddleware: Middleware = {
  onRequest({ request }) {
    const session = readSession();
    if (session) {
      request.headers.set('Authorization', `Bearer ${session.token}`);
    }
    return request;
  },
  onResponse({ response }) {
    if (response.status === UNAUTHORIZED && readSession()) {
      // The token expired or was rejected: start over at the login screen.
      clearSession();
      window.location.assign('/');
    }
    return response;
  },
};

export const api = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  // Look up fetch per call (not once at import), so test interceptors installed later are honoured.
  fetch: (request) => globalThis.fetch(request),
});
api.use(authMiddleware);

/** Returns the response data or throws an {@link ApiError} with the problem detail's message. */
export function unwrap<T>(result: { data?: T; error?: unknown; response: Response }): T {
  if (result.data !== undefined) {
    return result.data;
  }
  throw toApiError(result.response.status, result.error);
}

function toApiError(status: number, error: unknown): ApiError {
  if (typeof error === 'object' && error !== null) {
    const detail = 'detail' in error && typeof error.detail === 'string' ? error.detail : undefined;
    const code = 'code' in error && typeof error.code === 'string' ? error.code : undefined;
    return new ApiError(status, detail ?? tr.errors.unexpected, code);
  }
  return new ApiError(status, tr.errors.unexpected);
}
