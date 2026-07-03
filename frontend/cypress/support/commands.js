// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

/**
 * Custom Cypress Command to seed a workflow directly into the Postgres DB via the REST API.
 * This guarantees test isolation by setting up deterministic backend state before a UI test begins.
 */
Cypress.Commands.add('createMockWorkflow', (name = 'E2E Seeded Workflow') => {
  const payload = {
    name: name,
    tasks: [
      { name: "Extract Phase" },
      { name: "Load Phase", dependsOn: ["Extract Phase"] }
    ]
  };

  // Bypasses the UI entirely and executes a raw HTTP request to the Spring Boot engine
  cy.request({
    method: 'POST',
    url: 'http://localhost:8080/api/v1/workflows',
    body: payload,
    headers: {
      'Content-Type': 'application/json'
    }
  }).then((response) => {
    // Rigidly assert the seeding succeeded
    expect(response.status).to.eq(201);
    
    // Yield the response body (which contains the new UUID) back to the Cypress test chain
    return response.body;
  });
});
