import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { writeSession } from '@/shared/lib/session';
import { renderApp } from '@/test/renderApp';
import { meOf } from '@/test/server';

function loggedInAs(slug: string) {
  writeSession({ token: `token-${slug}`, user: meOf(slug) });
}

describe('demo login and app shell', () => {
  it('offers the students and opens Ana Sayfa with the menu after picking one', async () => {
    const router = renderApp('/');

    await userEvent.click(await screen.findByRole('button', { name: /Deniz/ }));

    expect(await screen.findByRole('navigation', { name: 'Ana menü' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/app');
    expect(screen.getByRole('heading', { name: 'Deniz' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ana Sayfa' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Testler ve Denemeler' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Eksiklerim' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Dersler' })).not.toBeInTheDocument();
    expect(sessionStorage.getItem('prescription.session')).toContain('token-deniz');
  });

  it('opens Dopingle from the lightning button and returns to the same page on close', async () => {
    loggedInAs('deniz');
    const router = renderApp('/app/denemeler');

    await userEvent.click(await screen.findByRole('link', { name: 'Dopingle' }));
    expect(router.state.location.pathname).toBe('/app/dopingle');
    expect(screen.getByText('Kaydedilen')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: "Dopingle'ı kapat" }));
    expect(router.state.location.pathname).toBe('/app/denemeler');

    await userEvent.click(screen.getByRole('link', { name: 'Dopingle' }));
    await userEvent.keyboard('{Escape}');
    expect(router.state.location.pathname).toBe('/app/denemeler');
  });

  it('lists only students as cards', async () => {
    renderApp('/');

    expect(await screen.findByRole('button', { name: /Deniz/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Mert/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Selin/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Admin Öğrenci/ })).not.toBeInTheDocument();
  });

  it('keeps the session after a reload', async () => {
    loggedInAs('deniz');

    renderApp('/app/eksiklerim');

    expect(await screen.findByRole('heading', { name: 'Eksiklerim' })).toBeInTheDocument();
    expect(screen.getByText('Deniz')).toBeInTheDocument();
  });

  it('sends a student away from the admin area and a guest away from the app', () => {
    loggedInAs('deniz');
    const studentRouter = renderApp('/admin');
    expect(studentRouter.state.location.pathname).not.toBe('/admin');

    sessionStorage.clear();
    const guestRouter = renderApp('/app/denemeler');
    expect(guestRouter.state.location.pathname).toBe('/');
  });

  it('logs out back to the login screen', async () => {
    loggedInAs('deniz');
    const router = renderApp('/app/denemeler');

    await userEvent.click(await screen.findByRole('button', { name: 'Çıkış' }));

    expect(router.state.location.pathname).toBe('/');
    expect(sessionStorage.length).toBe(0);
  });
});
