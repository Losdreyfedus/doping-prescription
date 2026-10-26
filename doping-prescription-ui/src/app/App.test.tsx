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
    expect(screen.getByRole('link', { name: 'Gelişimim' })).toBeInTheDocument();
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

    renderApp('/app/denemeler');

    expect(await screen.findByRole('heading', { name: 'Testler ve Denemeler' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Deniz hesap menüsü' })).toBeInTheDocument();
  });

  it('sends a student away from the admin area and a guest away from the app', () => {
    loggedInAs('deniz');
    const studentRouter = renderApp('/admin');
    expect(studentRouter.state.location.pathname).not.toBe('/admin');

    sessionStorage.clear();
    const guestRouter = renderApp('/app/denemeler');
    expect(guestRouter.state.location.pathname).toBe('/');
  });

  it('opens Gelişimim in the dark panel layout without the top bar', async () => {
    loggedInAs('deniz');
    renderApp('/app/gelisimim');

    expect(await screen.findByRole('heading', { name: 'Gelişimim' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Deniz hesap menüsü' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: "Ana Sayfa'ya dön" })).toHaveAttribute('href', '/app');
  });

  it('logs out back to the login screen', async () => {
    loggedInAs('deniz');
    const router = renderApp('/app/denemeler');

    await userEvent.click(await screen.findByRole('button', { name: 'Deniz hesap menüsü' }));
    expect(screen.getByRole('menu')).toHaveTextContent('0 puan');
    await userEvent.click(screen.getByRole('menuitem', { name: 'Çıkış Yap' }));

    expect(router.state.location.pathname).toBe('/');
    expect(sessionStorage.length).toBe(0);
  });

  it('drives the account menu with the keyboard and gives focus back on Escape', async () => {
    loggedInAs('deniz');
    renderApp('/app/denemeler');

    const trigger = await screen.findByRole('button', { name: 'Deniz hesap menüsü' });
    await userEvent.click(trigger);
    const items = screen.getAllByRole('menuitem');
    expect(items.map((item) => item.textContent)).toEqual(['Profilim', 'Ayarlar', 'Çıkış Yap']);
    expect(items[0]).toHaveFocus();

    await userEvent.keyboard('{ArrowDown}{ArrowDown}');
    expect(items[2]).toHaveFocus();
    await userEvent.keyboard('{ArrowDown}');
    expect(items[0]).toHaveFocus();
    await userEvent.keyboard('{ArrowUp}');
    expect(items[2]).toHaveFocus();

    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('keeps the Dopingle layer open when Escape was already handled (an open micro-question)', async () => {
    loggedInAs('deniz');
    const router = renderApp('/app/denemeler');
    await userEvent.click(await screen.findByRole('link', { name: 'Dopingle' }));
    const handled = (event: KeyboardEvent) => {
      event.preventDefault();
    };
    window.addEventListener('keydown', handled, { capture: true });

    await userEvent.keyboard('{Escape}');
    window.removeEventListener('keydown', handled, { capture: true });

    expect(router.state.location.pathname).toBe('/app/dopingle');
  });
});
