import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StatusBadge from '../StatusBadge';

describe('StatusBadge Component', () => {
  it('renders RUNNING status with pulse animation correctly', () => {
    // Act
    const { container } = render(<StatusBadge status="RUNNING" />);
    
    // Assert Text is present
    const badgeElement = screen.getByText('RUNNING');
    expect(badgeElement).toBeInTheDocument();
    
    // Assert Outer Node CSS mapping
    expect(badgeElement).toHaveClass('bg-amber-500/20');
    expect(badgeElement).toHaveClass('text-amber-400');
    
    // Assert Inner Pulse Dot is rendered with animation class
    const pulseDot = container.querySelector('.animate-pulse');
    expect(pulseDot).toBeInTheDocument();
    expect(pulseDot).toHaveClass('bg-amber-400');
  });
});
