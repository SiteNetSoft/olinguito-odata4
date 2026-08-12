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
 * Copyright 2026 SiteNetSoft - New file: end-to-end coverage for OpenType (open, dynamic-property) entities
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 3: direct dynamic-property GET fit coverage
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.sitenetsoft.olinguito.client.api.communication.ODataClientErrorException;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.UpdateType;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntityRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataPropertyRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityCreateResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityUpdateResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntitySet;
import org.sitenetsoft.olinguito.client.api.domain.ClientObjectFactory;
import org.sitenetsoft.olinguito.client.api.domain.ClientProperty;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.junit.Test;

/**
 * End-to-end coverage for OData open types (entities that accept undeclared, "dynamic" properties):
 * reading, filtering, ordering and selecting a dynamic property, plus creating an entity that carries one.
 *
 * Dynamic-property support is currently JSON-only (the XML instance serializer only ever writes the
 * EDM-declared property set), so every test here is skipped for the XML parameterization via
 * {@code assumeTrue(isJson())}, mirroring the existing IEEE754Compatible-style JSON-only tests
 * in {@link BasicITCase}.
 */
public class OpenTypeITCase extends AbstractParamTecSvcITCase {

  private static final String ES_OPEN = "ESOpen";
  private static final String ET_OPEN = "ETOpen";
  private static final String ASSUME_JSON_REASON =
      "Open-type dynamic properties are only emitted/parsed for JSON payloads.";

  @Test
  public void readEntityWithDynamicProperties() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final ODataEntityRequest<ClientEntity> request = getClient().getRetrieveRequestFactory()
        .getEntityRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientEntity> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final ClientEntity entity = response.getBody();
    assertNotNull(entity);

