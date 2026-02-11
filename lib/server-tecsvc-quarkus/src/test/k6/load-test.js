/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft
 */

/**
 * K6 Load Test Script for OData Technical Service
 *
 * This script exercises common OData operations to generate representative
 * execution profiles for Profile-Guided Optimization (PGO).
 *
 * Usage:
 *   k6 run --vus 10 --duration 60s src/test/k6/load-test.js
 *
 * Prerequisites:
 *   - Install k6: https://k6.io/docs/get-started/installation/
 *   - Start the instrumented application first
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const serviceDocTrend = new Trend('service_document_duration');
const metadataTrend = new Trend('metadata_duration');
const entitySetTrend = new Trend('entity_set_duration');
const singleEntityTrend = new Trend('single_entity_duration');
const queryTrend = new Trend('query_duration');

// Configuration - adjust BASE_URL for your environment
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ODATA_PATH = __ENV.ODATA_PATH || '/odata-server-tecsvc/odata.svc';
const SERVICE_URL = `${BASE_URL}${ODATA_PATH}`;

// Test configuration
export const options = {
    scenarios: {
        // Warm-up phase
        warmup: {
            executor: 'constant-vus',
            vus: 2,
            duration: '10s',
            startTime: '0s',
            tags: { phase: 'warmup' },
        },
        // Main load phase - exercises all code paths
        main_load: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { duration: '20s', target: 10 },
                { duration: '30s', target: 20 },
                { duration: '20s', target: 10 },
                { duration: '10s', target: 0 },
            ],
            startTime: '10s',
            tags: { phase: 'main' },
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],  // Less than 5% errors
        http_req_duration: ['p(95)<500'], // 95% of requests under 500ms
    },
};

// Common headers for OData requests
const headers = {
    'Accept': 'application/json',
    'OData-Version': '4.0',
};

const xmlHeaders = {
    'Accept': 'application/xml',
    'OData-Version': '4.0',
};

/**
 * Main test function - called for each virtual user iteration
 */
export default function () {
    // Distribute requests across different OData operations
    // to exercise various code paths
    const scenario = Math.random();

    if (scenario < 0.10) {
        testServiceDocument();
    } else if (scenario < 0.15) {
        testMetadataJson();
    } else if (scenario < 0.20) {
        testMetadataXml();
    } else if (scenario < 0.40) {
        testEntitySetRead();
    } else if (scenario < 0.55) {
        testEntitySetWithSelect();
    } else if (scenario < 0.70) {
        testEntitySetWithFilter();
    } else if (scenario < 0.80) {
        testSingleEntity();
    } else if (scenario < 0.90) {
        testEntitySetWithOrderBy();
    } else {
        testEntitySetWithTopSkip();
    }

    // Small delay between requests
    sleep(0.1);
}

/**
 * Test: Service Document
 * Exercises: URI parsing, JSON serialization, service metadata
 */
function testServiceDocument() {
    const res = http.get(SERVICE_URL, { headers });
    serviceDocTrend.add(res.timings.duration);

    const success = check(res, {
        'service document status is 200': (r) => r.status === 200,
        'service document has context': (r) => r.body.includes('@odata.context'),
    });
    errorRate.add(!success);
}

/**
 * Test: Metadata Document (JSON)
 * Exercises: EDM serialization, schema processing
 */
function testMetadataJson() {
    const res = http.get(`${SERVICE_URL}/$metadata`, {
        headers: { ...headers, 'Accept': 'application/json' }
    });
    metadataTrend.add(res.timings.duration);

    const success = check(res, {
        'metadata status is 200': (r) => r.status === 200,
    });
    errorRate.add(!success);
}

/**
 * Test: Metadata Document (XML)
 * Exercises: XML serialization, EDMX generation
 */
