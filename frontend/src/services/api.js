const BASE_URL = 'http://localhost:8080/api/v1';

export async function getWorkflows() {
  const response = await fetch(`${BASE_URL}/workflows`);
  if (!response.ok) {
    throw new Error(`Failed to fetch workflows: ${response.statusText}`);
  }
  return response.json();
}

export async function getWorkflow(id) {
  const response = await fetch(`${BASE_URL}/workflows/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch workflow ${id}: ${response.statusText}`);
  }
  return response.json();
}

export async function createWorkflow(payload) {
  const response = await fetch(`${BASE_URL}/workflows`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error(`Failed to create workflow: ${response.statusText}`);
  }
  return response.json();
}
