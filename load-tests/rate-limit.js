import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL;

if (!BASE_URL) {
    throw new Error('BASE_URL environment variable is required');
}

const accepted = new Counter('rate_limit_accepted');
const rejected = new Counter('rate_limit_rejected');

export const options = {
    vus: 1,
    iterations: 15,

    thresholds: {
        checks: ['rate==1'],
        rate_limit_accepted: ['count==10'],
        rate_limit_rejected: ['count==5'],
    },
};

export default function () {
    const payload = JSON.stringify({
        originalUrl:
            `https://example.com/rate-limit-${__VU}-${__ITER}-${Date.now()}`,
    });

    const response = http.post(
        `${BASE_URL}/urls`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    if (response.status === 201) {
        accepted.add(1);
    }

    if (response.status === 429) {
        rejected.add(1);
    }

    check(response, {
        'status is 201 or 429': (res) =>
            res.status === 201 || res.status === 429,
    });
}
