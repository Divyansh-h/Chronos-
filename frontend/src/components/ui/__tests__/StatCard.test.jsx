import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StatCard from '../StatCard';

describe('StatCard Component', () => {
  it('renders title and value correctly', () => {
    // Act
    render(<StatCard title="Active" value="42" />);
    
    // Assert
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('applies the glass-panel CSS class', () => {
    // Act
    const { container } = render(<StatCard title="Test" value="0" />);
    
    // Assert
    expect(container.firstChild).toHaveClass('glass-panel');
  });
});
