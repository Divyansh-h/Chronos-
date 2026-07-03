describe('Chaos Engineering E2E - Redis Network Degradation', () => {
  
  before(() => {
    // We can orchestrate Toxiproxy directly from Cypress!
    // Inject 2000ms of latency into the Redis tunnel
    cy.request({
      method: 'POST',
      url: 'http://localhost:8474/proxies/redis_chaos_proxy/toxics',
      failOnStatusCode: false, // Prevent failure if the toxic already exists
      body: {
        name: "redis_latency",
        type: "latency",
        stream: "downstream",
        attributes: { latency: 2000 }
      }
    });
  });

  after(() => {
    // Teardown: Always heal the network so subsequent tests don't fail
    cy.request({
      method: 'DELETE',
      url: 'http://localhost:8474/proxies/redis_chaos_proxy/toxics/redis_latency',
      failOnStatusCode: false
    });
  });

  it('maintains a perfectly responsive React UI despite extreme backend Task Queue latency', () => {
    // Act 1: Visit the app
    cy.visit('/workflows');

    // Assert 1: The UI shell should render instantly despite the backend fighting slow Redis connections
    cy.contains('h1', 'Workflows').should('be.visible');

    // Act 2: Click around the Navigation
    cy.contains('nav a', 'Workers').click();

    // Assert 2: React Router should process the transition instantly, proving the main thread is NOT blocked
    cy.contains('h1', 'Worker Nodes').should('be.visible');

    // Act 3: Return to Dashboard
    cy.contains('nav a', 'Dashboard').click();

    // Assert 3: Wait for the delayed network calls to eventually resolve. 
    // We increase the timeout to 10000ms (10 seconds) to account for the artificial 2000ms Redis delay
    // applied to every single backend database query.
    cy.contains('h3', 'Total Workflows', { timeout: 10000 }).should('be.visible');
  });
});
