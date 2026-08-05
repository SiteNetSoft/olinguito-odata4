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
 *
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Port OLINGO-1567: changeset 3xx error response regression test
 */
package org.sitenetsoft.olinguito.client.core.communication.response.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.io.BufferedReader;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchLineIterator;
import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchResponseItem;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataBatchResponse;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataResponse;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataBatchLineIteratorImpl;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataBatchUtilities;
import org.sitenetsoft.olinguito.client.core.communication.request.batch.ODataChangesetResponseItem;
import org.junit.jupiter.api.Test;

class ODataBatchResponseTest {

  @Test
  void testBatchResponse() throws URISyntaxException {

    ODataChangesetResponseItem expectedResItem = new ODataChangesetResponseItem(true);
    List<ODataBatchResponseItem> resList = new ArrayList<>();
    resList.add(expectedResItem);
    ODataBatchResponseManager manager = new ODataBatchResponseManager(new BatchResponse(), resList );
    assertNotNull(manager);
    assertNotNull(manager.next());
    assertNotNull(manager.hasNext());
   }
  
  @Test
  void testErrorBatchResponse() throws URISyntaxException {
    Map<String, Collection<String>> header = new HashMap<>();
    List<String> list = new ArrayList<>();
    list.add("multipart/mixed;boundary=changeset_12ks93js84d");
    header.put("content-type", list);
    final InputStream input = getClass().getResourceAsStream("batchResponse.batch");
    Reader reader = new InputStreamReader(input);
    ODataBatchLineIterator iterator = new ODataBatchLineIteratorImpl(new BufferedReader(reader));
    String boundary = "changeset_12ks93js84d";
    iterator.next();
    iterator.next();
    iterator.next();
    iterator.next();
    iterator.next();
    iterator.next();
    iterator.next();
    iterator.next();
    Entry<Integer, String> line = ODataBatchUtilities.readResponseLine(iterator);
    ODataBatchErrorResponse error = new ODataBatchErrorResponse
        (line, header, iterator, boundary );
    assertNotNull(error);
    assertNull(error.getETag());
   }
  
  @Test
  void changesetErrorWith3xxStatusIsParsedAsErrorResponse() {
    // OLINGO-1567: a failed changeset is returned as a single application/http error response
    // (not multipart/mixed). When that response carries a 3xx status (reporter's "code > 300"),
    // nextUnexpected() previously only handled >= 400 and threw "Expected item not found".
    final String batch = "--batch_id\r\n"
        + "Content-Type: application/http\r\n"
        + "Content-Transfer-Encoding: binary\r\n"
        + "\r\n"
        + "HTTP/1.1 303 See Other\r\n"
        + "Content-Type: application/json\r\n"
        + "\r\n"
        + "{\"error\":{\"code\":\"err\",\"message\":\"changeset failed\"}}\r\n"
        + "--batch_id--\r\n";

    final ODataChangesetResponseItem changeset = new ODataChangesetResponseItem(false);
    // Two expected responses so the changeset's internal iterator is non-empty.
    changeset.addResponse("1", mock(ODataResponse.class));
    changeset.addResponse("2", mock(ODataResponse.class));

    final List<ODataBatchResponseItem> expected = new ArrayList<>();
    expected.add(changeset);

    final ODataBatchResponseManager manager =
        new ODataBatchResponseManager(new RawBatchResponse(batch.getBytes(StandardCharsets.UTF_8)), expected);

    final ODataBatchResponseItem item = manager.next();
    assertTrue(item.hasNext());
    final ODataResponse response = item.next();
    assertInstanceOf(ODataBatchErrorResponse.class, response);
    // A failed changeset yields exactly one error response, then iteration stops.
    assertEquals(false, item.hasNext());
  }

  static class RawBatchResponse implements ODataBatchResponse {
    private final byte[] bytes;

    RawBatchResponse(final byte[] bytes) {
      this.bytes = bytes;
    }

    @Override
    public Collection<String> getHeaderNames() {
      return null;
    }

    @Override
    public Collection<String> getHeader(final String name) {
      final List<String> list = new ArrayList<>();
      list.add("multipart/mixed;boundary=batch_id");
      return list;
    }

    @Override
    public String getETag() {
      return null;
    }

    @Override
    public String getContentType() {
      return "multipart/mixed;boundary=batch_id";
    }

    @Override
    public int getStatusCode() {
      return 202;
    }

    @Override
    public String getStatusMessage() {
      return null;
    }

    @Override
    public InputStream getRawResponse() {
      return new ByteArrayInputStream(bytes);
    }

    @Override
    public ODataResponse initFromHttpResponse(final ODataHttpResponse res) {
      return null;
    }

    @Override
    public ODataResponse initFromBatch(final Entry<Integer, String> responseLine,
        final Map<String, Collection<String>> headers, final ODataBatchLineIterator batchLineIterator,
        final String boundary) {
      return null;
    }

    @Override
    public ODataResponse initFromEnclosedPart(final InputStream part) {
      return null;
    }

    @Override
    public void close() {
    }

    @Override
    public Iterator<ODataBatchResponseItem> getBody() {
      return null;
    }
  }

  class BatchResponse implements ODataBatchResponse{

    @Override
    public Collection<String> getHeaderNames() {
      return null;
    }

    @Override
    public Collection<String> getHeader(String name) {
      List<String> list = new ArrayList<>();
      list.add("multipart/mixed;boundary=changeset_12ks93js84d");
      return list;
    }

    @Override
    public String getETag() {
      return null;
    }

    @Override
    public String getContentType() {
      return null;
    }

    @Override
    public int getStatusCode() {
      return 0;
    }

    @Override
    public String getStatusMessage() {
      return null;
    }

    @Override
    public InputStream getRawResponse() {
      return  getClass().getResourceAsStream("batchResponse.batch");
    }

    @Override
    public ODataResponse initFromHttpResponse(ODataHttpResponse res) {
      return null;
    }

    @Override
    public ODataResponse initFromBatch(Entry<Integer, String> responseLine,
        Map<String, Collection<String>> headers,
        ODataBatchLineIterator batchLineIterator, String boundary) {
      return null;
    }

    @Override
    public ODataResponse initFromEnclosedPart(InputStream part) {
      return null;
    }

    @Override
    public void close() {
    }

    @Override
    public Iterator<ODataBatchResponseItem> getBody() {
      return null;
    }
    
  }
  
}
