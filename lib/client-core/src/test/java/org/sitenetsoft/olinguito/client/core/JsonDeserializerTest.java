/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.sitenetsoft.olinguito.client.core;

import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientLink;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.core.serialization.ClientODataDeserializerImpl;
import org.sitenetsoft.olinguito.client.core.serialization.ODataBinderImpl;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonDeserializerTest {


    /**
     * Without the attached fix this test with the corresponding resource json will throw
     * a {@link java.lang.IllegalArgumentException} with the message: 'Cannot build a primitive value for Stream'
     * this is because the <streamproptitle>@odata.type key is incorrectly filtered by the previous implementation
     */
    @Test
    public void testDeserializationStreamProperties() throws ODataDeserializerException {
        try (InputStream inputStream = getClass().getResourceAsStream("Employee.json")) {
            ClientODataDeserializerImpl clientODataDeserializer =
                new ClientODataDeserializerImpl(false, ContentType.JSON_FULL_METADATA);
            ResWrap<Entity> entity = clientODataDeserializer.toEntity(inputStream);
            ODataBinderImpl oDataBinder = new ODataBinderImpl(ODataClientFactory.getClient());
            ClientEntity oDataEntity = oDataBinder.getODataEntity(entity);
            List<ClientLink> mediaEditLinks = oDataEntity.getMediaEditLinks();
            assertEquals(2, mediaEditLinks.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
