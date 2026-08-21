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

import org.sitenetsoft.olinguito.server.api.ODataResponse;

/**
 * A unit of work the framework hands to an {@link AsyncSupport} implementation for execution off
 * the calling thread, as part of processing a {@code respond-async} preference (see
 * [OData-Protocol] section 11.6, Asynchronous Requests).
 *
 * <p>Invoking never throws: the framework has already converted any failure that occurred while
 * building the closure into an error {@link ODataResponse}, so the implementation only needs to
 * store whatever {@link #invoke()} returns.</p>
 */
@FunctionalInterface
public interface AsyncInvocation {

  /** Runs the deferred request processing and returns its outcome, success or error alike. */
  ODataResponse invoke();
}
