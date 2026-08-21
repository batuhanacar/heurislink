import http from 'k6/http';
import { check, fail } from 'k6';

const BASE_URL = __ENV.BASE_URL;

if (!BASE_URL) {
    throw new Error('BASE_URL environment variable is required');
}

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        checks: ['rate==1'],
        http_req_failed: ['rate==0'],
    },
};

export default function () {
    const payload = JSON.stringify({
        originalUrl: `https://example.com/k6-smoke-${Date.now()}`,
    });

    const createResponse = http.post(
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

    const created = check(createResponse, {
        'POST /urls returns 201': (response) => response.status === 201,
    });

    if (!created) {
        console.error(`POST status: ${createResponse.status}`);
        console.error(`POST body: ${createResponse.body}`);
        fail('Could not create short URL');
    }

    const responseBody = createResponse.json();

    check(responseBody, {
        'response contains shortCode': (body) =>
            body.shortCode !== undefined &&
            body.shortCode !== null &&
            body.shortCode.length > 0,
    });

    const shortCode = responseBody.shortCode;

    const redirectResponse = http.get(
        `${BASE_URL}/${shortCode}`,
        {
            redirects: 0,
            tags: {
                endpoint: 'redirect',
            },
        }
    );

    check(redirectResponse, {
        'redirect endpoint returns redirect': (response) =>
            response.status === 301 ||
            response.status === 302 ||
            response.status === 307 ||
            response.status === 308,
    });
}
