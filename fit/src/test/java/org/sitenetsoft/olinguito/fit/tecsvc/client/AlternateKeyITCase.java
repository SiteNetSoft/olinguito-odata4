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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 7: alternate-key CRUD round trips
 * (OData 4.01, Part 1: Protocol, section 4.1.1)
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 fix wave: bound action through an alternate key
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.request.ODataRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataDeleteRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.UpdateType;
import org.sitenetsoft.olinguito.client.api.communication.request.invoke.ODataInvokeRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntityRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataRawRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataDeleteResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityCreateResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityUpdateResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataInvokeResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRawResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientValue;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.junit.Test;

/**
 * Addressing entities through the alternate keys the technical service declares with
 * {@code Core.AlternateKeys}: {@code ESAllPrim} has the set-level groups
 * {@code (PropertyString)} and {@code (StringPart,PropertyGuid)}, {@code ETKeyNav} the
 * type-level group {@code (PropertyString)}.
 */
public class AlternateKeyITCase extends AbstractTecSvcITCase {

  private static final String ES_ALL_PRIM = "ESAllPrim";
  private static final String ES_KEY_NAV = "ESKeyNav";
  private static final String ES_TWO_PRIM = "ESTwoPrim";
  private static final String PROPERTY_INT16 = "PropertyInt16";
  private static final String PROPERTY_INT32 = "PropertyInt32";
  private static final String PROPERTY_STRING = "PropertyString";
  private static final String PROPERTY_GUID = "PropertyGuid";
  private static final String EMPLOYEE_STRING = "Employee1@company.example";
  private static final ContentType CONTENT_TYPE_JSON_FULL_METADATA =
      ContentType.create(ContentType.JSON, ContentType.PARAMETER_ODATA_METADATA,
          ContentType.VALUE_ODATA_METADATA_FULL);

  @Override
  protected ContentType getContentType() {
    return ContentType.JSON;
  }

  @Test
  public void readBySinglePartAlternateKey() throws Exception {
    final URI uri = alternateKeyUri(ES_ALL_PRIM, PROPERTY_STRING, EMPLOYEE_STRING);
    final ClientEntity entity = readEntity(uri, null);
    assertEquals(10, ((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
    assertEquals(EMPLOYEE_STRING, entity.getProperty(PROPERTY_STRING).getPrimitiveValue().toValue());

    // The canonical URL of the entity always uses the primary key, never the alternate key of the request.
    // Requested with full metadata, so that the server writes the id into the payload.
    final ODataRawRequest rawRequest = getClient().getRetrieveRequestFactory().getRawRequest(uri);
    rawRequest.setFormat(CONTENT_TYPE_JSON_FULL_METADATA.toContentTypeString());
    final ODataRawResponse rawResponse = rawRequest.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), rawResponse.getStatusCode());
    final String content = readContent(rawResponse.getRawResponse());
    assertThat(content, containsString("\"@odata.id\":\"ESAllPrim(10)\""));
    assertThat(content, containsString(
        "\"NavPropertyETTwoPrimOne@odata.navigationLink\":\"ESAllPrim(10)/NavPropertyETTwoPrimOne\""));
  }

  @Test
  public void readByAliasedMultiPartAlternateKey() {
    final Map<String, Object> key = new LinkedHashMap<>();
    key.put("StringPart", "First Resource - positive values");
    key.put(PROPERTY_GUID, UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"));
    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_ALL_PRIM).appendKeySegment(key).build();

    final ClientEntity entity = readEntity(uri, null);
    assertEquals(32767, ((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
  }

  @Test
  public void readByTypeLevelAlternateKey() {
    final ClientEntity entity =
        readEntity(alternateKeyUri(ES_KEY_NAV, PROPERTY_STRING, "I am String Property 2"), null);
    assertEquals(2, ((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
  }

  @Test
  public void unknownAlternateKeyValueIs404() {
    final ODataEntityRequest<ClientEntity> request = getClient().getRetrieveRequestFactory()
        .getEntityRequest(alternateKeyUri(ES_ALL_PRIM, PROPERTY_STRING, "nope"));
    try {
      request.execute();
      fail("Expected exception not thrown!");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getStatusCode());
    }
  }

  @Test
  public void partialAlternateKeyIs400() {
    // "StringPart" alone matches no declared alternate-key group, so it is not a key at all.
    final ODataEntityRequest<ClientEntity> request = getClient().getRetrieveRequestFactory()
        .getEntityRequest(alternateKeyUri(ES_ALL_PRIM, "StringPart", "x"));
    try {
      request.execute();
      fail("Expected exception not thrown!");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
    }
  }

  @Test
  public void patchThroughAlternateKey() {
    final ClientEntity patchEntity = getFactory().newEntity(
        new FullQualifiedName(SERVICE_NAMESPACE, "ETAllPrim"));
    patchEntity.getProperties().add(getFactory().newPrimitiveProperty(PROPERTY_INT32,
        getFactory().newPrimitiveValueBuilder().buildInt32(4711)));

    final ODataEntityUpdateRequest<ClientEntity> request = getClient().getCUDRequestFactory()
        .getEntityUpdateRequest(alternateKeyUri(ES_ALL_PRIM, PROPERTY_STRING, EMPLOYEE_STRING),
            UpdateType.PATCH, patchEntity);
    final ODataEntityUpdateResponse<ClientEntity> response = request.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    // Check the change in the same session, addressing the entity by its primary key.
    final URI primaryKeyUri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_ALL_PRIM).appendKeySegment(10).build();
    final ClientEntity entity = readEntity(primaryKeyUri, sessionCookie(response));
    assertEquals(4711, ((Number) entity.getProperty(PROPERTY_INT32).getPrimitiveValue().toValue()).intValue());
  }

  @Test
  public void deleteThroughAlternateKey() {
    final ODataDeleteRequest request = getClient().getCUDRequestFactory()
        .getDeleteRequest(alternateKeyUri(ES_ALL_PRIM, PROPERTY_STRING, "Second Resource - negative values"));
    final ODataDeleteResponse response = request.execute();
    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());

    // Check in the same session that the entity is gone, addressing it by its primary key.
    final URI primaryKeyUri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_ALL_PRIM).appendKeySegment(-32768).build();
    final ODataEntityRequest<ClientEntity> entityRequest =
        getClient().getRetrieveRequestFactory().getEntityRequest(primaryKeyUri);
    addCookie(entityRequest, sessionCookie(response));
    try {
      entityRequest.execute();
      fail("Expected exception not thrown!");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getStatusCode());
    }
  }

  @Test
  public void propertyContextUrlUsesPrimaryKey() throws Exception {
    // Recorded decision pin: TechnicalPrimitiveComplexProcessor builds the key path of a
    // property-level context URL from the entity (primary key), not from the request predicate.
    final URI uri = URI.create(alternateKeyUri(ES_ALL_PRIM, PROPERTY_STRING, EMPLOYEE_STRING)
        + "/" + PROPERTY_INT16);
    final ODataRawRequest request = getClient().getRetrieveRequestFactory().getRawRequest(uri);
    request.setFormat(ContentType.JSON.toContentTypeString());
    final ODataRawResponse response = request.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final String content = readContent(response.getRawResponse());
    assertThat(content, containsString("$metadata#ESAllPrim(10)/PropertyInt16"));
    assertThat(content, containsString("\"value\":10"));
  }

  @Test
  public void createdEntityLocationUsesPrimaryKey() {
    final String createdString = "Created through the alternate-key test";
    final ODataClient client = getClient();
    final ClientEntity newEntity = getFactory().newEntity(
        new FullQualifiedName(SERVICE_NAMESPACE, "ETAllPrim"));
    newEntity.getProperties().add(getFactory().newPrimitiveProperty(PROPERTY_STRING,
        getFactory().newPrimitiveValueBuilder().buildString(createdString)));
    newEntity.addLink(getFactory().newEntityNavigationLink("NavPropertyETTwoPrimOne",
        client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_TWO_PRIM).appendKeySegment(32766).build()));

    final ODataEntityCreateRequest<ClientEntity> createRequest = client.getCUDRequestFactory()
        .getEntityCreateRequest(client.newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_ALL_PRIM).build(),
            newEntity);
    final ODataEntityCreateResponse<ClientEntity> createResponse = createRequest.execute();
    assertEquals(HttpStatusCode.CREATED.getStatusCode(), createResponse.getStatusCode());
    assertEquals(SERVICE_URI + ES_ALL_PRIM + "(1)",
        createResponse.getHeader(HttpHeader.LOCATION).iterator().next());

    // The created entity is addressable through its alternate key in the same session.
    final ClientEntity entity = readEntity(alternateKeyUri(ES_ALL_PRIM, PROPERTY_STRING, createdString),
        sessionCookie(createResponse));
    assertEquals(1, ((Number) entity.getProperty(PROPERTY_INT16).getPrimitiveValue().toValue()).intValue());
  }

