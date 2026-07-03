import http from 'k6/http';
import { sleep, check } from 'k6';

// k6 Configuration Options
export const options = {
  // Simulate 50 concurrent virtual users (VUs)
  vus: 50,
  // Sustain this intense load for exactly 30 seconds
  duration: '30s',
};

export default function () {
  const url = 'http://localhost:8080/api/v1/workflows';
  
  // Construct a complex, deeply nested Directed Acyclic Graph (DAG) payload
  const payload = JSON.stringify({
    name: `Load Test Workflow - ${__VU}-${__ITER}`,
    tasks: [
      { name: "Extract Data", status: "PENDING" },
      { name: "Transform Step 1", status: "PENDING", dependsOn: ["Extract Data"] },
      { name: "Transform Step 2", status: "PENDING", dependsOn: ["Extract Data"] },
      { name: "Aggregate Results", status: "PENDING", dependsOn: ["Transform Step 1", "Transform Step 2"] },
      { name: "Load Database", status: "PENDING", dependsOn: ["Aggregate Results"] },
      { name: "Send Notifications", status: "PENDING", dependsOn: ["Load Database"] }
    ]
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Act: Hammer the backend API with the POST request
  const res = http.post(url, payload, params);

  // Assert: Verify the server is successfully creating records and returning HTTP 201 Created
  check(res, {
    'is status 201': (r) => r.status === 201,
  });

  // Brief pause to simulate realistic human/client network latency
  sleep(0.5);
}
