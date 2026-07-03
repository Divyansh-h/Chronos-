import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Dashboard from '../Dashboard';
import { getWorkflows } from '../../services/api';

// 1. Intercept the network API service
vi.mock('../../services/api', () => ({
  getWorkflows: vi.fn(),
}));

describe('Dashboard Page Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('displays a loading state and seamlessly resolves to actual data', async () => {
    // Arrange: Create a manual Promise so we can control exactly WHEN the network responds
    let resolveApi;
    const pendingPromise = new Promise((resolve) => {
      resolveApi = resolve;
    });
    
    // Inject the pending promise into the component
    getWorkflows.mockReturnValue(pendingPromise);

    // Act 1: Mount the Dashboard
    render(<Dashboard />);

    // Assert 1: The StatCards should instantly fall back to the "..." loading state
    // There are 3 stats (Total, Active, Failed) that depend on this loading state
    const loadingIndicators = screen.getAllByText('...');
    expect(loadingIndicators).toHaveLength(3);

    // Act 2: Manually resolve the network request with 2 mock workflows
    const mockWorkflows = [
      { id: '1', status: 'RUNNING' },
      { id: '2', status: 'FAILED' }
    ];
    
    // Wrap the resolution in act() to tell React a state transition is happening
    act(() => {
      resolveApi(mockWorkflows);
    });

    // Assert 2: Wait for React to re-render, then assert the loading state is entirely gone
    await waitFor(() => {
      expect(screen.queryByText('...')).not.toBeInTheDocument();
    });

    // Assert 3: The real numbers should now be visible in the DOM
    // Total Workflows = 2
    expect(screen.getByText('2')).toBeInTheDocument();
    
    // Active Workflows = 1, Failed Workflows = 1
    const ones = screen.getAllByText('1');
    expect(ones.length).toBeGreaterThanOrEqual(2);
  });
});
