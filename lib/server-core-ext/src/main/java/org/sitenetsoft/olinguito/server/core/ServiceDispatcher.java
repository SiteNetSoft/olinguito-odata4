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
 * Copyright 2026 SiteNetSoft - Reject dynamic (open-type) property path segments with 404 instead
 * of silently dropping them, since this legacy dispatcher has no mechanism to serve their values
 * Copyright 2026 SiteNetSoft - Moved the dynamic-property 404 check to a pre-check over the
 * parsed URI resource parts in internalExecute (instead of a RequestURLVisitor#visit override),
 * so RequestURLVisitor's existing visit(UriInfo)/visit(UriInfoResource) signatures need not widen
 */
package org.sitenetsoft.olinguito.server.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.commons.core.Decoder;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.CustomContentTypeSupport;
import org.sitenetsoft.olinguito.server.api.serializer.RepresentationType;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoBatch;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoCrossjoin;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoEntityId;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoKind;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoMetadata;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoService;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceAction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceComplexProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceCount;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceDynamicProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePartTyped;
import org.sitenetsoft.olinguito.server.api.uri.UriResourcePrimitiveProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceRef;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceSingleton;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceValue;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.ApplyOption;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FormatOption;
import org.sitenetsoft.olinguito.server.core.requests.ActionRequest;
import org.sitenetsoft.olinguito.server.core.requests.BatchRequest;
import org.sitenetsoft.olinguito.server.core.requests.DataRequest;
import org.sitenetsoft.olinguito.server.core.requests.FunctionRequest;
import org.sitenetsoft.olinguito.server.core.requests.MediaRequest;
import org.sitenetsoft.olinguito.server.core.requests.MetadataRequest;
import org.sitenetsoft.olinguito.server.core.requests.OperationRequest;
import org.sitenetsoft.olinguito.server.core.requests.ServiceDocumentRequest;
import org.sitenetsoft.olinguito.server.core.uri.parser.Parser;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSemanticException;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidator;

public class ServiceDispatcher extends RequestURLHierarchyVisitor {
  private final OData odata;
  protected ServiceMetadata metadata;
  protected ServiceHandler handler;
  protected CustomContentTypeSupport customContentSupport;
  private String idOption;
  protected ServiceRequest request;

  public ServiceDispatcher(OData odata, ServiceMetadata metadata, ServiceHandler handler,
      CustomContentTypeSupport customContentSupport) {
    this.odata = odata;
    this.metadata = metadata;
    this.handler = handler;
    this.customContentSupport = customContentSupport;
  }

  public void execute(ODataRequest odRequest, ODataResponse odResponse) {
    FormatOption formatOption = null;
    ODataException oDataException = null;
    try {
      String path = odRequest.getRawODataPath();      
      String query = odRequest.getRawQueryPath();      
      if(path.indexOf("$entity") != -1) {
        executeIdOption(query, odRequest, odResponse);
      } else {
        UriInfo uriInfo = new Parser(this.metadata.getEdm(), odata)
          .parseUri(path, query, null, odRequest.getRawBaseUri());
        
        formatOption = uriInfo.getFormatOption();
        
        internalExecute(uriInfo, odRequest, odResponse);
      }
      return;
    } catch(ODataLibraryException | ODataApplicationException e) {
    	oDataException = e;
    }
    ContentType contentType = ContentType.JSON;
    try {
      contentType = ContentNegotiator.doContentNegotiation(formatOption, 
          odRequest, this.customContentSupport, RepresentationType.ERROR);
    } catch (ContentNegotiatorException e) {
      // ignore, default to JSON
    }
    handleException(oDataException, contentType, odRequest, odResponse);
  }
  
  protected void handleException(ODataException e, ContentType contentType,
      ODataRequest odRequest, ODataResponse odResponse) {
    ErrorHandler errorHandler = new ErrorHandler(this.odata, this.metadata,
        this.handler, contentType);
    errorHandler.handleException(e, odRequest, odResponse);    
  }
  
  private void internalExecute(UriInfo uriInfo, ODataRequest odRequest,
      ODataResponse odResponse) throws ODataLibraryException,
      ODataApplicationException {

    new UriValidator().validate(uriInfo, odRequest.getMethod());

    rejectDynamicPropertySegments(uriInfo);

    // part1, 8.2.6
    String isolation = odRequest.getHeader(HttpHeader.ODATA_ISOLATION);
    if (isolation != null && "snapshot".equals(isolation) && !this.handler.supportsDataIsolation()) {
      odResponse.setStatusCode(HttpStatusCode.PRECONDITION_FAILED.getStatusCode());
      return;
    }

    visit(uriInfo);

    // this should cover for any unsupported calls until they are implemented
    if (this.request == null) {
      this.request = new ServiceRequest(this.odata, this.metadata) {
        @Override
        public ContentType getResponseContentType() throws ContentNegotiatorException {
          return ContentType.APPLICATION_JSON;
        }

        @Override
        public void execute(ServiceHandler handler, ODataResponse response)
            throws ODataLibraryException, ODataApplicationException {
          handler.anyUnsupported(getODataRequest(), response);
        }
      };
    }

    // To handle $entity?$id=http://localhost/EntitySet(key) as
    // http://localhost/EntitySet(key)
    if (this.idOption != null) {
      try {
        this.request.setODataRequest(odRequest);
        this.request = this.request.parseLink(new URI(this.idOption));
      } catch (URISyntaxException e) {
        throw new ODataHandlerException("Invalid $id value",
            ODataHandlerException.MessageKeys.FUNCTIONALITY_NOT_IMPLEMENTED, this.idOption);
      }
    }

    this.request.setODataRequest(odRequest);
    this.request.setUriInfo(uriInfo);
    this.request.setCustomContentTypeSupport(this.customContentSupport);
    this.request.execute(this.handler, odResponse);
  }

  @Override
  public void visit(UriInfoMetadata info) {
    this.request = new MetadataRequest(this.odata, this.metadata);
  }

  @Override
  public void visit(UriInfoService info) {
    this.request = new ServiceDocumentRequest(this.odata, this.metadata);
  }

  @Override
  public void visit(UriResourceEntitySet info) {
    DataRequest dataRequest = new DataRequest(this.odata, this.metadata);
    dataRequest.setUriResourceEntitySet(info);
    this.request = dataRequest;
  }

  @Override
  public void visit(UriResourceCount option) {
    if (this.request instanceof DataRequest dataRequest) {
      dataRequest.setCountRequest(option != null);
    } else if (this.request instanceof OperationRequest opRequest) {
      opRequest.setCountRequest(option != null);
    }
  }

  @Override
  public void visit(UriResourceComplexProperty info) {
    DataRequest dataRequest = (DataRequest) this.request;
    dataRequest.setUriResourceProperty(info);
  }

  @Override
  public void visit(UriResourcePrimitiveProperty info) {
    DataRequest dataRequest = (DataRequest) this.request;
    dataRequest.setUriResourceProperty(info);
  }

  /**
   * A dynamic (open-type) property segment has no backing {@link
   * org.sitenetsoft.olinguito.commons.api.edm.EdmProperty}, so it cannot be plugged into {@link
   * DataRequest#setUriResourceProperty}, whose whole {@code PropertyRequest} machinery (context
   * URL, serializer options, {@link ServiceHandler} read/update hooks) is built around one. This
   * legacy, {@link ServiceHandler}-based dispatcher (used by simple samples such as TripPin) has
   * no equivalent hook for serving an undeclared property's value at all. Per OData, addressing a
   * property that does not resolve on the requested instance must 404 - and since this dispatcher
   * can never resolve a dynamic property's value, every dynamic-property segment 404s here, the
   * same way an unknown segment on a closed type has always 404'd. (The newer processor-based
   * dispatch stack used by the OpenType tecsvc integration tests fully supports reading, filtering,
   * and ordering by dynamic properties; only this older stack is limited.)
   *
   * <p>Implemented as a pre-check over the already-parsed URI resource parts, run once up front
   * in {@link #internalExecute}, rather than as a {@link RequestURLVisitor#visit(UriResourceDynamicProperty)}
   * override: {@code RequestURLVisitor} is a shipped public interface, and routing this through
   * the visitor would require widening {@code visit(UriInfo)}/{@code visit(UriInfoResource)}
   * with a new checked exception - a source-breaking change for any other implementer.
   *
   * @param uriInfo the parsed request URI; a no-op unless it is a resource-path URI
   *     ({@link UriInfoKind#resource})
   * @throws UriParserSemanticException {@code PROPERTY_NOT_IN_TYPE} (404) if any resource part is
   *     a {@link UriResourceDynamicProperty}
   */
  private void rejectDynamicPropertySegments(UriInfo uriInfo) throws UriParserSemanticException {
    if (uriInfo.getKind() != UriInfoKind.resource) {
      return;
    }

    List<UriResource> parts = uriInfo.asUriInfoResource().getUriResourceParts();
    EdmType owningType = null;
    for (UriResource resource : parts) {
      if (resource instanceof UriResourceDynamicProperty dynamicProperty) {
        String typeName = owningType == null ? "the requested resource"
            : owningType.getFullQualifiedName().getFullQualifiedNameAsString();
        throw new UriParserSemanticException(
            "The type '" + typeName + "' has no property '" + dynamicProperty.getPropertyName() + "'",
            UriParserSemanticException.MessageKeys.PROPERTY_NOT_IN_TYPE,
            typeName, dynamicProperty.getPropertyName());
      }
      // Tracks the type of the most recent typed segment so a dynamic property nested under a
      // (possibly open) complex property, e.g. .../PropertyComp/CompDynamic, is reported against
      // the complex type it actually belongs to rather than the root entity/singleton type.
      if (resource instanceof UriResourcePartTyped typed && typed.getType() != null) {
        owningType = typed.getType();
      }
    }
  }

  @Override
  public void visit(UriResourceValue info) {
    DataRequest dataRequest = (DataRequest) this.request;
    if (dataRequest.isPropertyRequest()) {
      dataRequest.setValueRequest(info != null);
    } else {
      MediaRequest mediaRequest = new MediaRequest(this.odata, this.metadata);
      mediaRequest.setUriResourceEntitySet(dataRequest.getUriResourceEntitySet());
      this.request = mediaRequest;
    }
  }

  @Override
  public void visit(UriResourceAction info) {
    ActionRequest actionRequest = new ActionRequest(this.odata, this.metadata);
    actionRequest.setUriResourceAction(info);
    this.request = actionRequest;
  }

  @Override
  public void visit(UriResourceFunction info) {
    FunctionRequest functionRequest = new FunctionRequest(this.odata, this.metadata);
    functionRequest.setUriResourceFunction(info);
    this.request = functionRequest;
  }

  @Override
  public void visit(UriResourceNavigation info) {
    DataRequest dataRequest = (DataRequest) this.request;
    dataRequest.addUriResourceNavigation(info);
  }

  @Override
  public void visit(UriResourceRef info) {
    // this is same as data, but return is just entity references.
    DataRequest dataRequest = (DataRequest) this.request;
    dataRequest.setReferenceRequest(info != null);
  }

  @Override
  public void visit(UriInfoBatch info) {
    this.request = new BatchRequest(this.odata, this.metadata);
  }

  @Override
  public void visit(UriResourceSingleton info) {
    DataRequest dataRequest = new DataRequest(this.odata, this.metadata);
    dataRequest.setUriResourceSingleton(info);
    this.request = dataRequest;
  }

  @Override
  public void visit(UriInfoEntityId info) {
    DataRequest dataRequest = new DataRequest(this.odata, this.metadata);
    this.request = dataRequest;
    super.visit(info);
  }

  @Override
  public void visit(UriInfoCrossjoin info) {
    DataRequest dataRequest = new DataRequest(this.odata, this.metadata);
    dataRequest.setCrossJoin(info);
    this.request = dataRequest;
  }
  
  @Override
  public void visit(ApplyOption option) {
    ((DataRequest)this.request).setApply(option);
  }
  
  private void executeIdOption(String query, ODataRequest odRequest,
      ODataResponse odResponse) throws ODataLibraryException,
      ODataApplicationException {
    StringBuilder sb = new StringBuilder();
    StringTokenizer st = new StringTokenizer(query, "&");
    boolean first = true;
    while(st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.startsWith("$id=")) {
        URI id = URI.create(Decoder.decode(token.substring(4)));
        sb.append(id.getPath());
      } else {
        if (first) {
          sb.append("?");
        } else {
          sb.append("&");
        }
        sb.append(token);
      }
    }    
    DataRequest dataRequest = new DataRequest(this.odata, this.metadata);
    this.request = dataRequest;
    
    this.request.setODataRequest(odRequest);
    this.request = this.request.parseLink(URI.create(sb.toString()));

    this.request.setODataRequest(odRequest);
    this.request.setCustomContentTypeSupport(this.customContentSupport);
    this.request.execute(this.handler, odResponse);    
  }
}
