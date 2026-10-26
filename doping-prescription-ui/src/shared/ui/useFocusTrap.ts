import { useEffect, useRef } from 'react';

const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

/** The elements inside the container that Tab reaches, in document order. */
export function focusableWithin(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE));
}

/**
 * Keeps keyboard focus inside a modal dialog while it is mounted: focuses its first focusable element, wraps Tab and
 * Shift+Tab at the edges, calls `onEscape` on Escape and gives focus back to the element that had it before.
 */
export function useFocusTrap<T extends HTMLElement>(onEscape: () => void) {
  const containerRef = useRef<T>(null);
  const onEscapeRef = useRef(onEscape);

  useEffect(() => {
    onEscapeRef.current = onEscape;
  }, [onEscape]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    (focusableWithin(container)[0] ?? container).focus();

    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onEscapeRef.current();
      } else if (event.key === 'Tab') {
        wrapTab(event, container);
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus();
    };
  }, []);

  return containerRef;
}

function wrapTab(event: KeyboardEvent, container: HTMLElement) {
  const items = focusableWithin(container);
  const first = items.at(0);
  const last = items.at(-1);
  if (!first || !last) {
    event.preventDefault();
    return;
  }
  const active = document.activeElement;
  const outside = !container.contains(active);
  if (event.shiftKey && (active === first || outside)) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && (active === last || outside)) {
    event.preventDefault();
    first.focus();
  }
}
