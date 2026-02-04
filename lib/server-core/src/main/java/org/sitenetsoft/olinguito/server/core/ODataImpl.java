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

import java.util.Collection;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.constants.Constantsv00;
import org.sitenetsoft.olinguito.commons.api.constants.Constantsv01;
import org.sitenetsoft.olinguito.commons.api.IConstants;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataHandler;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.debug.DebugResponseHelper;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.FixedFormatDeserializer;
import org.sitenetsoft.olinguito.server.api.deserializer.ODataDeserializer;
import org.sitenetsoft.olinguito.server.api.etag.ETagHelper;
import org.sitenetsoft.olinguito.server.api.etag.ServiceMetadataETagSupport;
import org.sitenetsoft.olinguito.server.api.prefer.Preferences;
import org.sitenetsoft.olinguito.server.api.serializer.EdmAssistedSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.EdmDeltaSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.FixedFormatSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.uri.UriHelper;
import org.sitenetsoft.olinguito.server.core.debug.DebugResponseHelperImpl;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;
import org.sitenetsoft.olinguito.server.core.deserializer.FixedFormatDeserializerImpl;
import org.sitenetsoft.olinguito.server.core.deserializer.json.ODataJsonDeserializer;
import org.sitenetsoft.olinguito.server.core.deserializer.xml.ODataXmlDeserializer;
import org.sitenetsoft.olinguito.server.core.etag.ETagHelperImpl;
import org.sitenetsoft.olinguito.server.core.prefer.PreferencesImpl;
import org.sitenetsoft.olinguito.server.core.serializer.FixedFormatSerializerImpl;
import org.sitenetsoft.olinguito.server.core.serializer.json.EdmAssistedJsonSerializer;
import org.sitenetsoft.olinguito.server.core.serializer.json.ODataJsonSerializer;
import org.sitenetsoft.olinguito.server.core.serializer.json.JsonDeltaSerializer;
import org.sitenetsoft.olinguito.server.core.serializer.json.JsonDeltaSerializerWithNavigations;
import org.sitenetsoft.olinguito.server.core.serializer.xml.ODataXmlSerializer;
import org.sitenetsoft.olinguito.server.core.uri.UriHelperImpl;

public class ODataImpl extends OData {

