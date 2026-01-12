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
 */
package org.sitenetsoft.olinguito.server.example;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.sitenetsoft.olinguito.server.adapter.servlet.OData4HttpHandler;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.core.MetadataParser;
import org.sitenetsoft.olinguito.server.core.OData4Impl;

import static org.junit.Assert.assertNotNull;

public class TripPinServlet extends HttpServlet {
  private static final long serialVersionUID = 2663595419366214401L;
  private TripPinDataModel dataModel;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    OData odata = OData4Impl.newInstance();
    MetadataParser parser = new MetadataParser();
    ServiceMetadata metadata = null;

    try {
      parser.parseAnnotations(true);
      parser.useLocalCoreVocabularies(true);
      parser.implicitlyLoadCoreVocabularies(true);

      InputStream in = getClass().getClassLoader().getResourceAsStream("trippin.xml");
      assertNotNull("trippin.xml not found on test classpath", in);

      metadata = parser.buildServiceMetadata(new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }

    OData4HttpHandler handler = new OData4HttpHandler(odata, metadata);

    if (this.dataModel == null) {
      try {
        this.dataModel = new TripPinDataModel(metadata);
      } catch (Exception e) {
        throw new IOException("Failed to load data for TripPin Service");
      }
    }

    handler.register(new TripPinHandler(this.dataModel));
    handler.process(request, response);
  }
}
