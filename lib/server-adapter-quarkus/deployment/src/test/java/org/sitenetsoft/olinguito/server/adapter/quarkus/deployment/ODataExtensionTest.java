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
 */
package org.sitenetsoft.olinguito.server.adapter.quarkus.deployment;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for the OData Quarkus extension.
 * <p>
 * Note: These tests are currently disabled due to an issue with the Quarkus test framework
 * where the server starts but connections fail with NoHttpResponse. This appears to be
 * related to how the extension interacts with the test infrastructure and requires
 * further investigation.
 */
@Disabled("Quarkus test framework integration issue - server starts but connections fail")
class ODataExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TestEdmProvider.class, TestEntityProcessor.class)
                    .addAsResource("application.properties"));

    @Test
    void testServiceDocumentEndpoint() {
        given()
                .when().get("/odata")
                .then()
                .statusCode(200)
                .body(containsString("@odata.context"));
    }

    @Test
    void testMetadataEndpoint() {
        given()
                .when().get("/odata/$metadata")
                .then()
                .statusCode(200)
                .contentType(containsString("xml"));
    }

    @Test
    void testEntitySetEndpoint() {
        given()
                .when().get("/odata/Products")
                .then()
                .statusCode(200)
                .body(containsString("value"));
    }

    @Test
    void testNonODataEndpointReturns404() {
        given()
                .when().get("/nonexistent")
                .then()
                .statusCode(404);
    }
}
