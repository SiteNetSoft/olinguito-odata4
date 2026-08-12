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
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 3 fix round 1: nested dynamic-property GET, 204
 * Content-Type, and PUT/DELETE-still-501 regression coverage
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 4: replace the PUT/PATCH/DELETE-still-501 pins
 * with real dynamic-property write/delete coverage
 */
package org.sitenetsoft.olinguito.fit.tecsvc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

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

  @Test
  public void readNestedDynamicPropertyDirectly() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // PropertyComp (CTOpen, itself an open complex type) is seeded on entity 1 with a dynamic
    // CompDynamic property. Addressing it directly (ESOpen(1)/PropertyComp/CompDynamic) parses to
    // [EntitySet, ComplexProperty(PropertyComp), DynamicProperty(CompDynamic)] - the dynamic-property
    // lookup must navigate through the intervening complex-property segment, not just the entity's
    // own top-level property list.
    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .appendPropertySegment("PropertyComp")
            .appendPropertySegment("CompDynamic")
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientProperty> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());

    final ClientProperty property = response.getBody();
    assertNotNull(property);
    assertNotNull(property.getPrimitiveValue());
    assertEquals("dynamic comp value", property.getPrimitiveValue().toValue());
  }

  @Test
  public void readNestedAbsentDynamicPropertyReturns404() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // PropertyComp is present on entity 1, but it has no "NoSuch" dynamic property inside it.
    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .appendPropertySegment("PropertyComp")
            .appendPropertySegment("NoSuch")
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
  public void readDynamicPropertyOnAbsentParentComplexReturns404() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Entity 2 has no PropertyComp property at all (unlike entity 1), so navigating into it must
    // 404 rather than NPE.
    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(2)
            .appendPropertySegment("PropertyComp")
            .appendPropertySegment("CompDynamic")
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
  public void readNullDynamicPropertyReturns204WithoutContentType() {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Entity 1 is seeded with a present-but-null dynamic property (DynamicNull). A present-but-null
    // value must 204, and - mirroring the sibling declared-property read - must NOT set a
    // Content-Type header on that 204 (there is no body to describe).
    final ODataPropertyRequest<ClientProperty> request = getClient().getRetrieveRequestFactory()
        .getPropertyRequest(getClient().newURIBuilder(SERVICE_URI)
            .appendEntitySetSegment(ES_OPEN)
            .appendKeySegment(1)
            .appendPropertySegment("DynamicNull")
            .build());
    setCookieHeader(request);

    final ODataRetrieveResponse<ClientProperty> response = request.execute();
    saveCookieHeader(response);
    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
    assertNull(response.getContentType());
  }

  @Test
  public void putReplacesDynamicPropertyValue() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final URI uri = dynamicPropertyUri(1, "DynamicString");

    final HttpResponse<String> putResponse = sendDynamicPropertyRequest("PUT", uri, "{\"value\":\"changed\"}", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), putResponse.statusCode());
    final String isolatedCookie = isolatedCookie(putResponse);

    final HttpResponse<String> getResponse = sendDynamicPropertyRequest("GET", uri, null, isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), getResponse.statusCode());
    assertTrue(getResponse.body(), getResponse.body().endsWith("\"value\":\"changed\"}"));
  }

  @Test
  public void putChangesDynamicPropertyType() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // DynamicInt is seeded as an Int64 (42). A PUT carrying a value@odata.type annotation must be
    // able to switch it to an entirely different scalar type (here Guid) - PUT replaces both the
    // stored value AND type, unlike the whole-entity PUT/PATCH path's dynamic-property handling
    // (DataProvider#updateDynamicProperties), which only ever replaced the value.
    final URI uri = dynamicPropertyUri(1, "DynamicInt");
    final String uuid = UUID.randomUUID().toString();

    final HttpResponse<String> putResponse = sendDynamicPropertyRequest("PUT", uri,
        "{\"value@odata.type\":\"#Guid\",\"value\":\"" + uuid + "\"}", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), putResponse.statusCode());
    final String isolatedCookie = isolatedCookie(putResponse);

    final HttpResponse<String> getResponse = sendDynamicPropertyRequest("GET", uri, null, isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), getResponse.statusCode());
    assertTrue(getResponse.body(), getResponse.body().endsWith("\"value\":\"" + uuid + "\"}"));
  }

  @Test
  public void patchBehavesLikePutForDynamicScalar() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final URI uri = dynamicPropertyUri(1, "DynamicString");

    final HttpResponse<String> patchResponse =
        sendDynamicPropertyRequest("PATCH", uri, "{\"value\":\"patched\"}", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), patchResponse.statusCode());
    final String isolatedCookie = isolatedCookie(patchResponse);

    final HttpResponse<String> getResponse = sendDynamicPropertyRequest("GET", uri, null, isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), getResponse.statusCode());
    assertTrue(getResponse.body(), getResponse.body().endsWith("\"value\":\"patched\"}"));
  }

  @Test
  public void patchCreatesAbsentDynamicPropertyUpsert() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Design decision (see task report): tecsvc's declared-property direct-path update never
    // actually encounters a truly-absent property slot in normal use (DataProvider allocates one
    // for every declared property at entity-creation time, present-but-null at worst), so there is
    // no established "absent declared property -> 404" precedent to mirror here. Both PUT and PATCH
    // on an absent dynamic property therefore upsert (create it), matching ordinary REST PUT/PATCH
    // idempotent-create semantics. Entity 3 has no dynamic properties at all.
    final URI uri = dynamicPropertyUri(3, "FreshlyPatched");

    final HttpResponse<String> patchResponse =
        sendDynamicPropertyRequest("PATCH", uri, "{\"value\":\"created via patch\"}", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), patchResponse.statusCode());
    final String isolatedCookie = isolatedCookie(patchResponse);

    final HttpResponse<String> getResponse = sendDynamicPropertyRequest("GET", uri, null, isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), getResponse.statusCode());
    assertTrue(getResponse.body(), getResponse.body().endsWith("\"value\":\"created via patch\"}"));
  }

  @Test
  public void deleteRemovesDynamicProperty() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final URI deleteUri = dynamicPropertyUri(1, "DynamicString");

    final HttpResponse<String> deleteResponse = sendDynamicPropertyRequest("DELETE", deleteUri, null, null);
    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), deleteResponse.statusCode());
    final String isolatedCookie = isolatedCookie(deleteResponse);

    final HttpResponse<String> getResponse = sendDynamicPropertyRequest("GET", deleteUri, null, isolatedCookie);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), getResponse.statusCode());

    // The sibling dynamic property (DynamicInt) must be untouched by deleting DynamicString.
    final URI siblingUri = dynamicPropertyUri(1, "DynamicInt");
    final HttpResponse<String> siblingResponse = sendDynamicPropertyRequest("GET", siblingUri, null, isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), siblingResponse.statusCode());
    assertTrue(siblingResponse.body(), siblingResponse.body().endsWith("\"value\":42}"));
  }

  @Test
  public void deleteAbsentDynamicPropertyReturns404() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Entity 3 is seeded with no dynamic properties at all: nothing to remove.
    final HttpResponse<String> response =
        sendDynamicPropertyRequest("DELETE", dynamicPropertyUri(3, "DynamicString"), null, null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.statusCode());
  }

  @Test
  public void writeObjectPayloadToDynamicPropertyRejected() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final HttpResponse<String> response = sendDynamicPropertyRequest(
        "PUT", dynamicPropertyUri(1, "DynamicString"), "{\"value\":{\"a\":1}}", null);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.statusCode());
  }

  @Test
  public void writeUnresolvableAnnotatedTypeToDynamicPropertyRejected() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    final HttpResponse<String> response = sendDynamicPropertyRequest("PUT", dynamicPropertyUri(1, "DynamicString"),
        "{\"value@odata.type\":\"#Bogus\",\"value\":\"x\"}", null);
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), response.statusCode());
  }

  @Test
  public void putFullyReplacesDynamicCollectionProperty() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Design decision: a dynamic collection-valued property is written the same way a declared
    // collection property is (DataProvider#updateProperty's collection branch always clears and
    // replaces wholesale, regardless of the patch flag, since a collection has no per-element
    // identity to merge/patch by) - so PUT and PATCH on a dynamic collection both do a full
    // replace, never an append/merge.
    final URI uri = dynamicPropertyUri(3, "DynamicCollection");

    final HttpResponse<String> putResponse = sendDynamicPropertyRequest("PUT", uri, "{\"value\":[1,2,3]}", null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), putResponse.statusCode());
    final String isolatedCookie = isolatedCookie(putResponse);

    final HttpResponse<String> patchResponse =
        sendDynamicPropertyRequest("PATCH", uri, "{\"value\":[9]}", isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), patchResponse.statusCode());
    assertTrue(patchResponse.body(), patchResponse.body().endsWith("\"value\":[9]}"));

    final HttpResponse<String> getResponse = sendDynamicPropertyRequest("GET", uri, null, isolatedCookie);
    assertEquals(HttpStatusCode.OK.getStatusCode(), getResponse.statusCode());
    assertTrue(getResponse.body(), getResponse.body().endsWith("\"value\":[9]}"));
  }

  @Test
  public void writeNestedDynamicPropertyStillNotImplemented() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Scope decision (see task report): only entity-level dynamic properties are served for direct
    // PUT/PATCH/DELETE by this task; a dynamic property nested inside an open complex property
    // (PropertyComp/CompDynamic, seeded on entity 1) must still fail cleanly with 501 - not crash -
    // exactly like the whole write path did before this task, mirroring
    // readNestedDynamicPropertyDirectly's GET counterpart for the read side.
    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_OPEN)
        .appendKeySegment(1)
        .appendPropertySegment("PropertyComp")
        .appendPropertySegment("CompDynamic")
        .build();

    final HttpResponse<String> putResponse = sendDynamicPropertyRequest("PUT", uri, "{\"value\":\"x\"}", null);
    assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), putResponse.statusCode());

    final HttpResponse<String> deleteResponse = sendDynamicPropertyRequest("DELETE", uri, null, null);
    assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), deleteResponse.statusCode());
  }

  @Test
  public void writeDynamicPropertyOnClosedTypeStillRejected() throws IOException, InterruptedException {
    assumeTrue(ASSUME_JSON_REASON, isJson());

    // Pin: ETTwoPrim is a closed type, so an undeclared segment must still be rejected at
    // URI-parse time regardless of dynamic-property writes now being served for open types.
    final URI uri = getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment("ESTwoPrim")
        .appendKeySegment(32766)
        .appendPropertySegment("Nope")
        .build();

    final HttpResponse<String> response = sendDynamicPropertyRequest("PUT", uri, "{\"value\":\"x\"}", null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), response.statusCode());
  }

  private URI dynamicPropertyUri(final int key, final String propertyName) {
    return getClient().newURIBuilder(SERVICE_URI)
        .appendEntitySetSegment(ES_OPEN)
        .appendKeySegment(key)
        .appendPropertySegment(propertyName)
        .build();
  }

  /**
   * Sends a raw PUT/PATCH/DELETE/GET request with a hand-written JSON body, bypassing the OData
   * client SDK entirely. This is necessary because the SDK's typed property-update API
   * (see {@code ODataPropertyUpdateRequestImpl}) always serializes from a {@link ClientProperty}
   * and cannot express a {@code value@odata.type} annotation or a deliberately-malformed payload,
   * and it offers no PATCH variant for a primitive property at all (only PUT).
   * {@code java.net.http.HttpClient} is used instead of {@code java.net.HttpURLConnection} (the
   * idiom used elsewhere under {@code fit.tecsvc.http}) because the latter rejects the "PATCH"
   * method with a {@code ProtocolException} on the JDK's built-in implementation.
   *
   * @param cookie the session cookie to send, or {@code null} to start a fresh, isolated session
   *          (see the class-wide isolation rationale on {@link #createEntityWithDynamicProperty})
   */
  private HttpResponse<String> sendDynamicPropertyRequest(final String method, final URI uri, final String jsonBody,
      final String cookie) throws IOException, InterruptedException {
    final HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
        .header(HttpHeader.ACCEPT, "application/json")
        .method(method, jsonBody == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
    if (jsonBody != null) {
      builder.header(HttpHeader.CONTENT_TYPE, "application/json");
    }
    if (cookie != null) {
      builder.header(HttpHeader.COOKIE, cookie);
    }
    return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String isolatedCookie(final HttpResponse<String> response) {
    return response.headers().firstValue(HttpHeader.SET_COOKIE)
        .orElseThrow(() -> new AssertionError("Expected a Set-Cookie header to isolate the mutating session"));
  }
}
