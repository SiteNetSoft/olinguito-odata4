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
 * Copyright 2026 SiteNetSoft - Added fluent builder() API with adapter selection
 */
package org.sitenetsoft.olinguito.client.core;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;

import org.sitenetsoft.olinguito.client.api.Configuration;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.http.HttpClientFactory;
import org.sitenetsoft.olinguito.client.api.http.HttpUriRequestFactory;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpClientFactory;
import org.sitenetsoft.olinguito.client.core.http.DefaultHttpUriRequestFactory;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;

public final class ODataClientFactory {

  public static ODataClient getClient() {
    return new ODataClientImpl();
  }

  public static EdmEnabledODataClient getEdmEnabledClient(final String serviceRoot) {
    return getEdmEnabledClient(serviceRoot, null, null, ContentType.JSON);
  }

  public static EdmEnabledODataClient getEdmEnabledClient(
      final String serviceRoot, ContentType contentType) {
    return getEdmEnabledClient(serviceRoot, null, null, contentType);
  }

  public static EdmEnabledODataClient getEdmEnabledClient(
      final String serviceRoot, final Edm edm, final String metadataETag) {
    return getEdmEnabledClient(serviceRoot, edm, metadataETag, ContentType.JSON);
  }

  public static EdmEnabledODataClient getEdmEnabledClient(
      final String serviceRoot, final Edm edm,
      final String metadataETag, ContentType contentType) {

    final EdmEnabledODataClient instance =
        new EdmEnabledODataClientImpl(serviceRoot, edm, metadataETag);
    instance.getConfiguration().setDefaultPubFormat(contentType);
    return instance;
  }

  /**
   * Returns a fluent builder for constructing {@link ODataClient} instances
   * with a chosen HTTP backend and configuration.
   *
   * <pre>{@code
   * // Apache HttpClient (default)
   * ODataClient client = ODataClientFactory.builder()
   *     .withApache()
   *     .gzipCompression(true)
   *     .build();
   *
   * // OkHttp (requires client-adapter-okhttp on classpath)
   * ODataClient client = ODataClientFactory.builder()
   *     .withOkHttp()
   *     .gzipCompression(true)
   *     .build();
   *
   * // Custom factories
   * ODataClient client = ODataClientFactory.builder()
   *     .httpClientFactory(myFactory)
   *     .httpUriRequestFactory(myRequestFactory)
   *     .build();
   * }</pre>
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  private ODataClientFactory() {
    // empty constructor for static utility class
  }

  /**
   * Fluent builder for {@link ODataClient} with HTTP adapter selection.
   */
  public static final class Builder {

    private static final String OKHTTP_CLIENT_FACTORY =
        "org.sitenetsoft.olinguito.client.adapter.okhttp.OkHttpClientFactory";
    private static final String OKHTTP_URI_REQUEST_FACTORY =
        "org.sitenetsoft.olinguito.client.adapter.okhttp.OkHttpUriRequestFactory";

    private HttpClientFactory clientFactory;
    private HttpUriRequestFactory requestFactory;
    private ContentType defaultPubFormat;
    private ContentType defaultValueFormat;
    private ContentType defaultMediaFormat;
    private ContentType defaultBatchAcceptFormat;
    private Boolean gzipCompression;
    private Boolean useChunked;
    private Boolean keyAsSegment;
    private Boolean useXHttpMethod;
    private Boolean continueOnError;
    private ExecutorService executor;

    private Builder() {
    }

    /**
     * Selects the Apache HttpClient backend (the default).
     *
     * @return this builder
     */
    public Builder withApache() {
      this.clientFactory = new DefaultHttpClientFactory();
      this.requestFactory = new DefaultHttpUriRequestFactory();
      return this;
    }

    /**
     * Selects the OkHttp backend. Requires {@code odata-client-adapter-okhttp}
     * on the classpath; throws {@link IllegalStateException} if not found.
     *
     * @return this builder
     * @throws IllegalStateException if the OkHttp adapter is not available
     */
    public Builder withOkHttp() {
      try {
        final Class<?> clientFactoryClass =
            Class.forName(OKHTTP_CLIENT_FACTORY);
        final Class<?> requestFactoryClass =
            Class.forName(OKHTTP_URI_REQUEST_FACTORY);
        this.clientFactory = (HttpClientFactory) clientFactoryClass
            .getDeclaredConstructor().newInstance();
        this.requestFactory = (HttpUriRequestFactory) requestFactoryClass
            .getDeclaredConstructor().newInstance();
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            "OkHttp adapter not found on classpath. "
                + "Add odata-client-adapter-okhttp dependency.", e);
      } catch (NoSuchMethodException | InvocationTargetException
               | InstantiationException | IllegalAccessException e) {
        throw new IllegalStateException(
            "Failed to instantiate OkHttp adapter factories.", e);
      }
      return this;
    }

    /**
     * Sets a custom {@link HttpClientFactory}.
     *
     * @param factory the factory to use
     * @return this builder
     */
    public Builder httpClientFactory(final HttpClientFactory factory) {
      this.clientFactory = factory;
      return this;
    }

