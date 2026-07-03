import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import DAGVisualizer from '../DAGVisualizer';

describe('DAGVisualizer Component', () => {
  it('calculates correct topological levels and absolute positions for a branched DAG', () => {
    // Arrange: Create a DAG with 1 root and 2 parallel children
    const mockTasks = [
      { id: '1', name: 'Root', status: 'COMPLETED' },
      { id: '2', name: 'Child 1', status: 'RUNNING', dependsOn: ['Root'] },
      { id: '3', name: 'Child 2', status: 'PENDING', dependsOn: ['Root'] }
    ];

    // Act
    render(<DAGVisualizer tasks={mockTasks} />);

    // Assert: Root Node (Level 0)
    // Math -> X = offset(150) + level(0) * spacing(250) = 150
    // Math -> Y = containerHeight(600) / (levelNodes(1) + 1) * index(1) = 300
    const rootNode = screen.getByText('Root').closest('div');
    expect(rootNode).toHaveStyle({ left: '150px', top: '300px' });

    // Assert: Child 1 Node (Level 1, Index 0)
    // Math -> X = offset(150) + level(1) * spacing(250) = 400
    // Math -> Y = containerHeight(600) / (levelNodes(2) + 1) * index(1) = 200
    const child1Node = screen.getByText('Child 1').closest('div');
    expect(child1Node).toHaveStyle({ left: '400px', top: '200px' });

    // Assert: Child 2 Node (Level 1, Index 1)
    // Math -> X = offset(150) + level(1) * spacing(250) = 400
    // Math -> Y = containerHeight(600) / (levelNodes(2) + 1) * index(2) = 400
    const child2Node = screen.getByText('Child 2').closest('div');
    expect(child2Node).toHaveStyle({ left: '400px', top: '400px' });
  });
});
