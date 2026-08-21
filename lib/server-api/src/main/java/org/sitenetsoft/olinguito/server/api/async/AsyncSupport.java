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
 * Copyright 2026 SiteNetSoft - Added the asynchronous-processing service provider interface
 */
package org.sitenetsoft.olinguito.server.api.async;

import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.OlingoExtension;

/**
 * Service provider interface a service implements to let the framework process requests
 * asynchronously, as described in [OData-Protocol] section 11.6 (Asynchronous Requests) and section
 * 8.2.8.8 (Preference respond-async).
 *
 * <p>Register an implementation with {@code ODataHandler.register(OlingoExtension)}. With none
 * registered, the {@code respond-async} preference is ignored and not echoed, which is what a
 * service that does not support asynchronous processing must do: section 8.2.8.8 makes processing
 * asynchronously a MAY, and section 11.6 forbids answering 202 without the preference — never the
 * other way round.</p>
 *
 * <p>The implementation owns three things: where results are kept, on what thread they are
 * produced, and what the status monitor's URL looks like. Everything else — the 202 response, the
 * {@code Location} and {@code Preference-Applied} headers, the monitor's status codes and both
 * result representations — belongs to the framework and is not this interface's business.</p>
 */
public interface AsyncSupport extends OlingoExtension {

  /**
   * Whether this request addresses a status monitor resource this service minted.
   *
   * <p>Called before the request is parsed as an OData URI, because a monitor URL need not be one:
   * section 11.6 requires only that "the status monitor resource URL MUST differ from any other
   * resource URL".</p>
   */
  boolean isStatusMonitorRequest(ODataRequest request);

  /**
   * Accepts an invocation for asynchronous execution and returns the absolute URL of the status
   * monitor resource that will report on it — the value the framework puts in the {@code Location}
   * header of its 202 Accepted response (section 11.6, and section 8.3.3).
   *
   * <p>The invocation must not be run on the calling thread: the caller is about to return 202.</p>
   */
  String submit(ODataRequest request, AsyncInvocation invocation);

  /** The current state of the monitor resource this request addresses. */
  AsyncResult resolve(ODataRequest request);

  /**
   * Cancels the invocation this monitor request addresses, returning whether cancellation happened.
   *
   * <p>Section 11.6: a DELETE to the status monitor requests cancellation, and "if a delete request
   * is not supported by the service, the service returns 405 Method Not Allowed" — which is what
   * the framework answers when this returns {@code false}, its default. A service that returns
   * {@code true} guarantees section 11.6's "no observable change has occurred as a result of a
   * canceled request".</p>
   */
  default boolean cancel(final ODataRequest request) {
    return false;
  }

  /**
   * The value, in seconds, for the optional {@code Retry-After} header of a 202 response, or
   * {@code null} to omit it.
   *
   * <p>Section 8.3.7 makes the header a MAY and specifies only "the duration of time, in seconds";
   * there is no specified format beyond that, so the value is the service's to choose.</p>
   */
  default Integer getRetryAfter() {
    return null;
  }
}
