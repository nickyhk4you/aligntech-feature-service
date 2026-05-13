import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const evaluationDuration = new Trend('evaluation_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '3m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 100 },  // Ramp up to 100 users
    { duration: '3m', target: 100 },  // Stay at 100 users
    { duration: '1m', target: 200 },  // Ramp up to 200 users
    { duration: '3m', target: 200 },  // Stay at 200 users
    { duration: '2m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<100', 'p(99)<200'], // 95% under 100ms, 99% under 200ms
    http_req_failed: ['rate<0.01'],                 // Error rate < 1%
    errors: ['rate<0.01'],                          // Custom error rate < 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-key';

// Test data
const flagKeys = [
  'checkout-redesign',
  'premium-features',
  'dark-mode',
  'new-onboarding',
  'beta-feature',
];

const regions = ['us-west', 'us-east', 'eu-central', 'ap-south'];
const platforms = ['web', 'mobile', 'api'];

function randomUserId() {
  return `user-${Math.floor(Math.random() * 10000)}`;
}

function randomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export default function () {
  const userId = randomUserId();
  const region = randomElement(regions);
  const platform = randomElement(platforms);

  // Test 1: Evaluate single flag
  const evaluatePayload = JSON.stringify({
    context: {
      userId: userId,
      region: region,
      platform: platform,
      appVersion: '2.1.0',
    },
    flagKeys: [randomElement(flagKeys)],
  });

  const evaluateParams = {
    headers: {
      'Content-Type': 'application/json',
      'X-App-Key': API_KEY,
    },
  };

  const evaluateRes = http.post(
    `${BASE_URL}/api/v1/evaluate`,
    evaluatePayload,
    evaluateParams
  );

  check(evaluateRes, {
    'evaluate status is 200': (r) => r.status === 200,
    'evaluate response has results': (r) => {
      const body = JSON.parse(r.body);
      return body.results !== undefined;
    },
    'evaluate response time < 100ms': (r) => r.timings.duration < 100,
  });

  errorRate.add(evaluateRes.status !== 200);
  evaluationDuration.add(evaluateRes.timings.duration);

  sleep(0.5);

  // Test 2: Snapshot endpoint (less frequently)
  if (Math.random() < 0.1) {
    const snapshotRes = http.get(`${BASE_URL}/api/v1/snapshot`, {
      headers: { 'X-App-Key': API_KEY },
    });

    check(snapshotRes, {
      'snapshot status is 200': (r) => r.status === 200,
      'snapshot has flags': (r) => {
        const body = JSON.parse(r.body);
        return body.flags && body.flags.length > 0;
      },
      'snapshot response time < 200ms': (r) => r.timings.duration < 200,
    });

    errorRate.add(snapshotRes.status !== 200);
  }

  // Test 3: Explain endpoint (less frequently)
  if (Math.random() < 0.05) {
    const flagKey = randomElement(flagKeys);
    const explainRes = http.get(
      `${BASE_URL}/api/v1/explain/${flagKey}?userId=${userId}&region=${region}`,
      { headers: { 'X-App-Key': API_KEY } }
    );

    check(explainRes, {
      'explain status is 200': (r) => r.status === 200,
      'explain has rollout explanation': (r) => {
        const body = JSON.parse(r.body);
        return body.rolloutExplanation !== undefined;
      },
    });

    errorRate.add(explainRes.status !== 200);
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'load-test-results.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  let summary = '\n';
  
  summary += `${indent}Test Summary:\n`;
  summary += `${indent}  Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += `${indent}  Failed Requests: ${data.metrics.http_req_failed.values.passes}\n`;
  summary += `${indent}  Error Rate: ${(data.metrics.errors?.values.rate * 100 || 0).toFixed(2)}%\n`;
  summary += `${indent}  Avg Duration: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `${indent}  P95 Duration: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `${indent}  P99 Duration: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
  
  return summary;
}
