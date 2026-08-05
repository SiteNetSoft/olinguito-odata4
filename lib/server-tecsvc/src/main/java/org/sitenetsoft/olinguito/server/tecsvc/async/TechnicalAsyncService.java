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
 */
package org.sitenetsoft.olinguito.server.tecsvc.async;

import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.commons.api.format.PreferenceName;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.processor.Processor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
 * The TechnicalAsyncService provides asynchronous support for any Processor.
 * To use it following steps are necessary:
 * <ul>
 *   <li>Get the instance</li>
 *   <li>Create an instance of the Processor which should be wrapped for asynchronous support
 *   (do not forget to call the <code>init(...)</code> method on the processor)</li>
 *   <li>register the Processor instance via the <code>register(...)</code> method</li>
 *   <li>prepare the corresponding method with the request parameters via the
 *   <code>prepareFor()</code> method at the AsyncProcessor</li>
 *   <li>start the async processing via the <code>processAsync()</code> methods</li>
 * </ul>
 * A short code snippet is shown below:
 * <pre>
 * <code>
 * TechnicalAsyncService asyncService = TechnicalAsyncService.getInstance();
 * TechnicalEntityProcessor processor = new TechnicalEntityProcessor(dataProvider, serviceMetadata);
 * processor.init(odata, serviceMetadata);
 * AsyncProcessor<EntityProcessor> asyncProcessor = asyncService.register(processor, EntityProcessor.class);
 * asyncProcessor.prepareFor().readEntity(request, response, uriInfo, requestedFormat);
 * String location = asyncProcessor.processAsync();
 * </code>
 * </pre>
 */
public class TechnicalAsyncService {

  public static final String TEC_ASYNC_SLEEP = "tec.sleep";
  public static final String STATUS_MONITOR_TOKEN = "status";

  private static final Map<String, AsyncRunner> LOCATION_2_ASYNC_RUNNER =
      new ConcurrentHashMap<>();
  private static final ExecutorService ASYNC_REQUEST_EXECUTOR = Executors.newFixedThreadPool(10);
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  public <T extends Processor> AsyncProcessor<T> register(T processor, Class<T> processorInterface) {
    return new AsyncProcessor<T>(processor, processorInterface, this);
  }

  public static void updateHeader(ODataResponse response, HttpStatusCode status, String location) {
    response.setStatusCode(status.getStatusCode());
    response.setHeader(HttpHeader.LOCATION, location);
    response.setHeader(HttpHeader.PREFERENCE_APPLIED, PreferenceName.RESPOND_ASYNC.toString());

  }

  public static void acceptedResponse(ODataResponse response, String location) {
    updateHeader(response, HttpStatusCode.ACCEPTED, location);
  }

  private static final class AsyncProcessorHolder {
    private static final TechnicalAsyncService INSTANCE = new TechnicalAsyncService();
  }

  public static TechnicalAsyncService getInstance() {
    return AsyncProcessorHolder.INSTANCE;
  }

  public void shutdownThreadPool() {
    ASYNC_REQUEST_EXECUTOR.shutdown();
  }

  String processAsynchronous(AsyncProcessor<?> dispatchedProcessor)
      throws ODataApplicationException, ODataLibraryException {
    // use executor thread pool
    String location = createNewAsyncLocation(dispatchedProcessor.getRequest());
    dispatchedProcessor.setLocation(location);
    AsyncRunner run = new AsyncRunner(dispatchedProcessor);
    LOCATION_2_ASYNC_RUNNER.put(location, run);
    ASYNC_REQUEST_EXECUTOR.execute(run);
    //
    return location;
  }

  public AsyncRunner getRunner(String location) {
    return LOCATION_2_ASYNC_RUNNER.get(location);
  }

  public void removeRunner(String location) {
    LOCATION_2_ASYNC_RUNNER.remove(location);
  }

  public Map<String, AsyncRunner> getRunners() {
    return Collections.unmodifiableMap(LOCATION_2_ASYNC_RUNNER);
  }

  static void copy(final InputStream input, final OutputStream output) {
    if (output == null || input == null) {
      return;
    }

    try (ReadableByteChannel ic = Channels.newChannel(input);
         WritableByteChannel oc = Channels.newChannel(output)) {
      ByteBuffer inBuffer = ByteBuffer.allocate(8192);
      while (ic.read(inBuffer) > 0) {
        inBuffer.flip();
        oc.write(inBuffer);
        inBuffer.rewind();
      }
    } catch (IOException e) {
      throw new ODataRuntimeException("Error on reading request content");
    }
  }

  private String createNewAsyncLocation(ODataRequest request) {
    int pos = request.getRawBaseUri().lastIndexOf("/") + 1;
    return request.getRawBaseUri().substring(0, pos) + STATUS_MONITOR_TOKEN + "/" + ID_GENERATOR.incrementAndGet();
  }

  /**
   * Runnable for the AsyncProcessor.
   */
  public static class AsyncRunner implements Runnable {
    private static final Pattern PATTERN = Pattern.compile("(" + TEC_ASYNC_SLEEP + "=)(\\d*)");
    private final AsyncProcessor<? extends Processor> dispatched;
    private int defaultSleepTimeInSeconds = 0;
    private Exception exception;
    boolean finished = false;

    public AsyncRunner(AsyncProcessor<? extends Processor> wrap) {
      this(wrap, 0);
    }

    public AsyncRunner(AsyncProcessor<? extends Processor> wrap, int defaultSleepTimeInSeconds) {
      this.dispatched = wrap;
      if (defaultSleepTimeInSeconds > 0) {
        this.defaultSleepTimeInSeconds = defaultSleepTimeInSeconds;
      }
    }

    @Override
    public void run() {
      try {
        int sleep = getSleepTime(dispatched);
        TimeUnit.SECONDS.sleep(sleep);
        dispatched.process();
      } catch (final InterruptedException e) {
        exception = e;
      } catch (final InvocationTargetException e) {
        exception = e;
      } catch (final IllegalAccessException e) {
        exception = e;
      }
      finished = true;
    }

    private int getSleepTime(AsyncProcessor<? extends Processor> wrap) {
      String preferHeader = wrap.getPreferHeader();
      Matcher matcher = PATTERN.matcher(preferHeader);
      if (matcher.find()) {
        String waitTimeAsString = matcher.group(2);
        return Integer.parseInt(waitTimeAsString);
      }
      return defaultSleepTimeInSeconds;
    }

    public Exception getException() {
      return exception;
    }

    public boolean isFinished() {
      return finished;
    }

    public AsyncProcessor<? extends Processor> getDispatched() {
      return dispatched;
    }
  }
}
