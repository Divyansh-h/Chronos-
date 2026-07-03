describe('Sidebar Navigation E2E', () => {
  it('allows the user to seamlessly navigate across all core application pages', () => {
    // Act 1: Visit the root dashboard (resolves against baseUrl in cypress.config.js)
    cy.visit('/');

    // Assert 1: The browser should initially be at the root path
    cy.url().should('eq', Cypress.config().baseUrl + '/');
    cy.contains('h1', 'Dashboard').should('be.visible');

    // Act 2: Locate the 'Workflows' anchor tag inside the Sidebar <nav> and physically click it
    cy.contains('nav a', 'Workflows').click();

    // Assert 2: React Router should intercept the click and mutate the browser URL without a full page reload
    cy.url().should('include', '/workflows');
    cy.contains('h1', 'Workflows').should('be.visible');

    // Act 3: Click 'Workers'
    cy.contains('nav a', 'Workers').click();

    // Assert 3: Verify the Workers route
    cy.url().should('include', '/workers');
    cy.contains('h1', 'Worker Nodes').should('be.visible');

    // Act 4: Return home
    cy.contains('nav a', 'Dashboard').click();
    cy.url().should('eq', Cypress.config().baseUrl + '/');
  });
});
