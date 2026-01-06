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
package org.sitenetsoft.olinguito.server.core.responses;

import java.util.Collections;

import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;

public class NoContentResponse extends ServiceResponse {

  public NoContentResponse(ServiceMetadata metadata, ODataResponse response) {
    super(metadata, response, Collections.<String,String>emptyMap());
  }

  // 200
  public void writeOK() {
    this.response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    close();
  }

  // 201
  public void writeCreated() {
    this.response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
    close();
  }

  // 202
  public void writeAccepted() {
    this.response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
    close();
  }

  // 204
  public void writeNoContent() {
    writeNoContent(true);
  }

  // 304
  public void writeNotModified() {
    this.response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
    close();
  }

  // error response codes

  // 404
  public void writeNotFound() {
    writeNotFound(true);
  }

  // 501
  public void writeNotImplemented() {
    this.response.setStatusCode(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
    close();
  }

  // 405
  public void writeMethodNotAllowed() {
    this.response.setStatusCode(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode());
    close();
  }

  // 410
  public void writeGone() {
    this.response.setStatusCode(HttpStatusCode.GONE.getStatusCode());
    close();
  }

  // 412
  public void writePreConditionFailed() {
    this.response.setStatusCode(HttpStatusCode.PRECONDITION_FAILED.getStatusCode());
    close();
  }

  @Override
  public void accepts(ServiceResponseVisior visitor) throws ODataLibraryException,
      ODataApplicationException {
    visitor.visit(this);
  }
}
