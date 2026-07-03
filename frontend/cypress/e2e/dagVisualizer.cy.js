describe('Interactive DAG Visualizer E2E', () => {
  it('renders SVG connection paths and opens the sliding TaskDetailPanel on node click', () => {
    // Arrange: Stub the single workflow fetch request with a mock DAG containing a parent and child
    cy.intercept('GET', 'http://localhost:8080/api/v1/workflows/1', {
      statusCode: 200,
      body: {
        id: '1',
        name: 'Visualizer Test Workflow',
        status: 'RUNNING',
        tasks: [
          { id: 'task-a', name: 'Root Data Extraction', status: 'COMPLETED' },
          { id: 'task-b', name: 'Dependent Transformation', status: 'RUNNING', dependsOn: ['Root Data Extraction'] }
        ]
      }
    }).as('getWorkflowDetailsApi');

    // Act 1: Visit the details page directly
    cy.visit('/workflows/1');

    // Ensure our intercepted request was triggered and resolved
    cy.wait('@getWorkflowDetailsApi');

    // Assert 1: The SVG layer must exist and possess exactly 1 bezier curve <path> connecting Root to Child
    cy.get('.pointer-events-none path').should('exist').and('have.length', 1);

    // Act 2: Locate the absolute positioned HTML node for the Child task and click it
    cy.contains('div', 'Dependent Transformation').click();

    // Assert 2: The TaskDetailPanel should instantly slide into view (we assert the header exists)
    cy.contains('h2', 'Task Details').should('be.visible');

    // Assert 3: The panel should perfectly reflect the data of the node we just clicked
    // We query the DOM for the "Name" label, traverse to the parent div, and verify the value span
    cy.contains('span', 'Name')
      .parent()
      .find('span.text-white.font-medium')
      .should('have.text', 'Dependent Transformation');
      
    // Assert 4: Status badge rendered correctly inside the panel
    cy.contains('span', 'Status')
      .parent()
      .contains('RUNNING')
      .should('be.visible');

    // Act 3: Close the panel
    cy.get('aside button').click();

    // Act 4: Dispatch a custom DOM event simulating a real-time STOMP payload arriving from the Spring Boot backend
    cy.window().then((win) => {
      const stompPayload = {
        workflowId: '1',
        taskId: 'task-b',
        status: 'COMPLETED'
      };
      // The `useTaskflowEvents` hook listens to this when window.Cypress is true
      const event = new CustomEvent('Cypress-STOMP', { detail: stompPayload });
      win.dispatchEvent(event);
    });

    // Assert 5: The DAG node should instantly react to the new state and turn emerald-500
    cy.contains('div', 'Dependent Transformation')
      .should('have.class', 'border-emerald-500');
  });
});