function testMetadataXml() {
    const res = http.get(`${SERVICE_URL}/$metadata`, { headers: xmlHeaders });
    metadataTrend.add(res.timings.duration);

    const success = check(res, {
        'metadata XML status is 200': (r) => r.status === 200,
        'metadata is EDMX': (r) => r.body.includes('edmx:Edmx'),
    });
    errorRate.add(!success);
}

/**
 * Test: Entity Set Read
 * Exercises: Entity collection serialization, data provider
 */
function testEntitySetRead() {
    const entitySets = ['ESAllPrim', 'ESCompAllPrim', 'ESTwoPrim', 'ESMixPrimCollComp'];
    const entitySet = entitySets[Math.floor(Math.random() * entitySets.length)];

    const res = http.get(`${SERVICE_URL}/${entitySet}`, { headers });
    entitySetTrend.add(res.timings.duration);

    const success = check(res, {
        'entity set status is 200': (r) => r.status === 200,
        'entity set has value': (r) => r.body.includes('"value"'),
    });
    errorRate.add(!success);
}

/**
 * Test: Entity Set with $select
 * Exercises: Select option parsing, projection
 */
function testEntitySetWithSelect() {
    const res = http.get(`${SERVICE_URL}/ESAllPrim?$select=PropertyInt16,PropertyString`, { headers });
    queryTrend.add(res.timings.duration);

    const success = check(res, {
        'select query status is 200': (r) => r.status === 200,
    });
    errorRate.add(!success);
}

/**
 * Test: Entity Set with $filter
 * Exercises: Filter expression parsing, evaluation
 */
function testEntitySetWithFilter() {
    const filters = [
        "PropertyInt16 eq 1",
        "PropertyInt16 gt 0",
        "PropertyString ne null",
        "PropertyInt16 ge 0 and PropertyInt16 le 100",
    ];
    const filter = filters[Math.floor(Math.random() * filters.length)];

    const res = http.get(`${SERVICE_URL}/ESAllPrim?$filter=${encodeURIComponent(filter)}`, { headers });
    queryTrend.add(res.timings.duration);

    const success = check(res, {
        'filter query status is 200': (r) => r.status === 200,
    });
    errorRate.add(!success);
}

/**
 * Test: Single Entity Read
 * Exercises: Key predicate parsing, single entity serialization
 */
function testSingleEntity() {
    const res = http.get(`${SERVICE_URL}/ESAllPrim(32767)`, { headers });
    singleEntityTrend.add(res.timings.duration);

    const success = check(res, {
        'single entity status is 200': (r) => r.status === 200,
        'single entity has id': (r) => r.body.includes('@odata.id'),
    });
    errorRate.add(!success);
}

/**
 * Test: Entity Set with $orderby
 * Exercises: OrderBy parsing, sorting
 */
function testEntitySetWithOrderBy() {
    const res = http.get(`${SERVICE_URL}/ESAllPrim?$orderby=PropertyInt16 desc`, { headers });
    queryTrend.add(res.timings.duration);

    const success = check(res, {
        'orderby query status is 200': (r) => r.status === 200,
    });
    errorRate.add(!success);
}

/**
 * Test: Entity Set with $top and $skip
 * Exercises: Pagination logic
 */
function testEntitySetWithTopSkip() {
    const res = http.get(`${SERVICE_URL}/ESAllPrim?$top=5&$skip=1`, { headers });
    queryTrend.add(res.timings.duration);

    const success = check(res, {
        'pagination query status is 200': (r) => r.status === 200,
    });
    errorRate.add(!success);
}

/**
 * Setup function - runs once before the test
 */
export function setup() {
    // Verify service is available
    const res = http.get(SERVICE_URL, { headers });
    if (res.status !== 200) {
        throw new Error(`Service not available at ${SERVICE_URL}: ${res.status}`);
    }
    console.log(`OData service available at ${SERVICE_URL}`);
    return { startTime: new Date().toISOString() };
}

/**
 * Teardown function - runs once after the test
 */
export function teardown(data) {
    console.log(`Test completed. Started at: ${data.startTime}`);
}