  @Override
  public ODataSerializer createSerializer(final ContentType contentType) throws SerializerException {
    ODataSerializer serializer = null;

    if (contentType != null && contentType.isCompatible(ContentType.APPLICATION_JSON)) {
      final String metadata = contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA);
      if (metadata == null
          || ContentType.VALUE_ODATA_METADATA_MINIMAL.equalsIgnoreCase(metadata)
          || ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(metadata)
          || ContentType.VALUE_ODATA_METADATA_FULL.equalsIgnoreCase(metadata)) {
        serializer = new ODataJsonSerializer(contentType, new Constantsv00());
      }
    } else if (contentType != null && (contentType.isCompatible(ContentType.APPLICATION_XML)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML))) {
      serializer = new ODataXmlSerializer();
    }

    if (serializer == null) {
      throw new SerializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
          SerializerException.MessageKeys.UNSUPPORTED_FORMAT, 
          ((contentType != null) ? contentType.toContentTypeString() : null));
    } else {
      return serializer;
    }
  }
  
  @Override
  public ODataSerializer createSerializer(final ContentType contentType, 
      final List<String> versions) throws SerializerException {
    ODataSerializer serializer = null;
    IConstants constants = new Constantsv00();
    if(versions!=null && !versions.isEmpty() && getMaxVersion(versions) > 4){
      constants = new Constantsv01() ;
    }
    if (contentType != null && contentType.isCompatible(ContentType.APPLICATION_JSON)) {
      final String metadata = contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA);
      if (metadata == null
          || ContentType.VALUE_ODATA_METADATA_MINIMAL.equalsIgnoreCase(metadata)
          || ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(metadata)
          || ContentType.VALUE_ODATA_METADATA_FULL.equalsIgnoreCase(metadata)) {
        serializer = new ODataJsonSerializer(contentType, constants);
      }
    } else if (contentType != null && (contentType.isCompatible(ContentType.APPLICATION_XML)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML))) {
      serializer = new ODataXmlSerializer();
    }

    if (serializer == null) {
      throw new SerializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
          SerializerException.MessageKeys.UNSUPPORTED_FORMAT, 
          ((contentType != null) ? contentType.toContentTypeString() : null));
    } else {
      return serializer;
    }
  }

  @Override
  public FixedFormatSerializer createFixedFormatSerializer() {
    return new FixedFormatSerializerImpl();
  }

  @Override
  public EdmAssistedSerializer createEdmAssistedSerializer(final ContentType contentType) throws SerializerException {
    if (contentType != null && contentType.isCompatible(ContentType.APPLICATION_JSON)) {
      return new EdmAssistedJsonSerializer(contentType);
    }
    throw new SerializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
        SerializerException.MessageKeys.UNSUPPORTED_FORMAT, 
        ((contentType != null) ? contentType.toContentTypeString() : null));
  }
  
  @Override
  public EdmAssistedSerializer createEdmAssistedSerializer(final ContentType contentType, 
		  List<String> versions) throws SerializerException {
	  IConstants constants = new Constantsv00();
	    if(versions!=null && !versions.isEmpty() && getMaxVersion(versions) > 4){
	      constants = new Constantsv01() ;
	    }
    if (contentType != null && contentType.isCompatible(ContentType.APPLICATION_JSON)) {
      return new EdmAssistedJsonSerializer(contentType, constants);
    }
    throw new SerializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
        SerializerException.MessageKeys.UNSUPPORTED_FORMAT, 
        ((contentType != null) ? contentType.toContentTypeString() : null));
  }
  
  
  @Override
  public EdmDeltaSerializer createEdmDeltaSerializer(final ContentType contentType, final List<String> versions)
      throws SerializerException {
    if (contentType != null && contentType.isCompatible(ContentType.APPLICATION_JSON)) {
      if(versions!=null && !versions.isEmpty()){
       return getMaxVersion(versions)>4 ?  new JsonDeltaSerializerWithNavigations(contentType):
         new JsonDeltaSerializer(contentType);
      }
      return new JsonDeltaSerializerWithNavigations(contentType);
    }
    throw new SerializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
        SerializerException.MessageKeys.UNSUPPORTED_FORMAT, 
        ((contentType != null) ? contentType.toContentTypeString() : null));
  }

  private float getMaxVersion(List<String> versions) {
    Float versionValue [] = new Float [versions.size()];
    int i=0;
    Float max=new Float(0);
    for(String version:versions){
     Float ver = Float.valueOf(version);
     versionValue[i++] = ver;
     max = max > ver ? max : ver ;
   }
    return max;
  }

  @Override
  public ODataRequestHandlerImpl createHandler(final ServiceMetadata serviceMetadata) {
    return new ODataRequestHandlerImpl(this, serviceMetadata);
  }

  @Override
  public ODataHandler createRawHandler(ServiceMetadata serviceMetadata) {
    return new ODataHandlerImpl(this, serviceMetadata, new ServerCoreDebugger(this));
  }

  @Override
  public ServiceMetadata createServiceMetadata(final CsdlEdmProvider edmProvider,
      final List<EdmxReference> references) {
    return createServiceMetadata(edmProvider, references, null);
  }

  @Override
  public ServiceMetadata createServiceMetadata(final CsdlEdmProvider edmProvider,
      final List<EdmxReference> references, final ServiceMetadataETagSupport serviceMetadataETagSupport) {
    return new ServiceMetadataImpl(edmProvider, references, serviceMetadataETagSupport);
  }

  @Override
  public FixedFormatDeserializer createFixedFormatDeserializer() {
    return new FixedFormatDeserializerImpl();
  }

  @Override
  public UriHelper createUriHelper() {
    return new UriHelperImpl();
  }

  @Override
  public ODataDeserializer createDeserializer(final ContentType contentType) throws DeserializerException {
    if (contentType != null && contentType.isCompatible(ContentType.JSON)) {
      return new ODataJsonDeserializer(contentType);
    } else if (contentType != null && (contentType.isCompatible(ContentType.APPLICATION_XML)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML))) {
      return new ODataXmlDeserializer();
    } else {
      throw new DeserializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
          DeserializerException.MessageKeys.UNSUPPORTED_FORMAT, 
          ((contentType != null) ? contentType.toContentTypeString() : null));
    }
  }  
  @Override
  public ODataDeserializer createDeserializer(final ContentType contentType,
      ServiceMetadata metadata) throws DeserializerException {
    if (contentType != null && contentType.isCompatible(ContentType.JSON)) {
      return new ODataJsonDeserializer(contentType, metadata);
    } else if (contentType != null && (contentType.isCompatible(ContentType.APPLICATION_XML)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML))) {
      return new ODataXmlDeserializer(metadata);
    } else {
      throw new DeserializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
          DeserializerException.MessageKeys.UNSUPPORTED_FORMAT, 
          ((contentType != null) ? contentType.toContentTypeString() : null));
    }
  }

  @Override
  public EdmPrimitiveType createPrimitiveTypeInstance(final EdmPrimitiveTypeKind kind) {
    return EdmPrimitiveTypeFactory.getInstance(kind);
  }

  @Override
  public ETagHelper createETagHelper() {
    return new ETagHelperImpl();
  }

  @Override
  public Preferences createPreferences(final Collection<String> preferHeaders) {
    return new PreferencesImpl(preferHeaders);
  }

  @Override
  public DebugResponseHelper createDebugResponseHelper(final String debugFormat) {
    // Invalid formats are logged as warnings and default to JSON
    // Supported formats: json, html, download (see DebugSupport constants)
    return new DebugResponseHelperImpl(debugFormat);
  }

  @Override
  public ODataDeserializer createDeserializer(ContentType contentType, List<String> versions)
      throws DeserializerException {
    IConstants constants = new Constantsv00();
    if(versions!=null && !versions.isEmpty() && getMaxVersion(versions)>4){
      constants = new Constantsv01() ;
    }
    if (contentType != null && contentType.isCompatible(ContentType.JSON)) {
      return new ODataJsonDeserializer(contentType, constants);
    } else if (contentType != null && (contentType.isCompatible(ContentType.APPLICATION_XML)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML))) {
      return new ODataXmlDeserializer();
    } else {
      throw new DeserializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
          DeserializerException.MessageKeys.UNSUPPORTED_FORMAT, 
          ((contentType != null) ? contentType.toContentTypeString() : null));
    }
  
  }

  @Override
  public ODataDeserializer createDeserializer(ContentType contentType, ServiceMetadata metadata, List<String> versions)
      throws DeserializerException {
    IConstants constants = new Constantsv00();
    if(versions!=null && !versions.isEmpty() && getMaxVersion(versions)>4){
      constants = new Constantsv01() ;
    }
    if (contentType != null && contentType.isCompatible(ContentType.JSON)) {
      return new ODataJsonDeserializer(contentType, metadata, constants);
    } else if (contentType != null && (contentType.isCompatible(ContentType.APPLICATION_XML)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML))) {
      return new ODataXmlDeserializer(metadata);
    } else {
      throw new DeserializerException("Unsupported format: " + 
    ((contentType != null) ? contentType.toContentTypeString() : null),
          DeserializerException.MessageKeys.UNSUPPORTED_FORMAT, 
          ((contentType != null) ? contentType.toContentTypeString() : null));
    }
  }
}
