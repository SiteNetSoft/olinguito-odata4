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
 * Copyright 2026 SiteNetSoft - OLINGO-1505: Stream-property links are now written at metadata=minimal
 * Copyright 2026 SiteNetSoft - Adjusted XML stream-property assertions for the new XML refusal
 * Copyright 2026 SiteNetSoft - Real BAD_REQUEST assertions + moved paging/count XML coverage
 * to ESServerSidePaging (fix round 1)
 * Copyright 2026 SiteNetSoft - Added second-page XML paging coverage on ESServerSidePaging (fix round 2)
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

// IOUtils removed - using Java standard library
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.AbstractBaseTestITCase;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

public class BasicStreamITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";

  @Test
  public void streamESStreamJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStream?$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("Streamed-Employee1@company.example\"," +
            "\"Streamed-Employee2@company.example\"," +
            "\"Streamed-Employee3@company.example\""));
    assertTrue(content.contains("\"PropertyString\":\"TEST 1->streamed\""));
    assertTrue(content.contains("\"PropertyString\":\"TEST 2->streamed\""));
  }
  
  @Test
  public void streamESWithStreamJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESWithStream?$expand=PropertyStream&$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));
  }

  @Test
  public void streamESStreamXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStream?$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.APPLICATION_XML, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("<m:element>Streamed-Employee1@company.example</m:element>" +
            "<m:element>Streamed-Employee2@company.example</m:element>" +
            "<m:element>Streamed-Employee3@company.example</m:element>"));
    assertTrue(content.contains("<d:PropertyString>TEST 1->streamed</d:PropertyString>"));
    assertTrue(content.contains("<d:PropertyString>TEST 2->streamed</d:PropertyString>"));
  }

  @Test
  public void streamESStreamServerSidePagingXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    // ODataXmlSerializer#entityCollectionStreamed now runs a static, data-free EDM check for
    // Edm.Stream properties before ODataWritableContent.with(...) is even built, i.e. before the
    // processor commits any status or content type to the wire - so this now gets the same clean
    // 400 that ESWithStream and ESGeo already get in XML, instead of the stale, undefined-behavior
    // fragment ("<d:PropertyStream m:type=\"Stream\">readLink</d:PropertyStream>") this test used
    // to pin.
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    final String content = new String(connection.getErrorStream().readAllBytes(), Charset.defaultCharset());
    assertTrue("Expected the message to name the property, got: " + content, content.contains("PropertyStream"));
  }

  @Test
  public void streamESStreamServerSidePagingNextXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$format=xml&$skiptoken=1%2A10");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    // See streamESStreamServerSidePagingXml above: the static EDM check refuses the entity type
    // before anything is written, on every page, not only the first.
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    final String content = new String(connection.getErrorStream().readAllBytes(), Charset.defaultCharset());
    assertTrue("Expected the message to name the property, got: " + content, content.contains("PropertyStream"));
  }

  /**
   * The next-link/id/property XML fragments {@code streamESStreamServerSidePagingXml} used to pin
   * on {@code ESStreamServerSidePaging} are no longer producible in XML now that the entity type is
   * refused outright - moved here onto {@code ESServerSidePaging}, the equivalent server-side-paged
   * entity set with no stream property, so the underlying XML paging mechanics stay covered.
   */
  @Test
  public void serverSidePagingXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESServerSidePaging?$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.APPLICATION_XML, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("<a:link rel=\"next\" href="));
    assertTrue(content.contains("ESServerSidePaging?$format=xml&amp;%24skiptoken=1%2A10\"/>"));
    assertTrue(content.contains("<a:id>ESServerSidePaging(1)</a:id>"));
    assertTrue(content.contains("<d:PropertyInt16 m:type=\"Int16\">1</d:PropertyInt16>"));
  }

  /**
   * The second-page next-link ({@code %24skiptoken=2%2A10}) that {@code streamESStreamServerSidePagingNextXml}
   * used to pin on {@code ESStreamServerSidePaging} has no XML coverage anywhere else once that
   * entity type is refused outright; follows {@code serverSidePagingXml}'s page-1 link onto
   * {@code ESServerSidePaging} to keep second-page XML paging covered.
   */
  @Test
  public void serverSidePagingNextXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESServerSidePaging?$format=xml&$skiptoken=1%2A10");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.APPLICATION_XML, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("<a:link rel=\"next\" href="));
    assertTrue(content.contains("ESServerSidePaging?$format=xml&amp;%24skiptoken=2%2A10\"/>"));
    assertTrue(content.contains("<a:id>ESServerSidePaging(11)</a:id>"));
    assertTrue(content.contains("<d:PropertyInt16 m:type=\"Int16\">11</d:PropertyInt16>"));
  }

  @Test
  public void streamESStreamServerSidePagingJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("{\"PropertyInt16\":2,"+
    "\"PropertyStream@odata.mediaEtag\":\"eTag\",\"PropertyStream@odata.mediaContentType\":\"image/jpeg\","
    + "\"PropertyStream@odata.mediaEditLink\":\"http://mediaserver:1234/editLink\"}"));
    assertTrue(content.contains("\"@odata.nextLink\""));
    assertTrue(content.contains("ESStreamServerSidePaging?$format=json&%24skiptoken=1%2A10"));
  }
  
  
  @Test
  public void streamESStreamServerSidePagingJsonNext() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$format=json&$skiptoken=1%2A10");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("{\"PropertyInt16\":12,"+
    "\"PropertyStream@odata.mediaEtag\":\"eTag\",\"PropertyStream@odata.mediaContentType\":\"image/jpeg\","
    + "\"PropertyStream@odata.mediaEditLink\":\"http://mediaserver:1234/editLink\"}"));
    assertTrue(content.contains("\"@odata.nextLink\""));
    assertTrue(content.contains("ESStreamServerSidePaging?$format=json&%24skiptoken=2%2A10"));
  }
  
  
  @Test
  public void streamCountXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$count=true&$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    // See streamESStreamServerSidePagingXml above: the static EDM check refuses the entity type
    // before anything is written, $count=true included.
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    final String content = new String(connection.getErrorStream().readAllBytes(), Charset.defaultCharset());
    assertTrue("Expected the message to name the property, got: " + content, content.contains("PropertyStream"));
  }

  /**
   * The {@code <m:count>504</m:count>} fragment {@code streamCountXml} used to pin on
   * {@code ESStreamServerSidePaging} is no longer producible in XML now that the entity type is
   * refused outright - moved here onto {@code ESServerSidePaging} (503 entities), the equivalent
   * server-side-paged entity set with no stream property, so $count in XML stays covered.
   */
  @Test
  public void serverSidePagingCountXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESServerSidePaging?$count=true&$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.APPLICATION_XML, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("<a:link rel=\"next\" href="));
    assertTrue(content.contains("ESServerSidePaging?$count=true&amp;$format=xml&amp;%24skiptoken=1%2A10\"/>"));
    assertTrue(content.contains("<a:id>ESServerSidePaging(1)</a:id>"));
    assertTrue(content.contains("<m:count>503</m:count>"));
    assertTrue(content.contains("<d:PropertyInt16 m:type=\"Int16\">1</d:PropertyInt16>"));
  }
  
   
  @Test
  public void streamCountJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$count=true&$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("{\"PropertyInt16\":2,"+
    "\"PropertyStream@odata.mediaEtag\":\"eTag\",\"PropertyStream@odata.mediaContentType\":\"image/jpeg\","
    + "\"PropertyStream@odata.mediaEditLink\":\"http://mediaserver:1234/editLink\"}"));
    assertTrue(content.contains("\"@odata.nextLink\""));
    assertTrue(content.contains("ESStreamServerSidePaging?$count=true&$format=json&%24skiptoken=1%2A10"));
    assertTrue(content.contains("\"@odata.count\":504"));
  }
  
  @Test
  public void streamCountFalsetXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$count=false&$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    // See streamESStreamServerSidePagingXml above: the static EDM check refuses the entity type
    // before anything is written, $count=false included.
    assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), connection.getResponseCode());
    final String content = new String(connection.getErrorStream().readAllBytes(), Charset.defaultCharset());
    assertTrue("Expected the message to name the property, got: " + content, content.contains("PropertyStream"));
  }

  /**
   * The {@code assertFalse(content.contains("<m:count>504</m:count>"))} pin from the original
   * {@code streamCountFalsetXml} is no longer meaningful on {@code ESStreamServerSidePaging} (the
   * whole response is now refused); moved here onto {@code ESServerSidePaging} so $count=false in
   * XML stays covered.
   */
  @Test
  public void serverSidePagingCountFalseXml() throws Exception {
    URL url = new URL(SERVICE_URI + "ESServerSidePaging?$count=false&$format=xml");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.APPLICATION_XML, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertTrue(content.contains("<a:link rel=\"next\" href="));
    assertTrue(content.contains("ESServerSidePaging?$count=false&amp;$format=xml&amp;%24skiptoken=1%2A10\"/>"));
    assertTrue(content.contains("<a:id>ESServerSidePaging(1)</a:id>"));
    assertTrue(content.contains("<d:PropertyInt16 m:type=\"Int16\">1</d:PropertyInt16>"));
    assertFalse(content.contains("<m:count>503</m:count>"));
  }
  
   
  @Test
  public void streamCountFalseJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamServerSidePaging?$count=false&$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("{\"PropertyInt16\":2,"+
    "\"PropertyStream@odata.mediaEtag\":\"eTag\",\"PropertyStream@odata.mediaContentType\":\"image/jpeg\","
    + "\"PropertyStream@odata.mediaEditLink\":\"http://mediaserver:1234/editLink\"}"));
    assertTrue(content.contains("\"@odata.nextLink\""));
    assertTrue(content.contains("ESStreamServerSidePaging?$count=false&$format=json&%24skiptoken=1%2A10"));
    assertFalse(content.contains("\"@odata.count\":504"));
    }
  
  @Test
  public void expandStreamPropOnComplexTypeJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamOnComplexProp(7)?$expand=PropertyCompWithStream/PropertyStream,"
        + "PropertyEntityStream,PropertyCompWithStream/NavPropertyETStreamOnComplexPropOne($expand=PropertyStream),"
        + "PropertyCompWithStream/NavPropertyETStreamOnComplexPropMany/$count&$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.setRequestProperty("OData-Version", "4.01");
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("\"NavPropertyETStreamOnComplexPropOne\":{"
        + "\"PropertyInt16\":7,"
        + "\"PropertyStream@mediaEtag\":\"eTag\","
        + "\"PropertyStream@mediaContentType\":\"image/jpeg\","
        + "\"PropertyStream@mediaEditLink\":\"http://mediaserver:1234/editLink\","
        + "\"PropertyStream\":\"\ufffdioz\ufffd\\\"\ufffd\"}"));
    assertTrue(content.contains("\"NavPropertyETStreamOnComplexPropMany@count\":2"));
    assertTrue(content.contains("\"PropertyCompWithStream\":{"
        + "\"PropertyStream@mediaEtag\":\"eTag\","
        + "\"PropertyStream@mediaContentType\":\"image/jpeg\","
        + "\"PropertyStream@mediaEditLink\":\"http://mediaserver:1234/editLink\","
        + "\"PropertyStream\":\"\ufffdioz\ufffd\\\"\ufffd\","
        + "\"PropertyComp\":{\"PropertyInt16\":333,\"PropertyString\":\"TEST123\"}"));
    assertFalse(content.contains("\"PropertyInt16\":7,"
        + "\"PropertyInt32\":10,"
        + "\"PropertyEntityStream@mediaEtag\":\"eTag\","
        + "\"PropertyEntityStream@mediaContentType\":\"image/jpeg\","
        + "\"PropertyEntityStream\":\"ufffdioz\ufffd\\\"\ufffd\""));
    }
  
  @Test
  public void expandStreamPropOnComplexTypeWithRefJson() throws Exception {
    URL url = new URL(SERVICE_URI + "ESStreamOnComplexProp(7)?$expand="
        + "PropertyCompWithStream/NavPropertyETStreamOnComplexPropMany/$ref&$format=json");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.name());
    connection.setRequestProperty("OData-Version", "4.01");
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.JSON, ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));

    final String content = new String(connection.getInputStream().readAllBytes(), Charset.defaultCharset());

    assertTrue(content.contains("\"PropertyInt16\":7,"
        + "\"PropertyInt32\":10,\"PropertyEntityStream@mediaEtag\":\"eTag\","
        + "\"PropertyEntityStream@mediaContentType\":\"image/jpeg\","
        + "\"PropertyEntityStream@mediaEditLink\":\"http://mediaserver:1234/editLink\","
        + "\"PropertyCompWithStream\":{\"PropertyStream@mediaEtag\":\"eTag\","
        + "\"PropertyStream@mediaContentType\":\"image/jpeg\","
        + "\"PropertyStream@mediaEditLink\":\"http://mediaserver:1234/editLink\","
        + "\"PropertyComp\":{\"PropertyInt16\":333,\"PropertyString\":\"TEST123\"},"
        + "\"NavPropertyETStreamOnComplexPropMany\":["
        + "{\"@id\":\"ESWithStream(32767)\"},"
        + "{\"@id\":\"ESWithStream(7)\"}]}"));
    }
  
  @Test
  public void putRequestOnStreamProperty() throws Exception {
    URL url = new URL(SERVICE_URI  + "ESWithStream(7)/PropertyStream");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.PUT.name());
    connection.setRequestProperty(HttpHeader.CONTENT_TYPE, "image/jpeg");
    connection.setRequestProperty(HttpHeader.IF_MATCH, "*");
    connection.setRequestProperty(HttpHeader.ACCEPT, "application/json");
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("sample.png");
    byte[] bytes = in.readAllBytes();
    connection.setDoOutput(true);
    BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
    try {
        out.write(bytes, 0, bytes.length);
        out.flush();
      } finally {
        out.close();
      }
    connection.connect();

    assertEquals(HttpStatusCode.OK.getStatusCode(), connection.getResponseCode());
    assertEquals(ContentType.parse("image/jpeg"), 
    		ContentType.create(connection.getHeaderField(HttpHeader.CONTENT_TYPE)));
    assertEquals(bytes.length, connection.getInputStream().readAllBytes().length);
  }
  
  @Override
  protected ODataClient getClient() {
    return null;
  }

}
