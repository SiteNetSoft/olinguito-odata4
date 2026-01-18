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
package org.sitenetsoft.olinguito.server.core;

import static org.junit.Assert.assertNotNull;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ODataServerError;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchDeserializerException;
import org.sitenetsoft.olinguito.server.api.etag.ServiceMetadataETagSupport;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.core.legacy.ProcessorServiceHandler;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserException;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSemanticException;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSyntaxException;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;
import org.junit.Before;
import org.junit.Test;

public class ErrorHandlerTest {
  CsdlEdmProvider provider = null;
  
  @Before
  public void setUp() throws Exception {
    MetadataParser parser = new MetadataParser();
    parser.parseAnnotations(true);
    parser.useLocalCoreVocabularies(true);
    InputStream in = getClass().getClassLoader().getResourceAsStream("annotations.xml");
    assertNotNull("annotations.xml not found on test classpath", in);
    provider = (CsdlEdmProvider) parser.buildEdmProvider(new InputStreamReader(in, StandardCharsets.UTF_8));
  }
  
  @Test
  public void testError(){
    EdmxReference reference = new EdmxReference
        (URI.create("../v4.0/cs02/vocabularies/Org.OData.Core.V1.xml"));
    reference.addInclude(new EdmxReferenceInclude("Org.OData.Core.V1", "Core"));
    ServiceHandler handler = new ProcessorServiceHandler();
    ServiceMetadata metadata = new ServiceMetadataImpl(provider,  
        Collections.singletonList(reference), new ServiceMetadataETagSupport() {
      @Override
      public String getServiceDocumentETag() {
        return "W/\"serviceDocumentETag\"";
      }
      @Override
      public String getMetadataETag() {
        return "W/\"metadataETag\"";
      }
    } );
    OData odata = new ODataImpl();
    ErrorHandler error = new ErrorHandler(odata , metadata, handler,
        ContentType.APPLICATION_ATOM_XML);
    assertNotNull(error);
    UriValidationException e =new UriValidationException("message", 
        UriValidationException.MessageKeys.DOUBLE_KEY_PROPERTY , "param"); 
    ODataRequest request = new ODataRequest();
    ODataResponse response = new ODataResponse();
    error.handleException(e, request , response);
    error.handleException(new UriParserSemanticException("message",
        UriParserSemanticException.MessageKeys.COLLECTION_NOT_ALLOWED, "param")
        , request , response);
    error.handleException(new UriParserSyntaxException("message",
        UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION, "param")
        , request , response);
    String[] param = new String[2];
    error.handleException(new UriParserExceptionCustom("message",
        UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION, param)
        , request , response);
    error.handleException(new BatchDeserializerException("message",
        BatchDeserializerException.MessageKeys.INVALID_BASE_URI, "param")
        , request , response);
    error.handleException(new SerializerException("message",
        SerializerException.MessageKeys.IO_EXCEPTION, "param")
        , request , response);
    error.handleException(new ContentNegotiatorException("message",
        ContentNegotiatorException.MessageKeys.NO_CONTENT_TYPE_SUPPORTED, "param")
        , request , response);
    error.handleException(new UriParserSyntaxException("message",
        UriParserSyntaxException.MessageKeys.DUPLICATED_ALIAS, "param")
        , request , response);
    error.handleException(new DeserializerException("message",
        DeserializerException.MessageKeys.DUPLICATE_PROPERTY, "param")
        , request , response);
    error.handleException(new ODataHandlerException("message",
        ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD, "param")
        , request , response);
    error.handleException(new ODataApplicationException("message",
        500, Locale.ENGLISH)
        , request , response);
    error.handleException(new NullPointerException("message")
        , request , response);
    error.handleServerError(request, response, new ODataServerError());
  }

  class UriParserExceptionCustom extends UriParserException{

    public UriParserExceptionCustom(String developmentMessage, MessageKey messageKey,
        String[] parameters) {
      super(developmentMessage, messageKey, parameters);
    }
    
  }
}