    /**
     * Sets a custom {@link HttpUriRequestFactory}.
     *
     * @param factory the factory to use
     * @return this builder
     */
    public Builder httpUriRequestFactory(
        final HttpUriRequestFactory factory) {
      this.requestFactory = factory;
      return this;
    }

    /**
     * Enables or disables GZIP compression for requests.
     *
     * @param enabled {@code true} to enable
     * @return this builder
     */
    public Builder gzipCompression(final boolean enabled) {
      this.gzipCompression = enabled;
      return this;
    }

    /**
     * Sets the default publication format.
     *
     * @param contentType the default content type for entity payloads
     * @return this builder
     */
    public Builder defaultPubFormat(final ContentType contentType) {
      this.defaultPubFormat = contentType;
      return this;
    }

    /**
     * Sets the default value format.
     *
     * @param contentType the default content type for values
     * @return this builder
     */
    public Builder defaultValueFormat(final ContentType contentType) {
      this.defaultValueFormat = contentType;
      return this;
    }

    /**
     * Sets the default media format.
     *
     * @param contentType the default content type for media
     * @return this builder
     */
    public Builder defaultMediaFormat(final ContentType contentType) {
      this.defaultMediaFormat = contentType;
      return this;
    }

    /**
     * Sets the default batch accept format.
     *
     * @param contentType the default accept type for batch
     * @return this builder
     */
    public Builder defaultBatchAcceptFormat(
        final ContentType contentType) {
      this.defaultBatchAcceptFormat = contentType;
      return this;
    }

    /**
     * Enables or disables chunked transfer encoding.
     *
     * @param enabled {@code true} to use chunked encoding
     * @return this builder
     */
    public Builder useChunked(final boolean enabled) {
      this.useChunked = enabled;
      return this;
    }

    /**
     * Enables or disables key-as-segment URL convention.
     *
     * @param enabled {@code true} to use key-as-segment
     * @return this builder
     */
    public Builder keyAsSegment(final boolean enabled) {
      this.keyAsSegment = enabled;
      return this;
    }

    /**
     * Enables or disables X-HTTP-Method override.
     *
     * @param enabled {@code true} to use X-HTTP-Method header
     * @return this builder
     */
    public Builder useXHttpMethod(final boolean enabled) {
      this.useXHttpMethod = enabled;
      return this;
    }

    /**
     * Enables or disables continue-on-error for batch requests.
     *
     * @param enabled {@code true} to continue on error
     * @return this builder
     */
    public Builder continueOnError(final boolean enabled) {
      this.continueOnError = enabled;
      return this;
    }

    /**
     * Sets the executor service for async operations.
     *
     * @param executorService the executor to use
     * @return this builder
     */
    public Builder executor(final ExecutorService executorService) {
      this.executor = executorService;
      return this;
    }

    /**
     * Builds a new {@link ODataClient} with the configured settings.
     *
     * @return a configured ODataClient
     */
    public ODataClient build() {
      final ODataClient client = new ODataClientImpl();
      applyConfiguration(client.getConfiguration());
      return client;
    }

    /**
     * Builds an {@link EdmEnabledODataClient} with the configured settings.
     *
     * @param serviceRoot the OData service root URL
     * @return a configured EdmEnabledODataClient
     */
    public EdmEnabledODataClient buildEdmEnabled(
        final String serviceRoot) {
      return buildEdmEnabled(serviceRoot, null, null);
    }

    /**
     * Builds an {@link EdmEnabledODataClient} with pre-fetched EDM.
     *
     * @param serviceRoot  the OData service root URL
     * @param edm          pre-fetched EDM (may be {@code null})
     * @param metadataETag metadata ETag (may be {@code null})
     * @return a configured EdmEnabledODataClient
     */
    public EdmEnabledODataClient buildEdmEnabled(
        final String serviceRoot,
        final Edm edm, final String metadataETag) {
      final EdmEnabledODataClient client =
          new EdmEnabledODataClientImpl(serviceRoot, edm, metadataETag);
      applyConfiguration(client.getConfiguration());
      return client;
    }

    private void applyConfiguration(final Configuration config) {
      if (clientFactory != null) {
        config.setHttpClientFactory(clientFactory);
      }
      if (requestFactory != null) {
        config.setHttpUriRequestFactory(requestFactory);
      }
      if (defaultPubFormat != null) {
        config.setDefaultPubFormat(defaultPubFormat);
      }
      if (defaultValueFormat != null) {
        config.setDefaultValueFormat(defaultValueFormat);
      }
      if (defaultMediaFormat != null) {
        config.setDefaultMediaFormat(defaultMediaFormat);
      }
      if (defaultBatchAcceptFormat != null) {
        config.setDefaultBatchAcceptFormat(defaultBatchAcceptFormat);
      }
      if (gzipCompression != null) {
        config.setGzipCompression(gzipCompression);
      }
      if (useChunked != null) {
        config.setUseChuncked(useChunked);
      }
      if (keyAsSegment != null) {
        config.setKeyAsSegment(keyAsSegment);
      }
      if (useXHttpMethod != null) {
        config.setUseXHTTPMethod(useXHttpMethod);
      }
      if (continueOnError != null) {
        config.setContinueOnError(continueOnError);
      }
      if (executor != null) {
        config.setExecutor(executor);
      }
    }
  }
}
