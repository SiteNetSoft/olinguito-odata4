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
 * Copyright 2026 SiteNetSoft - Modernized Collections usage
 */
package org.sitenetsoft.olinguito.server.core.responses;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;

public class StreamResponse extends ServiceResponse {

  public StreamResponse(ServiceMetadata metadata, ODataResponse response) {
    super(metadata, response, Map.of());
  }

  public void writeStreamResponse(InputStream streamContent, ContentType contentType) {
    this.response.setContent(streamContent);
    writeOK(contentType);
    close();
  }

  public void writeBinaryResponse(byte[] streamContent, ContentType contentType) {
    this.response.setContent(new ByteArrayInputStream(streamContent));
    writeOK(contentType);
    close();
  }

  @Override
  public void accepts(ServiceResponseVisior visitor) throws ODataLibraryException,
      ODataApplicationException {
    visitor.visit(this);
  }
}
