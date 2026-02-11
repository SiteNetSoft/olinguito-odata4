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
 * Copyright 2026 SiteNetSoft - Migrate from deprecated DefaultHttpClient to HttpClientBuilder
 * Copyright 2026 SiteNetSoft - Replaced Apache HTTP types with OData abstractions
 */
package org.sitenetsoft.olinguito.samples.client.core.http;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.util.CharArrayBuffer;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.core.http.AbstractHttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.ApacheHttpClient;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * Shows how to use custom client connections.
 * <br/>
 * In certain situations it may be necessary to customize the way HTTP messages get transmitted across the wire beyond
 * what is possible using HTTP parameters in order to be able to deal non-standard, non-compliant behaviours. For
 * instance, for web crawlers it may be necessary to force HttpClient into accepting malformed response heads in order
 * to salvage the content of the messages.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/advanced.html#d5e1339">More
 * information</a>.
 */
public class CustomConnectionsHttpClientFactory extends AbstractHttpClientFactory {

  private static class MyLineParser extends BasicLineParser {

    @Override
    public Header parseHeader(final CharArrayBuffer buffer) throws ParseException {
      try {
        return super.parseHeader(buffer);
      } catch (ParseException ex) {
        // Suppress ParseException exception
        return new BasicHeader("invalid", buffer.toString());
      }
    }

  }

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    return new ApacheHttpClient(HttpClientBuilder.create()
        .setUserAgent(USER_AGENT)
        .setConnectionManager(connectionManager)
        .build());
  }

  @Override
  public void close(final ODataHttpClient httpClient) {
    try {
      httpClient.close();
    } catch (java.io.IOException e) {
      // silently close
    }
  }
}
