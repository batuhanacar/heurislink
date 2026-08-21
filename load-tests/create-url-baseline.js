import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL;

if (!BASE_URL) {
    throw new Error('BASE_URL environment variable is required');
}

export const options = {
    stages: [
        { duration: '15s', target: 5 },
        { duration: '30s', target: 5 },

        { duration: '15s', target: 10 },
        { duration: '30s', target: 10 },

        { duration: '15s', target: 20 },
        { duration: '30s', target: 20 },

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

export default function () {
    const payload = JSON.stringify({
        originalUrl:
            `https://example.com/k6-create-${__VU}-${__ITER}-${Date.now()}`,
    });

    const response = http.post(
        `${BASE_URL}/urls`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: {
                endpoint: 'create-url',
            },
        }
    );

    check(response, {
        'POST /urls returns 201': (res) => res.status === 201,

        'response contains shortCode': (res) => {
            if (res.status !== 201) {
                return false;
            }

            try {
                const body = res.json();

                return (
                    body.shortCode !== undefined &&
                    body.shortCode !== null &&
                    body.shortCode.length > 0
                );
            } catch {
                return false;
            }
        },
    });
}
