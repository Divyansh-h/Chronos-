import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Sidebar from '../Sidebar';

describe('Sidebar Navigation Component', () => {
  it('applies dynamic active highlight CSS classes based on the React Router DOM location', () => {
    // Act: Mount the Sidebar wrapped inside a virtual memory router.
    // By forcing the initial entry to '/workflows', we trick the NavLink into thinking it is active.
    render(
      <MemoryRouter initialEntries={['/workflows']}>
        <Sidebar />
      </MemoryRouter>
    );

    // Assert 1: The 'Workflows' link should resolve as active and receive the vibrant brand highlight classes
    const workflowsLink = screen.getByText('Workflows');
    expect(workflowsLink).toBeInTheDocument();
    expect(workflowsLink).toHaveClass('bg-brand-blue/20');
    expect(workflowsLink).toHaveClass('text-brand-blue');
    expect(workflowsLink).toHaveClass('font-medium');
    expect(workflowsLink).not.toHaveClass('text-slate-400');

    // Assert 2: The 'Dashboard' link should correctly resolve as inactive and remain muted
    const dashboardLink = screen.getByText('Dashboard');
    expect(dashboardLink).toBeInTheDocument();
    expect(dashboardLink).not.toHaveClass('bg-brand-blue/20');
    expect(dashboardLink).not.toHaveClass('text-brand-blue');
    expect(dashboardLink).toHaveClass('text-slate-400');
  });
});
