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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1587: configurable streamed/batch response timeout
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 7: configurable $query POST retrieve requests
 * (OData 4.01 URL Conventions section 4.17)
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: metadata document format setting
 */
package org.sitenetsoft.olinguito.client.api;

import java.util.concurrent.ExecutorService;

import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.HttpUriRequestFactory;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

/**
 * Configuration wrapper.
 */
public interface Configuration {

  /**
   * Gets the configured default <tt>Accept</tt> header value format for a batch request.
   *
   * @return configured default <tt>Accept</tt> header value for a batch request.
   */
  ContentType getDefaultBatchAcceptFormat();

  /**
   * Set the default <tt>Accept</tt> header value format for a batch request.
   *
   * @param contentType default <tt>Accept</tt> header value.
   */
  void setDefaultBatchAcceptFormat(ContentType contentType);

  /**
   * Gets the configured OData format for AtomPub exchanges. If this configuration parameter doesn't exist the
   * JSON_FULL_METADATA format will be used as default.
   *
   * @return configured OData format for AtomPub if specified; JSON_FULL_METADATA format otherwise.
   */
  ContentType getDefaultPubFormat();

  /**
   * Sets the default OData format for AtomPub exchanges.
   *
   * @param format default format.
   */
  void setDefaultPubFormat(ContentType format);

  /**
   * Gets the configured OData format. This value depends on what is returned from <tt>getDefaultPubFormat()</tt>.
   *
   * @return configured OData format
   * @see #getDefaultPubFormat()
   */
  ContentType getDefaultFormat();

  /**
   * Gets the configured OData value format. If this configuration parameter doesn't exist the TEXT format will be used
   * as default.
   *
   * @return configured OData value format if specified; TEXT_PLAIN format otherwise.
   */
  ContentType getDefaultValueFormat();

  /**
   * Sets the default OData value format.
   *
   * @param format default format.
   */
  void setDefaultValueFormat(ContentType format);

  /**
   * Gets the configured OData media format. If this configuration parameter doesn't exist the APPLICATION_OCTET_STREAM
   * format will be used as default.
   *
   * @return configured OData media format if specified; APPLICATION_OCTET_STREAM format otherwise.
   */
  ContentType getDefaultMediaFormat();

  /**
   * Sets the default OData media format.
   *
   * @param format default format.
   */
  void setDefaultMediaFormat(ContentType format);

  /**
   * Gets the format the metadata document is requested in.
   * <br/>
   * OData 4.01, Part 1: Protocol section 11.1.2: "If a request for metadata does not specify a format
   * preference (via Accept header or $format) then the XML representation MUST be returned", so this
   * defaults to {@link ContentType#APPLICATION_XML} and a client that never calls
   * {@link #setMetadataFormat(ContentType)} behaves exactly as it did before this setting existed.
   * <br/>
   * This is a <tt>default</tt> method so that implementations written against earlier versions of this
   * interface keep compiling.
   *
   * @return configured metadata format; APPLICATION_XML otherwise.
   */
  default ContentType getMetadataFormat() {
    return ContentType.APPLICATION_XML;
  }

  /**
   * Sets the format the metadata document is requested in: {@link ContentType#APPLICATION_XML} for
   * [OData-CSDLXML] or {@link ContentType#APPLICATION_JSON} for [OData-CSDLJSON].
   * <br/>
   * This is a <tt>default</tt> method so that implementations written against earlier versions of this
   * interface keep compiling; they simply do not support overriding the metadata format.
   *
   * @param contentType metadata format.
   */
  default void setMetadataFormat(final ContentType contentType) {
    throw new UnsupportedOperationException("This configuration does not support a metadata format override");
  }

  /**
   * Gets the HttpClient factory to be used for executing requests.
   *
   * @return provided implementation (if configured via <tt>setHttpClientFactory</tt> or default.
   */
  HttpClientFactory getHttpClientFactory();

  /**
   * Sets the HttpClient factory to be used for executing requests.
   *
   * @param factory implementation of <tt>HttpClientFactory</tt>.
   * @see HttpClientFactory
   */
  void setHttpClientFactory(HttpClientFactory factory);

  /**
   * Gets the HttpUriRequest factory for generating requests to be executed.
   *
   * @return provided implementation (if configured via <tt>setHttpUriRequestFactory</tt> or default.
   */
  HttpUriRequestFactory getHttpUriRequestFactory();

  /**
   * Sets the HttpUriRequest factory generating requests to be executed.
   *
   * @param factory implementation of <tt>HttpUriRequestFactory</tt>.
   * @see HttpUriRequestFactory
   */
  void setHttpUriRequestFactory(HttpUriRequestFactory factory);

  /**
   * Gets whether <tt>PUT</tt>, <tt>MERGE</tt>, <tt>PATCH</tt>, <tt>DELETE</tt> HTTP methods need to be translated to
   * <tt>POST</tt> with additional <tt>X-HTTTP-Method</tt> header.
   *
   * @return whether <tt>X-HTTTP-Method</tt> header is to be used
   */
  boolean isUseXHTTPMethod();

  /**
   * Sets whether <tt>PUT</tt>, <tt>MERGE</tt>, <tt>PATCH</tt>, <tt>DELETE</tt> HTTP methods need to be translated to
   * <tt>POST</tt> with additional <tt>X-HTTTP-Method</tt> header.
   *
   * @param value 'TRUE' to use tunneling.
   */
  void setUseXHTTPMethod(boolean value);

  /**
   * Checks whether Gzip compression (e.g. support for <tt>Accept-Encoding: gzip</tt> and
   * <tt>Content-Encoding: gzip</tt> HTTP headers) is enabled.
   *
   * @return whether HTTP Gzip compression is enabled
   */
  boolean isGzipCompression();

