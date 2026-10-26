import { type KeyboardEvent, useCallback, useEffect, useRef } from 'react';

const ITEM = '[role="menuitem"]';

/**
 * Keyboard support of a drop-down menu (WAI-ARIA menu button pattern): opening focuses the first item, ↑/↓ move
 * between items (wrapping), Home/End jump to the ends, Escape closes the menu and returns focus to the trigger, and Tab
 * closes it. Attach `menuRef` and `onKeyDown` to the element with role="menu" and `triggerRef` to its button.
 */
export function useMenuNavigation<M extends HTMLElement, T extends HTMLElement>(
  open: boolean,
  close: () => void,
) {
  const menuRef = useRef<M>(null);
  const triggerRef = useRef<T>(null);

  useEffect(() => {
    if (open) {
      menuItems(menuRef.current).at(0)?.focus();
    }
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    // Escape also closes the menu when focus has left it (e.g. after a click elsewhere on the page).
    const onKey = (event: globalThis.KeyboardEvent) => {
      if (event.key === 'Escape') {
        close();
        triggerRef.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
    };
  }, [open, close]);

  const onKeyDown = useCallback(
    (event: KeyboardEvent) => {
      const items = menuItems(menuRef.current);
      const current = items.findIndex((item) => item === document.activeElement);
      const target = nextIndex(event.key, current, items.length);
      if (target !== undefined) {
        event.preventDefault();
        items[target]?.focus();
      } else if (event.key === 'Tab') {
        close();
      }
    },
    [close],
  );

  return { menuRef, triggerRef, onKeyDown };
}

function menuItems(menu: HTMLElement | null): HTMLElement[] {
  return menu ? Array.from(menu.querySelectorAll<HTMLElement>(ITEM)) : [];
}

function nextIndex(key: string, current: number, count: number): number | undefined {
  switch (key) {
    case 'ArrowDown':
      return (current + 1) % count;
    case 'ArrowUp':
      return current <= 0 ? count - 1 : current - 1;
    case 'Home':
      return 0;
    case 'End':
      return count - 1;
    default:
      return undefined;
  }
}
