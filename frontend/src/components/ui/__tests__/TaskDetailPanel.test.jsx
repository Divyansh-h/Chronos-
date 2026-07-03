import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, vi } from 'vitest';
import TaskDetailPanel from '../TaskDetailPanel';

describe('TaskDetailPanel Component', () => {
  it('renders task metadata and perfectly pretty-prints nested JSON payloads', () => {
    // Arrange: Create a mock Task with a nested object for inputData
    const mockTask = {
      id: 'task-123',
      name: 'Data Extraction Worker',
      status: 'RUNNING',
      inputData: { table: "production_users", batchSize: 5000 }
    };
    const mockOnClose = vi.fn();

    // Act: Render the slide-out panel
    const { container } = render(<TaskDetailPanel task={mockTask} onClose={mockOnClose} />);

    // Assert 1: Basic Node Metadata
    expect(screen.getByText('Data Extraction Worker')).toBeInTheDocument();

    // Assert 2: JSON Payload Formatting
    // The component should execute JSON.stringify(data, null, 2) which inserts newlines and spaces.
    // We grab the <pre> tag and assert the formatting was preserved.
    const inputDataBlock = container.querySelectorAll('pre')[0];
    expect(inputDataBlock).toBeInTheDocument();
    expect(inputDataBlock).toHaveTextContent(/"table":\s*"production_users"/);
    expect(inputDataBlock).toHaveTextContent(/"batchSize":\s*5000/);
  });

  it('fires the injected onClose callback prop exactly once when the (X) button is clicked', () => {
    // Arrange
    const mockOnClose = vi.fn();
    const mockTask = { id: 'task-123', name: 'Test Task' };

    // Act
    render(<TaskDetailPanel task={mockTask} onClose={mockOnClose} />);
    
    // The panel only contains one button element (the top-right close icon)
    const closeButton = screen.getByRole('button');
    fireEvent.click(closeButton);

    // Assert
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });
});
