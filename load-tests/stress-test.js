import http from 'k6/http';
import { check, sleep } from 'k6';

// Stress test - push system to limits
export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Warm up
    { duration: '5m', target: 100 },   // Baseline
    { duration: '2m', target: 200 },   // Increase load
    { duration: '5m', target: 200 },   // Hold
    { duration: '2m', target: 400 },   // Stress
    { duration: '5m', target: 400 },   // Hold stress
    { duration: '2m', target: 800 },   // Peak stress
    { duration: '5m', target: 800 },   // Hold peak
    { duration: '5m', target: 0 },     // Recovery
  ],
  thresholds: {
    http_req_duration: ['p(99)<500'], // Allow higher latency under stress
    http_req_failed: ['rate<0.05'],    // Allow up to 5% errors under peak load
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-key';

export default function () {
  const payload = JSON.stringify({
    context: {
      userId: `user-${__VU}-${__ITER}`,
      region: 'us-west',
      platform: 'web',
    },
    flagKeys: ['test-feature'],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-App-Key': API_KEY,
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/evaluate`, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(0.1);
}
