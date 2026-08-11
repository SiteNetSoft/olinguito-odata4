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
 * Copyright 2026 SiteNetSoft - Migrate from deprecated DefaultHttpClient to HttpClientBuilder
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 * Copyright 2026 SiteNetSoft - Fixed deprecated HC 5.x execute() calls
 * Copyright 2026 SiteNetSoft - Fixed HC 5.x resource leaks: shared HttpClient, close responses
 * Copyright 2026 SiteNetSoft - Replaced wildcard imports with explicit imports
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Cover the dynamic (open-type) property 404 pre-check in
 * ServiceDispatcher#internalExecute
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.core.Encoder;
import org.sitenetsoft.olinguito.server.adapter.servlet.OData4HttpHandler;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.core.requests.ActionRequest;
import org.sitenetsoft.olinguito.server.core.requests.DataRequest;
import org.sitenetsoft.olinguito.server.core.requests.FunctionRequest;
import org.sitenetsoft.olinguito.server.core.requests.MediaRequest;
import org.sitenetsoft.olinguito.server.core.requests.MetadataRequest;
import org.sitenetsoft.olinguito.server.core.responses.CountResponse;
import org.sitenetsoft.olinguito.server.core.responses.EntityResponse;
import org.sitenetsoft.olinguito.server.core.responses.EntitySetResponse;
import org.sitenetsoft.olinguito.server.core.responses.ErrorResponse;
import org.sitenetsoft.olinguito.server.core.responses.MetadataResponse;
import org.sitenetsoft.olinguito.server.core.responses.NoContentResponse;
import org.sitenetsoft.olinguito.server.core.responses.PrimitiveValueResponse;
import org.sitenetsoft.olinguito.server.core.responses.PropertyResponse;
import org.sitenetsoft.olinguito.server.core.responses.StreamResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceDispatcherTest {
  private static final int TOMCAT_PORT = 9900;
  private Tomcat tomcat = new Tomcat();
  private CloseableHttpClient http;

  public class SampleODataServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private final ServiceHandler handler; // must be stateless
    private final ServiceMetadata metadata; // must be stateless

    public SampleODataServlet(ServiceHandler handler, ServiceMetadata metadata) {
      this.handler = handler;
      this.metadata = metadata;
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      OData odata = OData4Impl.newInstance();

      OData4HttpHandler handler = new OData4HttpHandler(odata, metadata);

      handler.register(this.handler);
      handler.process(request, response);
    }
  }

  public void beforeTest(ServiceHandler serviceHandler) throws Exception {
    MetadataParser parser = new MetadataParser();
    parser.parseAnnotations(true);
    parser.useLocalCoreVocabularies(true);
    parser.implicitlyLoadCoreVocabularies(true);

    InputStream in = getClass().getClassLoader().getResourceAsStream("trippin/trippin.xml");
    assertNotNull(in, "trippin/trippin.xml not found on test classpath");
    ServiceMetadata metadata = parser.buildServiceMetadata(new InputStreamReader(in, StandardCharsets.UTF_8));

    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    tomcat.setBaseDir(baseDir.getAbsolutePath());
    tomcat.getHost().setAppBase(baseDir.getAbsolutePath());
    Context cxt = tomcat.addContext("/trippin", baseDir.getAbsolutePath());
    Tomcat.addServlet(cxt, "trippin", new SampleODataServlet(serviceHandler, metadata));
    cxt.addServletMappingDecoded("/*", "trippin");
    tomcat.setPort(TOMCAT_PORT);
    tomcat.getConnector().setSecure(false);
    tomcat.start();
    http = HttpClientBuilder.create().build();
  }

  @AfterEach
  void afterTest() throws Exception {
    if (http != null) {
      http.close();
    }
    tomcat.stop();
    tomcat.destroy();
  }

  interface TestResult {
    void validate() throws Exception;
  }

  private HttpHost getLocalhost() {
    return new HttpHost(tomcat.getHost().getName(), 9900);
  }

  private ClassicHttpResponse httpGET(String url) throws Exception{
    ClassicHttpRequest request = new HttpGet(url);
    return httpSend(request);
  }

  private ClassicHttpResponse httpSend(ClassicHttpRequest request) throws Exception{
    return http.executeOpen(getLocalhost(), request, null);
  }

  private void helpGETTest(ServiceHandler handler, String path, TestResult validator)
      throws Exception {
    beforeTest(handler);
    try (ClassicHttpResponse response = httpGET("http://localhost:" + TOMCAT_PORT + "/" + path)) {
      validator.validate();
    }
  }

  private void helpTest(ServiceHandler handler, String path, String method, String payload,
      TestResult validator) throws Exception {
    beforeTest(handler);

    String editUrl = "http://localhost:" + TOMCAT_PORT + "/" + path;
    ClassicHttpRequest request = new HttpGet(editUrl);
    if (method.equals("POST")) {
      HttpPost post = new HttpPost(editUrl);
      post.setEntity(new StringEntity(payload));
      request = post;
    } else if (method.equals("PUT")) {
      HttpPut put = new HttpPut(editUrl);
      put.setEntity(new StringEntity(payload));
      request = put;
    } else if (method.equals("DELETE")) {
      HttpDelete delete = new HttpDelete(editUrl);
      request = delete;
    }
    request.setHeader("Content-Type", "application/json;odata.metadata=minimal");
    try (ClassicHttpResponse response = http.executeOpen(getLocalhost(), request, null)) {
      // response consumed and closed
    }

    validator.validate();
  }

  @Test
  void testMetadata() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/$metadata", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<MetadataRequest> arg1 = ArgumentCaptor.forClass(MetadataRequest.class);
        ArgumentCaptor<MetadataResponse> arg2 = ArgumentCaptor.forClass(MetadataResponse.class);
        Mockito.verify(handler).readMetadata(arg1.capture(), arg2.capture());
      }
    });
  }

  @Test
  void testEntitySet() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<EntitySetResponse> arg2 = ArgumentCaptor.forClass(EntitySetResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        // Need getName on ContextURL class
        // assertEquals("",
        // request.getContextURL(request.getOdata()).getName());
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  void testEntitySetCount() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports/$count", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<CountResponse> arg2 = ArgumentCaptor.forClass(CountResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        // Need getName on ContextURL class
        // assertEquals("",
        // request.getContextURL(request.getOdata()).getName());
        Assertions.assertEquals("text/plain", request.getResponseContentType().toContentTypeString());
      }
    });
  }

  @Test
  void testEntity() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports('0')", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<EntityResponse> arg2 = ArgumentCaptor.forClass(EntityResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertEquals(1, request.getUriResourceEntitySet().getKeyPredicates().size());
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  void testReadProperty() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports('0')/IataCode", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<PropertyResponse> arg2 = ArgumentCaptor.forClass(PropertyResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertTrue(request.isPropertyRequest());
        Assertions.assertFalse(request.isPropertyComplex());
        Assertions.assertEquals(1, request.getUriResourceEntitySet().getKeyPredicates().size());
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  void testReadComplexProperty() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports('0')/Location", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<PropertyResponse> arg2 = ArgumentCaptor.forClass(PropertyResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertTrue(request.isPropertyRequest());
        Assertions.assertTrue(request.isPropertyComplex());
        Assertions.assertEquals(1, request.getUriResourceEntitySet().getKeyPredicates().size());
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  public void testReadProperty$Value() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports('0')/IataCode/$value", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<PrimitiveValueResponse> arg2 = ArgumentCaptor
            .forClass(PrimitiveValueResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertTrue(request.isPropertyRequest());
        Assertions.assertFalse(request.isPropertyComplex());
        Assertions.assertEquals(1, request.getUriResourceEntitySet().getKeyPredicates().size());
        Assertions.assertEquals("text/plain", request.getResponseContentType().toContentTypeString());
      }
    });
  }

  @Test
  void testReadPropertyRef() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Airports('0')/IataCode/$value", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<PrimitiveValueResponse> arg2 = ArgumentCaptor
            .forClass(PrimitiveValueResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertTrue(request.isPropertyRequest());
        Assertions.assertFalse(request.isPropertyComplex());
        Assertions.assertEquals(1, request.getUriResourceEntitySet().getKeyPredicates().size());
        Assertions.assertEquals("text/plain", request.getResponseContentType().toContentTypeString());
      }
    });
  }

  @Test
  void testFunctionImport() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/GetNearestAirport(lat=12.11,lon=34.23)", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<FunctionRequest> arg1 = ArgumentCaptor.forClass(FunctionRequest.class);
        ArgumentCaptor<EntityResponse> arg3 = ArgumentCaptor.forClass(EntityResponse.class);
        ArgumentCaptor<HttpMethod> arg2 = ArgumentCaptor.forClass(HttpMethod.class);
        Mockito.verify(handler).invoke(arg1.capture(), arg2.capture(), arg3.capture());

        arg1.getValue();
      }
    });
  }

  @Test
  void testActionImport() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpTest(handler, "trippin/ResetDataSource", "POST", "", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<ActionRequest> arg1 = ArgumentCaptor.forClass(ActionRequest.class);
        ArgumentCaptor<NoContentResponse> arg2 = ArgumentCaptor.forClass(NoContentResponse.class);
        Mockito.verify(handler).invoke(arg1.capture(), Mockito.anyString(), arg2.capture());

        arg1.getValue();
      }
    });
  }

  @Test
  void testReadMedia() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/Photos(1)/$value", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<MediaRequest> arg1 = ArgumentCaptor.forClass(MediaRequest.class);
        ArgumentCaptor<StreamResponse> arg2 = ArgumentCaptor.forClass(StreamResponse.class);
        Mockito.verify(handler).readMediaStream(arg1.capture(), arg2.capture());

        MediaRequest request = arg1.getValue();
        Assertions.assertEquals("application/octet-stream", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  void testReadUnknownDynamicPropertySegmentReturns404() throws Exception {
    // Person is declared OpenType="true" in trippin.xml, so "Unknown" parses as a
    // UriResourceDynamicProperty rather than failing at parse time - but this legacy
    // ServiceHandler-based dispatcher has no way to serve a dynamic property's value, so
    // ServiceDispatcher#internalExecute's pre-check must reject it with 404 before any read/
    // invoke handler method is invoked. processError is stubbed to actually write the error
    // (mirroring the real TripPinHandler used elsewhere in this package) since a bare Mockito
    // mock's void methods otherwise no-op, leaving the response empty rather than a 404.
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    Mockito.doAnswer(invocation -> {
      final ODataServerError error = invocation.getArgument(0);
      final ErrorResponse response = invocation.getArgument(1);
      response.writeError(error);
      return null;
    }).when(handler).processError(Mockito.any(), Mockito.any());
    beforeTest(handler);
    try (ClassicHttpResponse response = httpGET(
        "http://localhost:" + TOMCAT_PORT + "/trippin/People('russelwhyte')/Unknown")) {
      Assertions.assertEquals(404, response.getCode());
    }
    Mockito.verify(handler, Mockito.never()).read(Mockito.any(), Mockito.any());
  }

  @Test
  void testReadNavigation() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/People('russelwhyte')/Friends", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<EntitySetResponse> arg2 = ArgumentCaptor.forClass(EntitySetResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  void testReadReference() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/People('russelwhyte')/Friends/$ref", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<EntitySetResponse> arg2 = ArgumentCaptor.forClass(EntitySetResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }

  @Test
  void testWriteReferenceCollection() throws Exception {
    String payload = "{\n" + "\"@odata.id\": \"/Photos(11)\"\n" + "}";

    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpTest(handler, "trippin/People('russelwhyte')/Friends/$ref", "POST", payload,
        new TestResult() {
          @Override
          public void validate() throws Exception {
            ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
            ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<URI> arg3 = ArgumentCaptor.forClass(URI.class);
            ArgumentCaptor<NoContentResponse> arg4 = ArgumentCaptor
                .forClass(NoContentResponse.class);
            Mockito.verify(handler).addReference(arg1.capture(), arg2.capture(), arg3.capture(),
                arg4.capture());

            DataRequest request = arg1.getValue();
            Assertions.assertEquals("application/json;odata.metadata=minimal", request
                .getResponseContentType().toContentTypeString());
          }
        });
  }

  @Test
  void testWriteReference() throws Exception {
    String payload = "{\n" + "\"@odata.id\": \"/Photos(11)\"\n" + "}";

    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpTest(handler, "trippin/People('russelwhyte')/Friends('someone')/Photo/$ref", "PUT", payload,
        new TestResult() {
          @Override
          public void validate() throws Exception {
            ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
            ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<URI> arg3 = ArgumentCaptor.forClass(URI.class);
            ArgumentCaptor<NoContentResponse> arg4 = ArgumentCaptor
                .forClass(NoContentResponse.class);
            Mockito.verify(handler).updateReference(arg1.capture(), arg2.capture(), arg3.capture(),
                arg4.capture());

            DataRequest request = arg1.getValue();
            Assertions.assertEquals("application/json;odata.metadata=minimal", request
                .getResponseContentType().toContentTypeString());
          }
        });
  }

  @Test
  public void test$id() throws Exception {
    final ServiceHandler handler = Mockito.mock(ServiceHandler.class);
    helpGETTest(handler, "trippin/$entity?$id="+Encoder.encode("http://localhost:" + TOMCAT_PORT
        + "/trippin/People('russelwhyte')")+"&"+Encoder.encode("$")+"select=FirstName", new TestResult() {
      @Override
      public void validate() throws Exception {
        ArgumentCaptor<DataRequest> arg1 = ArgumentCaptor.forClass(DataRequest.class);
        ArgumentCaptor<EntityResponse> arg2 = ArgumentCaptor.forClass(EntityResponse.class);
        Mockito.verify(handler).read(arg1.capture(), arg2.capture());

        DataRequest request = arg1.getValue();
        Assertions.assertEquals("application/json;odata.metadata=minimal", request.getResponseContentType()
            .toContentTypeString());
      }
    });
  }
}
