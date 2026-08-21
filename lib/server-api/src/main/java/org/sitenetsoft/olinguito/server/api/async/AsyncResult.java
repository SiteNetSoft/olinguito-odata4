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
 * The immutable outcome of resolving a status monitor request against an {@link AsyncSupport}
 * implementation, as described in [OData-Protocol] section 11.6 (Asynchronous Requests).
 */
public final class AsyncResult {

  /** The state of a status monitor resource. */
  public enum State {
    /** The invocation has not completed yet. */
    RUNNING,
    /** The invocation completed and its response is available. */
    COMPLETED,
    /** The status monitor resource this request addressed does not exist (any more). */
    NOT_FOUND
  }

  private final State state;
  private final ODataResponse response;

  private AsyncResult(final State state, final ODataResponse response) {
    this.state = state;
    this.response = response;
  }

  /** A result reporting that the invocation is still running. */
  public static AsyncResult running() {
    return new AsyncResult(State.RUNNING, null);
  }

  /**
   * A result reporting that the invocation completed, carrying its response.
   *
   * @throws IllegalArgumentException if {@code response} is {@code null} — a completed result
   *         without a response is a programming error the monitor path cannot render.
   */
  public static AsyncResult completed(final ODataResponse response) {
    if (response == null) {
      throw new IllegalArgumentException("A completed AsyncResult requires a non-null response.");
    }
    return new AsyncResult(State.COMPLETED, response);
  }

  /** A result reporting that the status monitor resource does not exist (any more). */
  public static AsyncResult notFound() {
    return new AsyncResult(State.NOT_FOUND, null);
  }

  /** The state of the status monitor resource. */
  public State getState() {
    return state;
  }

  /** The completed invocation's response, or {@code null} unless the state is {@link State#COMPLETED}. */
  public ODataResponse getResponse() {
    return response;
  }
}
