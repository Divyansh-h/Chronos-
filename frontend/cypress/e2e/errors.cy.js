describe('Error UI Handling E2E', () => {
  it('displays a graceful red error banner when the backend REST API crashes with a 500 status', () => {
    // Arrange: Simulate a catastrophic backend failure
    // We intercept the network request and force Cypress to return a 500 Internal Server Error
    cy.intercept('GET', 'http://localhost:8080/api/v1/workflows', {
      statusCode: 500,
      body: { message: "Internal Server Error" }
    }).as('failingApiRequest');

    // Act: Visit the dashboard
    cy.visit('/');

    // Ensure our stubbed network request actually executed and failed
    cy.wait('@failingApiRequest');

    // Assert 1: The UI must gracefully catch the error and display our red Alert banner
    // We locate the exact error string and verify its parent container possesses the specific red Tailwind classes
    const errorBanner = cy.contains('p', 'Failed to fetch dashboard data. Please check your network connection.')
      .parent(); 
      
    errorBanner.should('have.class', 'bg-red-500/20');
    errorBanner.should('have.class', 'border-red-500/50');
    errorBanner.should('have.class', 'text-red-400');
    
    // Assert 2: The application should NOT suffer from the "White Screen of Death"
    // The core shell (Dashboard H1) should still render perfectly fine despite the network failure
    cy.contains('h1', 'Dashboard').should('be.visible');
  });
});
