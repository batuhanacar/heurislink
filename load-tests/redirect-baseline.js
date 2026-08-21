import http from 'k6/http';
import { check, fail } from 'k6';

const BASE_URL = __ENV.BASE_URL;

if (!BASE_URL) {
    throw new Error('BASE_URL environment variable is required');
}

export const options = {
    stages: [
        { duration: '15s', target: 10 },
        { duration: '30s', target: 10 },

        { duration: '15s', target: 25 },
        { duration: '30s', target: 25 },

        { duration: '15s', target: 50 },
        { duration: '30s', target: 50 },

        { duration: '15s', target: 0 },
    ],

    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },

    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'p(90)',
        'p(95)',
        'p(99)',
        'max',
    ],
};

export function setup() {
    const payload = JSON.stringify({
        originalUrl: `https://example.com/k6-baseline-${Date.now()}`,
    });

    const response = http.post(
        `${BASE_URL}/urls`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: {
                endpoint: 'setup-create-url',
            },
        }
    );

    const success = check(response, {
        'setup POST /urls returns 201': (res) => res.status === 201,
    });

    if (!success) {
        console.error(`Setup status: ${response.status}`);
        console.error(`Setup body: ${response.body}`);
        fail('Could not create URL for redirect benchmark');
    }

    const body = response.json();

    if (!body.shortCode) {
        fail('Response did not contain shortCode');
    }

    console.log(`Benchmark shortCode: ${body.shortCode}`);

    return {
        shortCode: body.shortCode,
    };
}

export default function (data) {
    const response = http.get(
        `${BASE_URL}/${data.shortCode}`,
        {
            redirects: 0,
            tags: {
                endpoint: 'redirect',
            },
        }
    );

    check(response, {
        'redirect status is valid': (res) =>
            res.status === 301 ||
            res.status === 302 ||
            res.status === 307 ||
            res.status === 308,
    });
}
