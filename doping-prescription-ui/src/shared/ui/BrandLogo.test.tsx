import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { BrandLogo } from './BrandLogo';

describe('BrandLogo', () => {
  it('shows the company logo with an accessible name and the demo label', () => {
    render(<BrandLogo />);

    expect(screen.getByRole('img', { name: 'Doping Hafıza' })).toHaveAttribute('src', '/brand/logo.svg');
    expect(screen.getByText('Prescription demo')).toBeInTheDocument();
  });
});
