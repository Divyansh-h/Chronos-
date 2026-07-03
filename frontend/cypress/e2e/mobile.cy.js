describe('Responsive Mobile Layout E2E', () => {
  it('gracefully collapses the Sidebar off-screen on small viewports and toggles via a hamburger menu', () => {
    // Arrange: Force the Cypress Chromium engine to resize to exact mobile dimensions
    cy.viewport('iphone-x');
    
    // Act 1: Visit the app
    cy.visit('/');

    // Assert 1: The Sidebar container must be pushed completely off the left side of the screen
    cy.contains('aside', 'TASKFLOW')
      .parent() // Targets the wrapper div applying the responsive classes
      .should('have.class', '-translate-x-full');

    // Act 2: Physically tap the fixed Hamburger menu icon floating on the top left
    cy.get('[data-testid="mobile-menu-btn"]').click();

    // Assert 2: The Sidebar container should immediately remove the negative translation and slide into view
    cy.contains('aside', 'TASKFLOW')
      .parent()
      .should('have.class', 'translate-x-0')
      .and('not.have.class', '-translate-x-full');

    // Assert 3: The underlying modal backdrop overlay should now exist in the DOM
    cy.get('.backdrop-blur-sm').should('exist');

    // Act 3: Click the backdrop to dismiss the menu
    cy.get('.backdrop-blur-sm').click({ force: true }); // force:true because clicking an overlay might be intercepted

    // Assert 4: Verify it collapses again
    cy.contains('aside', 'TASKFLOW')
      .parent()
      .should('have.class', '-translate-x-full');
  });
});
