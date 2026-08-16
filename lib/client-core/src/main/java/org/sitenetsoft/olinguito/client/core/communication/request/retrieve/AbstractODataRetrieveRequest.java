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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 7: send retrieve requests carrying a query
 * string as a $query POST on demand (OData 4.01 URL Conventions section 4.17)
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 1 Task 7 fix round 1: set the $query POST
 * Content-Type at construction time (direct header write) instead of inside doExecute(), so
 * dispatch paths that never call this class's doExecute() - AsyncRequestWrapperImpl, and batch
 * serialization via AbstractODataRequest#toByteArray() - see the correct header too
 */
package org.sitenetsoft.olinguito.client.core.communication.request.retrieve;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpClient;
import org.sitenetsoft.olinguito.client.api.http.ODataHttpResponse;
import org.sitenetsoft.olinguito.client.api.communication.request.retrieve.ODataRetrieveRequest;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataRetrieveResponse;
import org.sitenetsoft.olinguito.client.core.communication.request.AbstractODataBasicRequest;
import org.sitenetsoft.olinguito.client.core.communication.response.AbstractODataResponse;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

/**
 * This is an abstract representation of an OData retrieve query request returning one or more result item.
 */
public abstract class AbstractODataRetrieveRequest<T>
        extends AbstractODataBasicRequest<ODataRetrieveResponse<T>>
        implements ODataRetrieveRequest<T> {

  /** OData 4.01 URL Conventions, section 4.17: the path suffix identifying a $query request. */
  private static final String QUERY_PATH_SEGMENT = "/$query";

  /**
   * The query string moved out of the URL and into the request body when this request is sent as a
   * <tt>$query</tt> POST (see {@link org.sitenetsoft.olinguito.client.api.Configuration
   * #isUseQueryPostRequest()}). <tt>null</tt> for a plain <tt>GET</tt> request.
   */
  private final String queryPostBody;

  /**
   * Constructor.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed.
   */
  public AbstractODataRetrieveRequest(final ODataClient odataClient, final URI query) {
    this(odataClient, query, isQueryPostRequest(odataClient, query));
  }

  /**
   * Delegate constructor: computes whether this request must be sent as a <tt>$query</tt> POST once,
   * then uses that single decision both to pick the HTTP method/URI passed to the superclass and to
   * decide whether a POST body needs to be kept around. When it is a <tt>$query</tt> POST, the
   * <tt>Content-Type</tt> header is also fixed to <tt>text/plain</tt> right here, at construction
   * time - see the note on {@link #odataHeaders} below for why that matters and why it is a direct
   * header write rather than a call to {@link #setContentType(String)}.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed, as originally built (with its query string, if any)
   * @param queryPost whether this request must be sent as a <tt>$query</tt> POST
   */
  private AbstractODataRetrieveRequest(final ODataClient odataClient, final URI query, final boolean queryPost) {
    super(odataClient, queryPost ? HttpMethod.POST : HttpMethod.GET, queryPost ? toQueryPostURI(query) : query);
    this.queryPostBody = queryPost ? query.getRawQuery() : null;
    if (queryPost) {
      // Fix round 1: this used to be a setContentType(...) call inside doExecute(), which only the
      // synchronous execute() path ever reaches. Two other dispatch paths read the Content-Type
      // header directly and never call this class's doExecute() at all:
      //  - AsyncRequestWrapperImpl's own doExecute() (a different, unrelated override on a
      //    different class) builds its own transport request from odataRequest.getHeaderNames()/
      //    getHeader(...), and its constructor additionally does
      //    odataRequest.setContentType(odataRequest.getContentType()) - a self-reassignment that,
      //    before this fix, read the still-unset header, fell back to the client's default format
      //    (e.g. application/json) via getContentType(), and then WROTE that default back onto the
      //    header, permanently locking in the wrong Content-Type before doExecute() ever had a
      //    chance to run.
      //  - Batch serialization (AbstractODataRequest#toByteArray(), called from
      //    AbstractODataBasicRequest#batch()) only fills in the default Content-Type "if not yet
      //    set" - it never calls doExecute() either.
      // Setting the header here, at construction, means every dispatch path observes the correct
      // value from the start, with no per-path special-casing needed.
      //
      // This is a direct write to the odataHeaders field (inherited, protected, from
      // AbstractODataRequest) rather than a call to the virtual setContentType(String) method,
      // because a subclass is free to override that method: AbstractMetadataRequestImpl does,
      // pinning it to a no-op so metadata responses always report application/xml. Metadata
      // requests never carry a query string (there is no $query use case for $metadata), so
      // queryPost is never true there and this code path is not reachable for that subclass today -
      // but writing the header field directly means the mechanism is correct on its own terms,
      // not merely correct by accident of which subclasses happen to reach it.
      odataHeaders.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
    }
  }

  /**
   * Whether the given request should be sent as a <tt>$query</tt> POST: the client is configured to
   * do so <em>and</em> the request URI actually carries a query string. A URI without a query string
   * always stays a plain <tt>GET</tt>, even when the configuration flag is on.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed
   * @return whether the request must be sent as a <tt>$query</tt> POST
   */
  private static boolean isQueryPostRequest(final ODataClient odataClient, final URI query) {
    final String rawQuery = query.getRawQuery();
    return odataClient.getConfiguration().isUseQueryPostRequest() && rawQuery != null && !rawQuery.isEmpty();
  }

  /**
   * Builds the <tt>$query</tt> POST target URI: the original path with the <tt>/$query</tt> segment
   * appended and the query string (and any fragment) removed. Works on the raw (already
   * percent-encoded) request URI text rather than reassembling URI components, so no component gets
   * re-encoded differently than it was originally.
   *
   * @param query the original request URI, known to carry a query string
   * @return the POST target URI
   */
  private static URI toQueryPostURI(final URI query) {
    final String raw = query.toASCIIString();
    final int queryIndex = raw.indexOf('?');
    final String withoutQuery = queryIndex == -1 ? raw : raw.substring(0, queryIndex);
    try {
      return new URI(withoutQuery + QUERY_PATH_SEGMENT);
    } catch (final URISyntaxException e) {
      throw new ODataRuntimeException("Unable to build the $query POST request URI", e);
    }
  }

  @Override
  public abstract ODataRetrieveResponse<T> execute();

  /**
   * This kind of request normally doesn't have any payload and null is returned. When this request is
   * being sent as a <tt>$query</tt> POST (see {@link
   * org.sitenetsoft.olinguito.client.api.Configuration#isUseQueryPostRequest()}), the query string
   * moved out of the URL is returned instead, UTF-8 encoded.
   */
  @Override
  public InputStream getPayload() {
    return queryPostBody == null ? null : new ByteArrayInputStream(queryPostBody.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Attaches the <tt>$query</tt> POST body, if any, before delegating to the regular synchronous
   * request execution. (The <tt>Content-Type</tt> header itself is already set, if needed, at
   * construction time - see the delegate constructor - so that it is also visible to dispatch paths,
   * such as the async wrapper and batch serialization, that never call this method.)
   */
  @Override
  protected ODataHttpResponse doExecute() {
    if (queryPostBody != null) {
      setRequestEntity(getPayload());
    }
    return super.doExecute();
  }

  /**
   * Response abstract class about an ODataRetrieveRequest.
   */
  protected abstract class AbstractODataRetrieveResponse
          extends AbstractODataResponse implements ODataRetrieveResponse<T> {

    protected AbstractODataRetrieveResponse(final ODataClient odataClient, final ODataHttpClient httpClient,
            final ODataHttpResponse res) {

      super(odataClient, httpClient, res);
    }

    @Override
    public abstract T getBody();
  }
}
