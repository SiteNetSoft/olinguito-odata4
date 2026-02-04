/*
 * Copyright 2026 SiteNetSoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sitenetsoft.olinguito.server.tecsvc.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * JVM-mode integration tests for the Technical OData Service.
 * These tests run against the Quarkus application in JVM mode.
 */
@QuarkusTest
class TechnicalServiceTest {

    private static final String BASE_PATH = "/odata-server-tecsvc/odata.svc";

    @Test
    void testServiceDocumentEndpoint() {
        given()
            .when().get(BASE_PATH)
            .then()
            .statusCode(200)
            .body(containsString("@odata.context"));
    }

    @Test
    void testMetadataEndpoint() {
        given()
            .when().get(BASE_PATH + "/$metadata")
            .then()
            .statusCode(200)
            .contentType(containsString("xml"));
    }

    @Test
    void testEntitySetEndpoint() {
        given()
            .when().get(BASE_PATH + "/ESAllPrim")
            .then()
            .statusCode(200)
            .body(containsString("value"));
    }

    @Test
    void testEntitySetWithSelectEndpoint() {
        given()
            .when().get(BASE_PATH + "/ESAllPrim?$select=PropertyInt16")
            .then()
            .statusCode(200)
            .body(containsString("PropertyInt16"));
    }

    @Test
    void testEntitySetWithFilterEndpoint() {
        given()
            .when().get(BASE_PATH + "/ESAllPrim?$filter=PropertyInt16 eq 32767")
            .then()
            .statusCode(200);
    }

    @Test
    void testSingleEntityEndpoint() {
        given()
            .when().get(BASE_PATH + "/ESAllPrim(32767)")
            .then()
            .statusCode(200)
            .body(containsString("PropertyInt16"));
    }

    @Test
    void testNonExistentEntityReturns404() {
        given()
            .when().get(BASE_PATH + "/NonExistentEntitySet")
            .then()
            .statusCode(404);
    }
}
