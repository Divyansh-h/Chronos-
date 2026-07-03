import http from 'k6/http';
import { sleep, check } from 'k6';

// k6 Configuration Options
export const options = {
  // Simulate 500 aggressive worker nodes all trying to pull from Redis simultaneously
  vus: 500,
  duration: '30s',
  
  // Strict Service Level Objectives (SLOs)
  thresholds: {
    // Assert that 95% of all requests complete in under 50ms, proving Redis lock contention is minimal
    http_req_duration: ['p(95)<50'], 
    // Assert less than 1% of requests fail
    http_req_failed: ['rate<0.01'], 
  },
};

export default function () {
  // Assuming a generic internal polling endpoint exists for the workers
  const url = 'http://localhost:8080/api/v1/tasks/poll';
  
  const payload = JSON.stringify({
    workerId: `worker-node-${__VU}`,
    maxTasks: 1
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Act: 500 virtual users smash the exact same endpoint simultaneously
  const res = http.post(url, payload, params);

  // Assert: The backend should either return 200 (Task Claimed) or 204 (Queue Empty)
  // If it returns 500 or 409, our concurrency handling (OptimisticLocking) has failed under pressure.
  check(res, {
    'is status 200 or 204': (r) => r.status === 200 || r.status === 204,
  });

  // 100ms pause prevents the testing machine from running out of TCP sockets
  sleep(0.1);
}
