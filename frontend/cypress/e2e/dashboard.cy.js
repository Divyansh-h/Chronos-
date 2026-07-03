describe('Dashboard Aggregation E2E', () => {
  it('intercepts the GET request using a fixture and validates UI telemetry math', () => {
    // Arrange: Intercept the REST API and inject our deterministic fixture data.
    // This removes all backend flakiness and guarantees the array always possesses exactly 5 items.
    cy.intercept('GET', 'http://localhost:8080/api/v1/workflows', { fixture: 'workflows.json' }).as('getWorkflowsApi');

    // Act: Visit the root URL (Dashboard)
    cy.visit('/');

    // Wait for our stubbed network request to explicitly resolve before taking DOM measurements
    cy.wait('@getWorkflowsApi');

    // Assert: Total Workflows = 5 (2 RUNNING, 1 FAILED, 2 COMPLETED)
    // We locate the StatCard by finding the title, traversing to the parent div, and asserting the text of the <p> tag.
    cy.contains('h3', 'Total Workflows')
      .parent()
      .find('p')
      .should('have.text', '5');

    // Assert: Active Workflows = 2
    cy.contains('h3', 'Active Workflows')
      .parent()
      .find('p')
      .should('have.text', '2');

    // Assert: Failed Workflows = 1
    cy.contains('h3', 'Failed Workflows')
      .parent()
      .find('p')
      .should('have.text', '1');
  });
});
