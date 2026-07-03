describe('Create Workflow E2E Journey', () => {
  it('validates the complete user journey of drafting and submitting a new DAG workflow', () => {
    // Arrange: Stub the backend POST endpoint
    // We use cy.intercept to trap the network request, preventing Cypress from mutating our real Postgres DB.
    cy.intercept('POST', 'http://localhost:8080/api/v1/workflows', {
      statusCode: 201,
      body: {
        id: 'e2e-workflow-777',
        name: 'E2E Test Workflow',
        status: 'PENDING',
        tasks: [
          { id: 'task-1', name: 'Task 1', status: 'PENDING' }
        ]
      }
    }).as('createWorkflowApi');

    // Act 1: Navigate to the workflows dashboard
    cy.visit('/workflows');

    // Act 2: Trigger the creation modal
    cy.contains('button', 'Create Workflow').click();

    // Act 3: Fill out the workflow's human-readable name
    cy.get('input[type="text"]').type('E2E Test Workflow');

    // Act 4: Inject the JSON payload representing our execution DAG
    const validJsonDAG = `[
      { "name": "Task 1" }
    ]`;
    
    // We disable parseSpecialCharSequences so Cypress physically types the `{` and `}` brackets without interpreting them as keyboard shortcuts
    cy.get('textarea').clear().type(validJsonDAG, { parseSpecialCharSequences: false });

    // Act 5: Submit the form
    cy.contains('button', 'Submit').click();

    // Assert 1: The browser correctly negotiated the network fetch with the exact payload we typed
    cy.wait('@createWorkflowApi').then((interception) => {
      expect(interception.request.body.name).to.equal('E2E Test Workflow');
      // Validates that our UI actually parsed the stringified JSON into a real array before POSTing
      expect(interception.request.body.tasks[0].name).to.equal('Task 1');
    });

    // Assert 2: The modal gracefully unmounts/closes after success
    cy.contains('button', 'Submit').should('not.exist');

    // Assert 3: The newly returned stubbed workflow automatically renders in the Data Grid
    cy.contains('E2E Test Workflow').should('be.visible');
    cy.contains('e2e-workflow-777').should('be.visible');
  });
});
