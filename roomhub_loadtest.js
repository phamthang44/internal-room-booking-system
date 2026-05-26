import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ── Custom metrics ────────────────────────────────────────────────────────────
const errorRate    = new Rate('error_rate');
const bookingMs    = new Trend('booking_latency_ms', true);
const conflictCount = new Counter('booking_conflicts_409');

// ── Test config ───────────────────────────────────────────────────────────────
export const options = {
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    scenarios: {
        // Primary: concurrent booking writes — the concurrency stress test
        concurrent_bookings: {
            executor: 'ramping-vus',
            exec: 'createBooking',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 10 },  // warm up
                { duration: '20s', target: 50 },  // ramp to 50 concurrent users
                { duration: '30s', target: 50 },  // sustain peak load
                { duration: '10s', target: 0 },   // ramp down
            ],
        },
        // Secondary: read traffic running in parallel — proves reads stay fast under write pressure
        room_reads: {
            executor: 'constant-vus',
            exec: 'readClassrooms',
            vus: 20,
            duration: '70s',
            startTime: '0s',
        },
    },
    thresholds: {
        // Use the built-in scenario name tag (not a custom one — they conflict)
        'http_req_duration{scenario:concurrent_bookings}': ['p(95)<2000'],
        'http_req_duration{scenario:room_reads}':          ['p(95)<500'],
        error_rate: ['rate<0.05'],
    },
};

// ── Config — update before running ───────────────────────────────────────────
const BASE_URL = 'https://api.roomhub.online';

// Credentials for auto-login — edit these or pass via env:
//   k6 run -e USERNAME=you@uni.edu.vn -e PASSWORD=yourpass roomhub_loadtest.js
const USERNAME = __ENV.USERNAME || 'student01@uni.edu.vn';
const PASSWORD = __ENV.PASSWORD || 'yourpassword';

// Spread across more IDs to reduce natural collision between VUs.
// Add real IDs from your DB for best results.
const CLASSROOM_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const TIME_SLOT_IDS = [1, 2, 3, 4, 5, 6, 7, 8];

// ── Auto-login (runs once before any VUs start) ───────────────────────────────
export function setup() {
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ identifier: USERNAME, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status !== 200) {
        throw new Error(`Login failed (${res.status}): ${res.body}`);
    }

    const token = res.json('data.accessToken');
    if (!token) {
        throw new Error(`No accessToken in login response: ${res.body}`);
    }

    console.log(`[setup] Logged in as ${USERNAME} — token acquired`);
    return { token };
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function randomFrom(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function futureDate(daysAhead) {
    const d = new Date();
    d.setDate(d.getDate() + daysAhead);
    return d.toISOString().split('T')[0]; // "YYYY-MM-DD"
}

// Unique key per VU × iteration — exercises the real idempotency path in production.
// The X-Idempotency-Key header prevents duplicate booking on network retries.
function idempotencyKey() {
    return `k6-vu${__VU}-iter${__ITER}-${Date.now()}`;
}

function buildPayload() {
    // Stay within the 7-day booking window enforced by policy BK_002.
    // 7 days × 10 classrooms × 8 slots = 560 unique combinations — enough headroom for 50 VUs.
    const daysAhead   = 1 + ((__VU * 3 + __ITER * 2) % 7);
    const classroomId = CLASSROOM_IDS[(__VU * 3 + __ITER) % CLASSROOM_IDS.length];
    const timeSlotId  = TIME_SLOT_IDS[(__VU + __ITER * 5) % TIME_SLOT_IDS.length];
    const bookingDate = futureDate(daysAhead);

    return JSON.stringify({
        classroomId,
        bookingDate,
        timeSlotIds: [timeSlotId],
        timeBooking: `${bookingDate}T03:00:00.000Z`,
        purpose: `k6-load-test-vu${__VU}-iter${__ITER}`,
        attendees: 2,
    });
}

function writeHeaders(token) {
    return {
        'Content-Type':      'application/json',
        'Authorization':     `Bearer ${token}`,
        'X-Idempotency-Key': idempotencyKey(),
    };
}

function readHeaders(token) {
    return { 'Authorization': `Bearer ${token}` };
}

// ── Scenario 1: POST /api/v1/bookings (write, 50 VUs) ────────────────────────
export function createBooking(data) {
    const res = http.post(`${BASE_URL}/api/v1/bookings`, buildPayload(), { headers: writeHeaders(data.token) });

    bookingMs.add(res.timings.duration);

    if (res.status === 409) {
        conflictCount.add(1);
    }

    // Business-rule rejections (400, 403, 422) are expected under real data conditions
    // and do NOT indicate server instability — only 5xx does.
    const ok = check(res, {
        'status 2xx or expected 4xx': (r) => r.status < 500,
        'response has body':          (r) => r.body && r.body.length > 0,
    });

    errorRate.add(!ok);

    // Log first failure of each VU so you can see the actual business-rule message
    if (__ITER === 0 && (res.status < 200 || res.status >= 300)) {
        console.log(`[VU=${__VU} first response] ${res.status} → ${res.body.substring(0, 300)}`);
    }

    sleep(0.5);
}

// ── Scenario 2: GET /api/v1/classrooms (read, 20 VUs, parallel) ──────────────
export function readClassrooms(data) {
    const res = http.get(`${BASE_URL}/api/v1/rooms?page=0&size=12`, { headers: readHeaders(data.token) });

    check(res, {
        'rooms 200': (r) => r.status === 200,
    });

    // Log once per VU so you can see the actual URL/auth issue
    if (__ITER === 0 && res.status !== 200) {
        console.log(`[READ VU=${__VU}] ${res.status} → ${res.body.substring(0, 300)}`);
    }

    sleep(1);
}

// ── Summary ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const m = data.metrics;

    const ms   = (key) => (m.http_req_duration && m.http_req_duration.values[key] != null)
                            ? Math.round(m.http_req_duration.values[key]) + 'ms' : 'n/a';
    const reqs      = m.http_reqs ? m.http_reqs.values.count : 0;
    const rps       = m.http_reqs ? m.http_reqs.values.rate.toFixed(1) : '0';
    const errPct    = (m.error_rate ? m.error_rate.values.rate * 100 : 0).toFixed(1);
    const conflicts = m.booking_conflicts_409 ? m.booking_conflicts_409.values.count : 0;

    const pad = (s) => String(s).padEnd(21);

    console.log('\n╔════════════════════════════════════════════╗');
    console.log('║        ROOMHUB LOAD TEST RESULTS           ║');
    console.log('╠════════════════════════════════════════════╣');
    console.log(`║  Peak VUs         : ${pad('50 concurrent users')}║`);
    console.log(`║  Total requests   : ${pad(reqs)}║`);
    console.log(`║  Throughput       : ${pad(rps + ' RPS')}║`);
    console.log('╠════════════════════════════════════════════╣');
    console.log(`║  Median latency   : ${pad(ms('med'))}║`);
    console.log(`║  p95 latency      : ${pad(ms('p(95)'))}║  ← CV metric`);
    console.log(`║  p99 latency      : ${pad(ms('p(99)'))}║`);
    console.log('╠════════════════════════════════════════════╣');
    console.log(`║  Error rate       : ${pad(errPct + '%')}║  (target < 5%)`);
    console.log(`║  409 Conflicts    : ${pad(conflicts)}║  (expected — concurrency)`);
    console.log('╚════════════════════════════════════════════╝\n');

    return {};
}