    final ClientProperty dynamicString = entity.getProperty("DynamicString");
    assertNotNull(dynamicString);
    assertEquals("dynamic", dynamicString.getPrimitiveValue().toValue());
  }

  @Test
  public void filterOnDynamicProperty() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_OPEN)
        .filter("DynamicInt gt 10")
        .build();
    final ODataEntitySetRequest<ClientEntitySet> request =
        getClient().getRetrieveRequestFactory().getEntitySetRequest(uri);
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientEntitySet> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final List<ClientEntity> entities = response.getBody().getEntities();
    // Entity 1 has DynamicInt=42 (matches), entity 2 has DynamicInt=7 (does not match),
    // entity 3 has no DynamicInt at all (absent property does not match "gt 10").
    assertEquals(1, entities.size());
    assertShortOrInt(1, entities.get(0).getProperty("PropertyInt16").getPrimitiveValue().toValue());
  }

  @Test
  public void filterOnNestedDynamicProperty() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // PropertyComp (CTOpen, itself an open complex type) is seeded ONLY on entity 1; entities 2
    // and 3 have no PropertyComp property at all. A collection filter visits every entity, so
    // this single request exercises both the nested dynamic-property lookup (entity 1) and the
    // "parent complex property entirely absent" path (entities 2 and 3) in visitMember.
    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_OPEN)
        .filter("PropertyComp/CompDynamic eq 'dynamic comp value'")
        .build();
    final ODataEntitySetRequest<ClientEntitySet> request =
        getClient().getRetrieveRequestFactory().getEntitySetRequest(uri);
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientEntitySet> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final List<ClientEntity> entities = response.getBody().getEntities();
    assertEquals(1, entities.size());
    assertShortOrInt(1, entities.get(0).getProperty("PropertyInt16").getPrimitiveValue().toValue());
  }

  @Test
  public void orderByDynamicProperty() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_OPEN)
        .orderBy("DynamicInt desc")
        .build();
    final ODataEntitySetRequest<ClientEntitySet> request =
        getClient().getRetrieveRequestFactory().getEntitySetRequest(uri);
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientEntitySet> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final List<ClientEntity> entities = response.getBody().getEntities();
    assertEquals(3, entities.size());
    // DynamicInt: entity 1 = 42, entity 2 = 7, entity 3 = absent. Per OrderByHandler's
    // null-ordering rule (OrderByHandler#applyOrderByOptionInternal), a null/absent operand
    // always compares as "less than" any present value BEFORE the direction flip is applied
    // (see the ternary at OrderByHandler:81) - so nulls sort FIRST for ascending order, and
    // (after the descending flip) LAST for descending order, as used here. That places the two
    // present values first, highest to lowest, and the absent one (entity 3) last.
    assertShortOrInt(1, entities.get(0).getProperty("PropertyInt16").getPrimitiveValue().toValue());
    assertShortOrInt(2, entities.get(1).getProperty("PropertyInt16").getPrimitiveValue().toValue());
    assertShortOrInt(3, entities.get(2).getProperty("PropertyInt16").getPrimitiveValue().toValue());
  }

  @Test
  public void selectDynamicProperty() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final ODataEntityRequest<ClientEntity> request = getClient().getRetrieveRequestFactory()
        .getEntityRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .select("DynamicString")
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientEntity> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final ClientEntity entity = response.getBody();
    assertNotNull(entity);

    final ClientProperty dynamicString = entity.getProperty("DynamicString");
    assertNotNull(dynamicString);
    assertEquals("dynamic", dynamicString.getPrimitiveValue().toValue());

    assertNull(entity.getProperty("PropertyString"));
  }

  @Test
  public void createEntityWithDynamicProperty() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final ClientObjectFactory factory = getFactory();
    final ClientEntity entity = factory.newEntity(new FullQualifiedName(SERVICE_NAMESPACE, ET_OPEN));
    entity.getProperties().add(factory.newPrimitiveProperty("PropertyInt16",
        factory.newPrimitiveValueBuilder().buildInt16((short) 100)));
    entity.getProperties().add(factory.newPrimitiveProperty("PropertyString",
        factory.newPrimitiveValueBuilder().buildString("created open type entity")));
    // Undeclared (dynamic) property, only legal because ETOpen is an open type.
    entity.getProperties().add(factory.newPrimitiveProperty("Brand",
        factory.newPrimitiveValueBuilder().buildString("new")));

    final URI uri = getClient().newURIBuilder(SERVICE_URI).appendEntitySetSegment(ES_OPEN).build();
    final ODataEntityCreateRequest<ClientEntity> request =
        getClient().getCUDRequestFactory().getEntityCreateRequest(uri, entity);
    // Intentionally not sharing the class-wide session cookie: this mutates ESOpen, and the
    // read-only tests above assume the pristine 3-entity seed data. Sending no cookie makes the
    // server start a fresh, isolated session (see TechnicalServlet#service), so the created
    // entity is invisible to the other tests regardless of JUnit execution order.

    final ODataEntityCreateResponse<ClientEntity> response = request.execute();
    assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatusCode());

    final ClientEntity created = response.getBody();
    assertNotNull(created);

    final ClientProperty brand = created.getProperty("Brand");
    assertNotNull(brand);
    assertEquals("new", brand.getPrimitiveValue().toValue());
  }

  @Test
  public void putReplacementOmittingDynamicPropertyDropsIt() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // ESOpen(1) is seeded with DynamicString="dynamic" and DynamicInt=42. A full PUT
    // (UpdateType.REPLACE) that omits both must wholesale-replace the stored entity, including
    // its dynamic properties, not just patch over the declared ones - both must be gone
    // afterwards.
    //
    // Deliberately targeting entity 1, not entity 2: entities 2 and 3 were seeded without a
    // PropertyComp property object at all (unlike entity 1, or any entity created via
    // DataProvider#create(), which always gets one - even if empty/null - for every EDM-declared
    // property). DataProvider#updateProperty() requires the entity's own current property slot
    // to be non-null and throws "Cannot update type of the entity" (HTTP 400) otherwise, so a PUT
    // to entity 2 or 3 fails for a reason unrelated to dynamic properties. That is a pre-existing
    // seed-data gap outside this fix's scope; entity 1 has no such gap and exercises the same
    // patch-flag fix just as well (with two dynamic properties to drop instead of one).
    final ClientObjectFactory factory = getFactory();
    final ClientEntity replacement = factory.newEntity(new FullQualifiedName(SERVICE_NAMESPACE, ET_OPEN));
    replacement.getProperties().add(factory.newPrimitiveProperty("PropertyInt16",
        factory.newPrimitiveValueBuilder().buildInt16((short) 1)));
    replacement.getProperties().add(factory.newPrimitiveProperty("PropertyString",
        factory.newPrimitiveValueBuilder().buildString("replaced open type 1")));

    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_OPEN)
        .appendKeySegment(1)
        .build();
    final ODataEntityUpdateRequest<ClientEntity> updateRequest = getClient().getCUDRequestFactory()
        .getEntityUpdateRequest(uri, UpdateType.REPLACE, replacement);
    // Intentionally not sharing the class-wide session cookie (same rationale as
    // createEntityWithDynamicProperty above): this mutates the seeded ESOpen(1), and the
    // read-only tests in this class assume pristine seed data. Sending no cookie makes the
    // server start a fresh, isolated session; the isolated session's own cookie is then reused
    // only for the follow-up re-GET below, so the mutation stays invisible to the other tests
    // regardless of JUnit execution order.
    final ODataEntityUpdateResponse<ClientEntity> updateResponse = updateRequest.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), updateResponse.getStatusCode());
    final String isolatedCookie = updateResponse.getHeader(HttpHeader.SET_COOKIE).iterator().next();

    final ODataEntityRequest<ClientEntity> getRequest =
        getClient().getRetrieveRequestFactory().getEntityRequest(uri);
    getRequest.addCustomHeader(HttpHeader.COOKIE, isolatedCookie);
    final ODataRetrieveResponse<ClientEntity> getResponse = getRequest.execute();
    assertEquals(HttpStatusCode.OK.getStatusCode(), getResponse.getStatusCode());

    final ClientEntity reread = getResponse.getBody();
    assertNotNull(reread);
    assertNull(reread.getProperty("DynamicInt"));
    assertNull(reread.getProperty("DynamicString"));
  }

  @Test
  public void readDynamicPropertyDirectly() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .appendPropertySegment("DynamicString")
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientProperty> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final ClientProperty property = response.getBody();
    assertNotNull(property);
    assertNotNull(property.getPrimitiveValue());
    assertEquals("dynamic", property.getPrimitiveValue().toValue());
    assertEquals("Edm.String",
        property.getPrimitiveValue().getType().getFullQualifiedName().getFullQualifiedNameAsString());
  }

  @Test
  public void readAbsentDynamicPropertyReturns404() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Entity 3 is seeded with no dynamic properties at all.
    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(3)
            .appendPropertySegment("DynamicString")
            .build());
    setCookieHeader(request);

    try {
      request.execute();
      fail("Expected exception not thrown!");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getStatusCode());
    }
  }

  @Test
  public void readDynamicInt64Directly() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .appendPropertySegment("DynamicInt")
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientProperty> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final ClientProperty property = response.getBody();
    assertNotNull(property);
    assertNotNull(property.getPrimitiveValue());
    // Deliberately not asserting getPrimitiveValue().getType() here: OData JSON minimal metadata
    // carries no @odata.type annotation for a raw primitive-property document (see
    // ODataJsonSerializer#primitive), and a dynamic property has no CSDL declaration the client
    // could otherwise resolve the type from either, so the client SDK falls back to its own
    // bare-JSON-number heuristic (Edm.Int32) regardless of what the server actually resolved and
    // serialized the value as. See readDynamicIntSerializesAsNumberNotString below for a
    // wire-level pin of the server-side Edm.Int64 resolution instead.
    assertShortOrInt(42, property.getPrimitiveValue().toValue());
  }

  @Test
  public void readDynamicIntSerializesAsNumberNotString() throws Exception {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .appendPropertySegment("DynamicInt")
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientProperty> response = request.execute();
    saveCookieHeader(response);

    // Pins the server-side type resolution (DynamicPropertyTypeResolver resolves the stored Long
    // 42L to Edm.Int64) at the wire level: a bare JSON number, not a quoted string - which is what
    // the Edm.String fallback would have produced instead.
    final String actualResult = new String(response.getRawResponse().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(actualResult.endsWith("\"value\":42}"));
  }

  @Test
  public void readDynamicPropertyOnClosedTypeStillRejected() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Pin: ETTwoPrim is a closed type, so an undeclared segment must still be rejected at
    // URI-parse time (PROPERTY_NOT_IN_TYPE), regardless of dynamic-property GET now being served
    // for open types.
    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment("ESTwoPrim")
            .appendKeySegment(32766)
            .appendPropertySegment("Nope")
            .build());
    setCookieHeader(request);

    try {
      request.execute();
      fail("Expected exception not thrown!");
    } catch (final ODataClientErrorException e) {
      assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), e.getStatusCode());
    }
  }
}
