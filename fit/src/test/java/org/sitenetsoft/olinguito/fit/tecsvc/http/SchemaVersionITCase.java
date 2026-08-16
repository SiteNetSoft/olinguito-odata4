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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 3: fit-level round trips for the versioned
 * tecsvc schema and the $schemaversion system query option (OData 4.01, Part 1: Protocol,
 * section 11.2.12)
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.AbstractBaseTestITCase;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

/**
 * Raw-HTTP round trips against a real Tomcat instance for the OData 4.01 <tt>$schemaversion</tt>
 * system query option (Part 1: Protocol, section 11.2.12) and the <tt>Core.SchemaVersion</tt>
 * annotation tecsvc publishes on its schema. tecsvc is wired with the schema version
 * {@value #SCHEMA_VERSION}; the annotation in <tt>$metadata</tt> and the value the server accepts
 * come from one and the same constant
 * ({@code org.sitenetsoft.olinguito.server.tecsvc.provider.SchemaProvider#SCHEMA_VERSION}), which
 * these tests pin from the outside.
 */
public class SchemaVersionITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";
  private static final String SCHEMA_VERSION = "1.0.0";
  private static final String UNKNOWN_SCHEMA_VERSION = "9.9.9";
  /**
   * Exact serialized shape of the schema-level annotation in the XML metadata document: the
   * metadata serializer writes a constant expression as a child element, not as an attribute.
   */
  private static final String SCHEMA_VERSION_ANNOTATION =
      "<Annotation Term=\"Core.SchemaVersion\"><String>" + SCHEMA_VERSION + "</String></Annotation>";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_XML = "application/xml";

  private static final String CRLF = "\r\n";
  private static final String BATCH_CONTENT_TYPE = " "
      + ContentType.create(ContentType.MULTIPART_MIXED, "boundary", "batch_123").toContentTypeString();
  private static final String HEADER_CONTENT_TRANSFER_ENCODING_BINARY = "Content-Transfer-Encoding: binary";
  private static final String HEADER_CONTENT_TYPE_HTTP =
      HttpHeader.CONTENT_TYPE + ": " + ContentType.APPLICATION_HTTP.toContentTypeString();

  @Test
  public void metadataCarriesSchemaVersionAnnotation() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET, "$metadata", APPLICATION_XML, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue("the schema must carry the Core.SchemaVersion annotation",
        body.contains(SCHEMA_VERSION_ANNOTATION));
    assertTrue("the annotation must sit on the Schema element itself, not on one of its children",
        body.contains(SCHEMA_VERSION_ANNOTATION + "</Schema>"));
  }

  @Test
  public void jsonMetadataCarriesSchemaVersionAnnotation() throws Exception {
    final HttpURLConnection connection =
        getConnection(HttpMethod.GET, "$metadata?$format=json", APPLICATION_JSON, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the JSON metadata document must carry the same annotation",
        readResponse(connection).contains("\"@Core.SchemaVersion\":\"" + SCHEMA_VERSION + "\""));
  }

  @Test
  public void metadataWithStarSchemaVersion() throws Exception {
    final HttpURLConnection connection =
        getConnection(HttpMethod.GET, "$metadata?$schemaversion=*", APPLICATION_XML, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("'*' always matches the current version",
        readResponse(connection).contains(SCHEMA_VERSION_ANNOTATION));
  }

  @Test
  public void metadataWithMatchingSchemaVersion() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        "$metadata?$schemaversion=" + SCHEMA_VERSION, APPLICATION_XML, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the served document must be the requested version",
        readResponse(connection).contains(SCHEMA_VERSION_ANNOTATION));
  }

  @Test
  public void metadataWithUnknownSchemaVersionIsNotFound() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        "$metadata?$schemaversion=" + UNKNOWN_SCHEMA_VERSION, APPLICATION_XML, null, null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
    assertTrue("the error body must name the version that does not exist",
        readResponse(connection).contains(UNKNOWN_SCHEMA_VERSION));
  }

  @Test
  public void requestWithoutSchemaVersionStillPasses() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET, "ESAllPrim", APPLICATION_JSON, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
  }

  @Test
  public void matchingSchemaVersionPasses() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        "ESAllPrim?$schemaversion=" + SCHEMA_VERSION, APPLICATION_JSON, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the collection must still be served", readResponse(connection).contains("\"PropertyInt16\":"));
  }

  @Test
  public void starSchemaVersionPasses() throws Exception {
    final HttpURLConnection connection =
        getConnection(HttpMethod.GET, "ESAllPrim?$schemaversion=*", APPLICATION_JSON, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the collection must still be served", readResponse(connection).contains("\"PropertyInt16\":"));
  }

  @Test
  public void schemaVersionCombinesWithOtherQueryOptions() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        "ESAllPrim?$select=PropertyInt16&$top=1&$schemaversion=" + SCHEMA_VERSION,
        APPLICATION_JSON, null, null);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue("selected property must be present", body.contains("\"PropertyInt16\":"));
    assertTrue("non-selected property must be absent", !body.contains("\"PropertyString\":"));
  }

  @Test
  public void unknownSchemaVersionIsNotFound() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.GET,
        "ESAllPrim?$schemaversion=" + UNKNOWN_SCHEMA_VERSION, APPLICATION_JSON, null, null);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
    final String body = readResponse(connection);
    assertTrue("the response must be an OData error body", body.contains("\"error\""));
    assertTrue("the error body must name the version that does not exist", body.contains(UNKNOWN_SCHEMA_VERSION));
  }

  /**
   * MANDATORY Wave-1 carry-over: a <tt>$schemaversion</tt> supplied in a <tt>/$query</tt> POST body
   * must be merged into the rewritten request before the version check runs, exactly as if it had
   * been part of the URL query string.
   */
  @Test
  public void schemaVersionThroughQueryPostAccepted() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "$metadata/$query",
        APPLICATION_XML, "$schemaversion=*", TEXT_PLAIN);
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertTrue("the metadata document must be served",
        readResponse(connection).contains(SCHEMA_VERSION_ANNOTATION));
  }

  @Test
  public void schemaVersionThroughQueryPostRejected() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST, "$metadata/$query",
        APPLICATION_XML, "$schemaversion=" + UNKNOWN_SCHEMA_VERSION, TEXT_PLAIN);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
    assertTrue("the error body must name the version that does not exist",
        readResponse(connection).contains(UNKNOWN_SCHEMA_VERSION));
  }

  /**
   * A batch part that carries no <tt>$schemaversion</tt> of its own inherits the outer
   * <tt>$batch</tt> request's value; since the outer value has already been validated, the part is
   * served normally. (That the value is actually appended to the part is pinned from the inside by
   * {@code MockedBatchHandlerTest#batchPartsInheritOuterSchemaVersion}; here we pin that the
   * appended option does not break the part.)
   */
  @Test
  public void batchPartInheritsOuterSchemaVersion() throws Exception {
    final HttpURLConnection connection =
        batch("$batch?$schemaversion=" + SCHEMA_VERSION, getRequest("ESAllPrim(32767)"));
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("HTTP/1.1 200 OK", firstPartStatusLine(connection));
  }

  /**
   * The outer <tt>$batch</tt> request is itself a request and is version-checked before its parts
   * are dispatched, so an unknown outer version rejects the whole batch with 404 rather than
   * producing a batch response whose parts fail individually.
   */
  @Test
  public void batchWithUnknownOuterSchemaVersionIsNotFound() throws Exception {
    final HttpURLConnection connection = getConnection(HttpMethod.POST,
        "$batch?$schemaversion=" + UNKNOWN_SCHEMA_VERSION, APPLICATION_JSON,
        getRequest("ESAllPrim(32767)"), BATCH_CONTENT_TYPE);
    assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), connection.getResponseCode());
    assertTrue("the error body must name the version that does not exist",
        readResponse(connection).contains(UNKNOWN_SCHEMA_VERSION));
  }

  /**
   * A part's own <tt>$schemaversion</tt> takes precedence over the outer request's and is validated
   * for that part alone: the batch as a whole still succeeds, but the offending part answers 404.
   */
  @Test
  public void batchPartOwnUnknownSchemaVersionIsNotFound() throws Exception {
    final HttpURLConnection connection = batch("$batch?$schemaversion=*",
        getRequest("ESAllPrim(32767)?$schemaversion=" + UNKNOWN_SCHEMA_VERSION));
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("HTTP/1.1 404 Not Found", firstPartStatusLine(connection));
  }

  @Test
  public void batchPartOwnSchemaVersionOverridesOuter() throws Exception {
    final HttpURLConnection connection = batch("$batch?$schemaversion=" + SCHEMA_VERSION,
        getRequest("ESAllPrim(32767)?$schemaversion=*"));
    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals("HTTP/1.1 200 OK", firstPartStatusLine(connection));
  }

  private HttpURLConnection batch(final String pathAndQuery, final String content) throws IOException {
    return getConnection(HttpMethod.POST, pathAndQuery, APPLICATION_JSON, content, BATCH_CONTENT_TYPE);
  }

  private String getRequest(final String uri) {
    return "--batch_123" + CRLF
        + HEADER_CONTENT_TRANSFER_ENCODING_BINARY + CRLF
        + HEADER_CONTENT_TYPE_HTTP + CRLF
        + CRLF
        + "GET " + uri + " HTTP/1.1" + CRLF
        + CRLF
        + CRLF
        + "--batch_123--";
  }

  /**
   * Reads past the batch response's boundary and MIME part headers and returns the HTTP status line
   * of the first part.
   */
  private String firstPartStatusLine(final HttpURLConnection connection) throws IOException {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
      assertTrue("first line must be the batch boundary", reader.readLine().contains("batch_"));
      assertEquals(HEADER_CONTENT_TYPE_HTTP, reader.readLine());
      assertEquals(HEADER_CONTENT_TRANSFER_ENCODING_BINARY, reader.readLine());
      assertEquals("", reader.readLine());
      return reader.readLine();
    }
  }

  private HttpURLConnection getConnection(final HttpMethod method, final String pathAndQuery,
      final String accept, final String body, final String contentType) throws IOException {
    final URL url = new URL(SERVICE_URI + pathAndQuery);
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(method.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, accept);
    if (body != null) {
      connection.setRequestProperty(HttpHeader.CONTENT_TYPE, contentType);
      connection.setDoOutput(true);
      final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
      writer.write(body);
      writer.close();
    }
    connection.connect();
    return connection;
  }

  private static String readResponse(final HttpURLConnection connection) throws IOException {
    final InputStream stream = connection.getResponseCode() >= 400
        ? connection.getErrorStream() : connection.getInputStream();
    return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Override
  protected ODataClient getClient() {
    return null;
  }
}
