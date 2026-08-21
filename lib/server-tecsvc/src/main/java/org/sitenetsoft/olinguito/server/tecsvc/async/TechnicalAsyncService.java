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
 * Copyright 2026 SiteNetSoft - Removed servlet-dependent methods to make engine-agnostic
 * Copyright 2026 SiteNetSoft - Replaced synchronizedMap with ConcurrentHashMap
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 10: implement the AsyncSupport service provider
 * interface ([OData-Protocol] section 11.6) so the framework, not the processors, owns the
 * respond-async preference; the dynamic-proxy processor wrapping is gone
 */
package org.sitenetsoft.olinguito.server.tecsvc.async;

import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.async.AsyncInvocation;
import org.sitenetsoft.olinguito.server.api.async.AsyncResult;
import org.sitenetsoft.olinguito.server.api.async.AsyncSupport;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asynchronous processing support for the technical service, as an {@link AsyncSupport}
 * implementation registered on the request handler.
 *
 * <p>The framework recognizes a status monitor request, hands over a ready-made
 * {@link AsyncInvocation} for the requests that carry the <code>respond-async</code> preference and
 * renders every response itself; this class only owns where results are kept, on what thread they
 * are produced and what the monitor URL looks like ([OData-Protocol] section 11.6).</p>
 *
 * <p>A result is retrieved exactly once: {@link #resolve(ODataRequest)} removes the runner when it
 * reports the completed response, so a second request to the same monitor URL is answered 404. That
 * is the technical service's long-standing behavior, kept deliberately; section 11.6 makes 404
 * correct after a successful cancellation or once the service stops retaining the result.</p>
 *
 * <p>For testing purposes the runner honors a <code>tec.sleep=&lt;seconds&gt;</code> token in the
 * raw <code>Prefer</code> header, which delays the invocation so that a client can observe the
 * running state of the monitor resource.</p>
 */
public class TechnicalAsyncService implements AsyncSupport {

  public static final String TEC_ASYNC_SLEEP = "tec.sleep";
  public static final String STATUS_MONITOR_TOKEN = "status";

  private static final Map<String, AsyncRunner> LOCATION_2_ASYNC_RUNNER =
      new ConcurrentHashMap<>();
  private static final ExecutorService ASYNC_REQUEST_EXECUTOR = Executors.newFixedThreadPool(10);
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private static final class AsyncProcessorHolder {
    private static final TechnicalAsyncService INSTANCE = new TechnicalAsyncService();
  }

  public static TechnicalAsyncService getInstance() {
    return AsyncProcessorHolder.INSTANCE;
  }

  public void shutdownThreadPool() {
    ASYNC_REQUEST_EXECUTOR.shutdown();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The monitor URLs this service mints all have a <code>/status/</code> path segment, which no
   * other resource of the technical service has, satisfying section 11.6's requirement that "the
   * status monitor resource URL MUST differ from any other resource URL".</p>
   */
  @Override
  public boolean isStatusMonitorRequest(final ODataRequest request) {
    final String uri = request.getRawRequestUri();
    return uri != null && uri.contains("/" + STATUS_MONITOR_TOKEN + "/");
  }

  @Override
  public String submit(final ODataRequest request, final AsyncInvocation invocation) {
    final String location = createNewAsyncLocation(request);
    final AsyncRunner runner = new AsyncRunner(invocation, getSleepTime(request));
    LOCATION_2_ASYNC_RUNNER.put(location, runner);
    ASYNC_REQUEST_EXECUTOR.execute(runner);
    return location;
  }

  @Override
  public AsyncResult resolve(final ODataRequest request) {
    final String location = request.getRawRequestUri();
    final AsyncRunner runner = LOCATION_2_ASYNC_RUNNER.get(location);
    if (runner == null) {
      return AsyncResult.notFound();
    }
    if (!runner.isFinished()) {
      return AsyncResult.running();
    }
    LOCATION_2_ASYNC_RUNNER.remove(location);
    final ODataResponse response = runner.getResponse();
    // An invocation never throws (the framework turns failures into an error response), so a
    // finished runner without a response can only mean the executor thread was interrupted; there
    // is nothing left to report on, which is what a missing monitor resource means.
    return response == null ? AsyncResult.notFound() : AsyncResult.completed(response);
  }

  /** The runners currently known to this service, keyed by their monitor location. */
  public Map<String, AsyncRunner> getRunners() {
    return Collections.unmodifiableMap(LOCATION_2_ASYNC_RUNNER);
  }

  private String createNewAsyncLocation(ODataRequest request) {
    int pos = request.getRawBaseUri().lastIndexOf("/") + 1;
    return request.getRawBaseUri().substring(0, pos) + STATUS_MONITOR_TOKEN + "/" + ID_GENERATOR.incrementAndGet();
  }

  /**
   * Reads the test-only <code>tec.sleep=&lt;seconds&gt;</code> token out of the raw
   * <code>Prefer</code> header, returning zero when it is absent.
   */
  private static int getSleepTime(final ODataRequest request) {
    final String preferHeader = request.getHeader(HttpHeader.PREFER);
    if (preferHeader == null) {
      return 0;
    }
    final Matcher matcher = AsyncRunner.SLEEP_PATTERN.matcher(preferHeader);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(2));
    }
    return 0;
  }

  /**
   * Runs one deferred invocation and keeps its response until the monitor resource is read.
   */
  public static class AsyncRunner implements Runnable {
    static final Pattern SLEEP_PATTERN = Pattern.compile("(" + TEC_ASYNC_SLEEP + "=)(\\d+)");

    private final AsyncInvocation invocation;
    private final int sleepTimeInSeconds;
    private volatile ODataResponse result;
    private volatile Exception exception;
    private volatile boolean finished = false;

    public AsyncRunner(final AsyncInvocation invocation, final int sleepTimeInSeconds) {
      this.invocation = invocation;
      this.sleepTimeInSeconds = Math.max(sleepTimeInSeconds, 0);
    }

    @Override
    public void run() {
      try {
        if (sleepTimeInSeconds > 0) {
          TimeUnit.SECONDS.sleep(sleepTimeInSeconds);
        }
        result = invocation.invoke();
      } catch (final InterruptedException e) {
        exception = e;
        Thread.currentThread().interrupt();
      } catch (final RuntimeException e) {
        exception = e;
      } finally {
        finished = true;
      }
    }

    /** The response the invocation produced, or {@code null} if it did not complete normally. */
    public ODataResponse getResponse() {
      return result;
    }

    public Exception getException() {
      return exception;
    }

    public boolean isFinished() {
      return finished;
    }
  }
}
