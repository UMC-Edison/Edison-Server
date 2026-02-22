import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const oldApiDuration = new Trend('old_api_duration');
const newApiDuration = new Trend('new_api_duration');

const BASE_URL = 'http://host.docker.internal:8080';
const AUTH_TOKEN = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwiZXhwIjoxNzcxOTc0ODE5LCJlbWFpbCI6ImVzdGVsbGUwMzI5QGV3aGEuYWMua3IifQ.1zXjeQW5KhPPz2J91y3_6O2cHX1gApHjqgXnhEoSd1E';

export const options = {
  vus: 5,
  duration: '30s',
};

export default function () {
  const params = {
    headers: {
      'Authorization': `Bearer ${AUTH_TOKEN}`,
      'Content-Type': 'application/json',
    },
  };

  const oldRes = http.get(`${BASE_URL}/spaces/map`, params);
  oldApiDuration.add(oldRes.timings.duration);
  check(oldRes, { 'old API 200': (r) => r.status === 200 });

  sleep(1);

  const newRes = http.get(`${BASE_URL}/bubbles/embeddings`, params);
  newApiDuration.add(newRes.timings.duration);
  check(newRes, { 'new API 200': (r) => r.status === 200 });

  sleep(1);
}