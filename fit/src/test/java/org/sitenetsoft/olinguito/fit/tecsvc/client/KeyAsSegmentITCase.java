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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 4: client key-as-segment round trip
 * (OData 4.01, Part 2: URL Conventions, section 4.3.6)
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 4 fix round 1: exact seeded count assertion
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntityRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataValueRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientPrimitiveValue;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

/**
 * Round trip of the client URI builder's key-as-segment mode against the technical service served
 * with the key-as-segment convention switched on.
 */
public class KeyAsSegmentITCase extends AbstractTecSvcITCase {

  private static final String KAS_SERVICE_URI = TecSvcConst.KEY_AS_SEGMENT_BASE_URI + '/';

  private static final String ES_ALL_PRIM = "ESAllPrim";
  private static final String ES_TWO_KEY_NAV = "ESTwoKeyNav";
  private static final String PROPERTY_INT16 = "PropertyInt16";
  private static final String PROPERTY_STRING = "PropertyString";

  @Override
  protected ContentType getContentType() {
    return ContentType.APPLICATION_JSON;
  }

  private ODataClient getKeyAsSegmentClient() {
    final ODataClient client = getClient();
    client.getConfiguration().setKeyAsSegment(true);
    return client;
  }

  private ClientEntity read(final ODataClient client, final URI uri) {
    final ODataEntityRequest<ClientEntity> request = client.getRetrieveRequestFactory().getEntityRequest(uri);
    setCookieHeader(request);
    final ODataRetrieveResponse<ClientEntity> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    return response.getBody();
  }

  @Test
  public void singleKeySegment() {
    final ODataClient client = getKeyAsSegmentClient();
    final URI uri = client.newURIBuilder(KAS_SERVICE_URI)
        .appendEntitySetSegment(ES_ALL_PRIM).appendKeySegment(32767).build();
    assertEquals(KAS_SERVICE_URI + ES_ALL_PRIM + "/32767", uri.toASCIIString());

    final ClientEntity entity = read(client, uri);
    assertNotNull(entity);
    assertEquals(32767, ((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
  }

  @Test
  public void multiPartKeySegments() {
    final ODataClient client = getKeyAsSegmentClient();
    // The key property names are not part of a key-as-segment URL, so the values must be passed in
    // the order the key properties are declared in the metadata.
    final Map<String, Object> key = new LinkedHashMap<>();
    key.put(PROPERTY_INT16, 1);
    key.put(PROPERTY_STRING, "1");
    final URI uri = client.newURIBuilder(KAS_SERVICE_URI)
        .appendEntitySetSegment(ES_TWO_KEY_NAV).appendKeySegment(key).build();
    assertEquals(KAS_SERVICE_URI + ES_TWO_KEY_NAV + "/1/1", uri.toASCIIString());

    final ClientEntity entity = read(client, uri);
    assertNotNull(entity);
    assertEquals(1, ((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
    assertEquals("1", entity.getProperty(PROPERTY_STRING).getPrimitiveValue().toValue());
  }

  @Test
  public void countSegmentAfterKeySegmentStillWorks() {
    final ODataClient client = getKeyAsSegmentClient();
    final URI uri = client.newURIBuilder(KAS_SERVICE_URI).appendEntitySetSegment(ES_TWO_KEY_NAV)
        .appendKeySegment(1).appendKeySegment("1").appendNavigationSegment("NavPropertyETTwoKeyNavMany")
        .appendCountSegment().build();
    assertEquals(KAS_SERVICE_URI + ES_TWO_KEY_NAV + "/1/1/NavPropertyETTwoKeyNavMany/$count",
        uri.toASCIIString());

    final ODataValueRequest request = client.getRetrieveRequestFactory().getValueRequest(uri);
    setCookieHeader(request);
    final ODataRetrieveResponse<ClientPrimitiveValue> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    // DataCreator#linkESTwoKeyNav links ESTwoKeyNav(1,'1') to two ESTwoKeyNav entities.
    assertEquals("2", response.getBody().toValue().toString());
  }

  @Test
  public void nonNumericKeySegmentIsRejected() {
    final ODataClient client = getKeyAsSegmentClient();
    final URI uri = client.newURIBuilder(KAS_SERVICE_URI)
        .appendEntitySetSegment(ES_ALL_PRIM).appendKeySegment("abc").build();
    assertEquals(KAS_SERVICE_URI + ES_ALL_PRIM + "/abc", uri.toASCIIString());

    try {
      client.getRetrieveRequestFactory().getEntityRequest(uri).execute();
      fail("Expected exception not thrown!");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
    }
  }
}
