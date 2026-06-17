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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - String-literal escaping now done by the library (ENUM_VALUE regex tightened)
 */
package org.sitenetsoft.olinguito.client.core.uri;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExampleUriGeneratorTest {
    /**
     * This test demonstrates the normal behavior of encapsulating a string with single quotes
     * <p>
     * The test succeeds
     */
    @Test
    void testHappyPath() {
        String uri = ExampleUriGenerator.filterPersonByName("henk");

        // Expected: https://example.com/Person?$filter=(name eq 'henk')
        assertEquals("https://example.com/Person?%24filter=%28name%20eq%20'henk'%29", uri);
    }


    /**
     * A string value that tries to break out of its literal must be safely quoted and have its
     * embedded single quotes doubled, so it is treated as a single literal and cannot circumvent
     * the filter. The library now does this itself (the caller no longer escapes manually): the
     * value is wrapped in single quotes and the {@code ENUM_VALUE} regex no longer mistakes an
     * unprefixed quoted-looking string for an enum literal.
     */
    @Test
    void testODataInjection() {
        String uri = ExampleUriGenerator.filterPersonByName("' or name ne '");

        // The whole injection attempt becomes one quoted literal: (name eq ''' or name ne ''')
        assertEquals("https://example.com/Person?%24filter=%28name%20eq%20'''%20or%20name%20ne%20'''%29", uri);
    }

    static final class ExampleUriGenerator {
        private static final ODataClient client = ODataClientFactory.getClient();

        private ExampleUriGenerator() {
        }

        static String filterPersonByName(String name) {
            // The library escapes string literals (doubles embedded single quotes) itself.
            String filter = client.getFilterFactory() //
                .eq("name", name) //
                .build();

            return client.newURIBuilder("https://example.com/") //
                .appendEntitySetSegment("Person") //
                .filter(filter) //
                .build().toString();
        }
    }

}