  /**
   * Sets Gzip compression (e.g. support for <tt>Accept-Encoding: gzip</tt> and
   * <tt>Content-Encoding: gzip</tt> HTTP headers) enabled or disabled.
   *
   * @param value whether to use Gzip compression.
   */
  void setGzipCompression(boolean value);

  /**
   * Checks whether chunk HTTP encoding is being used.
   *
   * @return whether chunk HTTP encoding is being used
   */
  boolean isUseChuncked();

  /**
   * Sets chunk HTTP encoding enabled or disabled.
   *
   * @param value whether to use chunk HTTP encoding.
   */
  void setUseChuncked(boolean value);

  /**
   * Checks whether URIs contain entity key between parentheses (standard) or instead as additional segment
   * (non-standard).
   * <br/>
   * Example: http://services.odata.org/V4/OData/OData.svc/Products(0) or
   * http://services.odata.org/V4/OData/OData.svc/Products/0
   *
   * @return whether URIs shall be built with entity key between parentheses (standard) or instead as additional
   * segment.
   */
  boolean isKeyAsSegment();

  /**
   * Sets whether URIs shall be built with entity key between parentheses (standard) or instead as additional segment
   * (non-standard).
   * <br/>
   * Example: http://services.odata.org/V4/OData/OData.svc/Products(0) or
   * http://services.odata.org/V4/OData/OData.svc/Products/0
   *
   * @param value 'TRUE' to use this feature.
   */
  void setKeyAsSegment(boolean value);

  /**
   * Gets whether query URIs in request should contain fully qualified type name. - OData Intermediate Conformance
   * Level: MUST support casting to a derived type according to [OData-URL] if derived types are present in the model.
   * <br/>
   * Example: http://host/service/Customers/Model.VipCustomer(102) or http://host/service/Customers/Model.VipCustomer
   *
   * @return whether query URIs in request should contain fully qualified type name. segment.
   */
  boolean isAddressingDerivedTypes();

  /**
   * Sets whether query URIs in request should contain fully qualified type name. - OData Intermediate Conformance
   * Level: MUST support casting to a derived type according to [OData-URL] if derived types are present in the model.
   * <br/>
   * Example: http://host/service/Customers/Model.VipCustomer(102) or http://host/service/Customers/Model.VipCustomer
   *
   * @param value 'TRUE' to use this feature.
   */
  void setAddressingDerivedTypes(boolean value);

  /**
   * Checks whether operation name in request URI should be fully qualified name, which is required by OData V4
   * protocol, but some service may still choose to support shorter name.
   * <br/>
   * Example: http://host/service/Customers(2)/NS1.Model.IncreaseSalary VS
   * http://host/service/Customers(2)/IncreaseSalary
   *
   * @return wheter operation name in request URI should be fully qualified name. segment.
   */
  boolean isUseUrlOperationFQN();

  /**
   * Sets whether operation name in request URI should be fully qualified name, which is required by OData V4 protocol,
   * but some service may still choose to support shorter name.
   * <br/>
   * Example: http://host/service/Customers(2)/NS1.Model.IncreaseSalary VS
   * http://host/service/Customers(2)/IncreaseSalary
   *
   * @param value 'TRUE' to use this feature.
   */
  void setUseUrlOperationFQN(boolean value);

  /**
   * When processing a set of requests (in batch requests, for example), checks if the execution will be aborted after
   * first error encountered or not.
   *
   * @return whether execution of a set of requests will be aborted after first error
   */
  boolean isContinueOnError();

  /**
   * When processing a set of requests (in batch requests, for example), sets if the execution will be aborted after
   * first error encountered or not.
   *
   * @param value 'TRUE' to use this feature.
   */
  void setContinueOnError(boolean value);

  /**
   * Retrieves request executor service.
   *
   * @return request executor service.
   */
  ExecutorService getExecutor();

  /**
   * Sets request executor service.
   *
   * @param executorService new executor services.
   */
  void setExecutor(ExecutorService executorService);

  /**
   * Gets the maximum number of seconds the client waits for a streamed/batch response before the
   * request is aborted.
   *
   * @return response timeout in seconds.
   */
  int getResponseTimeoutInSec();

  /**
   * Sets the maximum number of seconds the client waits for a streamed/batch response before the
   * request is aborted.
   *
   * @param responseTimeoutInSec response timeout in seconds (must be positive).
   */
  void setResponseTimeoutInSec(int responseTimeoutInSec);

  /**
   * Checks whether retrieve requests carrying a query string are sent as a <tt>POST</tt> to a
   * <tt>/$query</tt> resource-path suffix with the query string moved into a <tt>text/plain</tt>
   * body, instead of as a plain <tt>GET</tt> with the query string in the URL. See OData 4.01 URL
   * Conventions, section 4.17.
   *
   * @return whether retrieve requests with a query string are sent via <tt>$query</tt> POST.
   */
  boolean isUseQueryPostRequest();

  /**
   * Sets whether retrieve requests carrying a query string are sent as a <tt>POST</tt> to a
   * <tt>/$query</tt> resource-path suffix with the query string moved into a <tt>text/plain</tt>
   * body, instead of as a plain <tt>GET</tt> with the query string in the URL. See OData 4.01 URL
   * Conventions, section 4.17. Retrieve requests without a query string are unaffected and always
   * remain plain <tt>GET</tt> requests.
   *
   * @param value 'TRUE' to send retrieve requests with a query string via <tt>$query</tt> POST.
   */
  void setUseQueryPostRequest(boolean value);
}
