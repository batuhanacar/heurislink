import http from 'k6/http';
import { check, fail } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL;

if (!BASE_URL) {
    throw new Error('BASE_URL environment variable is required');
}

const cacheMissDuration = new Trend('cache_miss_duration', true);
const cacheHitDuration = new Trend('cache_hit_duration', true);

export const options = {
    vus: 1,
    iterations: 100,

    thresholds: {
        checks: ['rate==1'],
        cache_miss_duration: ['p(95)<1000'],
        cache_hit_duration: ['p(95)<1000'],
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
            `https://example.com/cache-test-${__VU}-${__ITER}-${Date.now()}`,
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
        'URL created successfully': (res) => res.status === 201,
    });

    if (!created) {
        console.error(`Create status: ${createResponse.status}`);
        console.error(`Create body: ${createResponse.body}`);
        fail('Could not create short URL');
    }

    const shortCode = createResponse.json().shortCode;

    if (!shortCode) {
        fail('Response did not contain shortCode');
    }

    const coldResponse = http.get(
        `${BASE_URL}/${shortCode}`,
        {
            redirects: 0,
            tags: {
                cache: 'miss',
            },
        }
    );

    cacheMissDuration.add(coldResponse.timings.duration);

    check(coldResponse, {
        'cold redirect succeeds': (res) =>
            res.status === 301 ||
            res.status === 302 ||
            res.status === 307 ||
            res.status === 308,
    });

    const warmResponse = http.get(
        `${BASE_URL}/${shortCode}`,
        {
            redirects: 0,
            tags: {
                cache: 'hit',
            },
        }
    );

    cacheHitDuration.add(warmResponse.timings.duration);

    check(warmResponse, {
        'warm redirect succeeds': (res) =>
            res.status === 301 ||
            res.status === 302 ||
            res.status === 307 ||
            res.status === 308,
    });
}
