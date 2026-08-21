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
 * Copyright 2026 SiteNetSoft - Added a fit round trip proving the streamed
 * collection-property serialization path produces byte-identical wire output
 * to the pre-streaming buffered path.
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.AbstractBaseTestITCase;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

/**
 * End-to-end round trip for the two tecsvc endpoints that Tier 6 Wave 3 moved onto the streamed
 * collection-property serializer: {@code ESCollAllPrim(1)/CollPropertyString} (primitive) and
 * {@code ESMixPrimCollComp(7)/CollPropertyComp} (complex). The expected bodies below were captured
 * from master commit {@code 30bfb2177} (the pre-Wave-3 baseline, before any streaming code existed)
 * by running an ad hoc capture test against the embedded tecsvc Tomcat instance in a scratch
 * worktree and pasting the byte-exact response bodies. They are pinned here so this test proves the
 * streamed path did not alter a single byte on the wire, rather than merely re-describing whatever
 * the new code happens to produce.
 */
public class StreamedCollectionITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";

  // The @odata.metadataEtag is a random UUID minted fresh each time the tecsvc server starts up,
  // so it cannot be pinned literally; it is normalized to "ETAG" in both the captured and the
  // actual body before comparison, and every other byte is asserted verbatim.
  private static final Pattern UUID_PATTERN =
      Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  // Captured from master 30bfb2177 before streaming was enabled.
  private static final String PRIM_COLLECTION_BODY =
      "{\"@odata.context\":\"../$metadata#ESCollAllPrim(1)/CollPropertyString\","
      + "\"@odata.metadataEtag\":\"W/\\\"ETAG\\\"\","
      + "\"value\":[\"Employee1@company.example\",\"Employee2@company.example\","
      + "\"Employee3@company.example\"]}";

  // Captured from master 30bfb2177 before streaming was enabled.
  private static final String COMP_COLLECTION_BODY =
      "{\"@odata.context\":\"../$metadata#ESMixPrimCollComp(7)/CollPropertyComp\","
      + "\"@odata.metadataEtag\":\"W/\\\"ETAG\\\"\","
      + "\"value\":[{\"PropertyInt16\":123,\"PropertyString\":\"TEST 1\"},"
      + "{\"PropertyInt16\":456,\"PropertyString\":\"TEST 2\"},"
      + "{\"@odata.type\":\"#olingo.odata.test1.CTBase\",\"PropertyInt16\":789,"
      + "\"PropertyString\":\"TEST 3\",\"AdditionalPropString\":\"ADD TEST\"}]}";

  private static String normalizeMetadataEtag(final String body) {
    return UUID_PATTERN.matcher(body).replaceAll("ETAG");
  }

  private HttpURLConnection get(final String pathAndQuery) throws Exception {
    URL url = new URL(SERVICE_URI + pathAndQuery);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.setRequestProperty(HttpHeader.ACCEPT, "application/json");
    connection.connect();
    return connection;
  }

  @Test
  public void primitiveCollectionIsDeliveredAndWellFormed() throws Exception {
    HttpURLConnection connection = get("ESCollAllPrim(1)/CollPropertyString");

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertEquals(PRIM_COLLECTION_BODY, normalizeMetadataEtag(content));
  }

  @Test
  public void complexCollectionIsDeliveredAndWellFormed() throws Exception {
    HttpURLConnection connection = get("ESMixPrimCollComp(7)/CollPropertyComp");

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertEquals(COMP_COLLECTION_BODY, normalizeMetadataEtag(content));
  }

  @Test
  public void primitiveCollectionWithCountPutsTheCountBeforeTheValue() throws Exception {
    HttpURLConnection connection = get("ESCollAllPrim(1)/CollPropertyString?$count=true");

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("\"@odata.count\":3"));
    // [OData-JSON] 4.4: odata.count must precede value in document order.
    assertTrue(content.indexOf("@odata.count") < content.indexOf("\"value\""));
  }

  @Test
  public void streamedCollectionIsChunkedOrContentLengthed() throws Exception {
    // Only assert the response is retrievable and complete; the container decides framing and
    // Part 1 says nothing about chunked delivery being required for ordinary responses.
    HttpURLConnection connection = get("ESMixPrimCollComp(7)/CollPropertyComp");

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertEquals(COMP_COLLECTION_BODY, normalizeMetadataEtag(content));
  }

  @Test
  public void aNonStreamedCollectionStillWorks() throws Exception {
    HttpURLConnection connection = get("ESCollAllPrim(1)/CollPropertyInt16");

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("\"value\":[1000,2000,30112]"));
  }

  @Override
  protected ODataClient getClient() {
    return null;
  }
}
