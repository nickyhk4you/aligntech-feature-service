import http from 'k6/http';
import { check } from 'k6';

// Spike test - sudden traffic spikes
export const options = {
  stages: [
    { duration: '10s', target: 100 },   // Normal load
    { duration: '1m', target: 100 },    // Hold
    { duration: '10s', target: 1000 },  // Spike!
    { duration: '1m', target: 1000 },   // Hold spike
    { duration: '10s', target: 100 },   // Recovery
    { duration: '1m', target: 100 },    // Hold normal
    { duration: '10s', target: 0 },     // Shutdown
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // Allow degradation during spike
    http_req_failed: ['rate<0.1'],      // Allow 10% errors during spike
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-key';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/snapshot`, {
    headers: { 'X-App-Key': API_KEY },
  });

  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });
}