  @Test
  public void boundActionThroughAlternateKey() {
    // ETKeyNav's type-level alternate key addresses the binding parameter of a bound action.
    final URI uri = getClient().newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_KEY_NAV)
        .appendKeySegment(Map.of(PROPERTY_STRING, (Object) "I am String Property 2"))
        .appendActionCallSegment(SERVICE_NAMESPACE + ".BA_RTETTwoKeyNav").build();
    final ODataInvokeRequest<ClientEntity> request = getClient().getInvokeRequestFactory()
        .getActionInvokeRequest(uri, ClientEntity.class, Collections.<String, ClientValue> emptyMap());
    final ODataInvokeResponse<ClientEntity> response = request.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    assertNotNull(response.getBody());
  }

  private URI alternateKeyUri(final String entitySet, final String keyName, final String keyValue) {
    return getClient().newURIBuilder(SERVICE_URI).appendEntitySetSegment(entitySet)
        .appendKeySegment(Map.of(keyName, (Object) keyValue)).build();
  }

  private ClientEntity readEntity(final URI uri, final String cookie) {
    final ODataEntityRequest<ClientEntity> request = getClient().getRetrieveRequestFactory().getEntityRequest(uri);
    addCookie(request, cookie);
    final ODataRetrieveResponse<ClientEntity> response = request.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
    final ClientEntity entity = response.getBody();
    assertNotNull(entity);
    return entity;
  }

  private void addCookie(final ODataRequest request, final String cookie) {
    if (cookie != null) {
      request.addCustomHeader(HttpHeader.COOKIE, cookie);
    }
  }

  private String sessionCookie(final ODataResponse response) {
    final Collection<String> header = response.getHeader(HttpHeader.SET_COOKIE);
    assertNotNull("no session cookie in the response", header);
    assertTrue("no session cookie in the response", !header.isEmpty());
    return header.iterator().next();
  }

  private String readContent(final InputStream stream) throws Exception {
    try (InputStream input = stream) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
