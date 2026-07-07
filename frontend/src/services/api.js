const BASE_URL = `${import.meta.env.VITE_API_BASE_URL || ''}/api/v1`;

function getHeaders() {
  const apiKey = localStorage.getItem('API_KEY');
  return {
    'Content-Type': 'application/json',
    ...(apiKey ? { 'X-API-Key': apiKey } : {})
  };
}

export async function getWorkflows(page = 0, size = 10) {
  const response = await fetch(`${BASE_URL}/workflows?page=${page}&size=${size}`, {
    headers: getHeaders()
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch workflows: ${response.statusText}`);
  }
  return response.json();
}

export async function getWorkflow(id) {
  const response = await fetch(`${BASE_URL}/workflows/${id}`, {
    headers: getHeaders()
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch workflow ${id}: ${response.statusText}`);
  }
  return response.json();
}

export async function createWorkflow(payload) {
  const response = await fetch(`${BASE_URL}/workflows`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error(`Failed to create workflow: ${response.statusText}`);
  }
  return response.json();
}

export async function cancelWorkflow(id) {
  const response = await fetch(`${BASE_URL}/workflows/${id}/cancel`, {
    method: 'POST',
    headers: getHeaders()
  });
  if (!response.ok) {
    throw new Error(`Failed to cancel workflow: ${response.statusText}`);
  }
}

export async function retryWorkflow(id) {
  const response = await fetch(`${BASE_URL}/workflows/${id}/retry`, {
    method: 'POST',
    headers: getHeaders()
  });
  if (!response.ok) {
    throw new Error(`Failed to retry workflow: ${response.statusText}`);
  }
}

export async function getWorkers(page = 0, size = 20) {
  const response = await fetch(`${BASE_URL}/workers?page=${page}&size=${size}`, {
    headers: getHeaders()
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch workers: ${response.statusText}`);
  }
  return response.json();
}
