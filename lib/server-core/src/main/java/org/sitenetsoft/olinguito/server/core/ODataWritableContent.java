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
 * Copyright 2026 SiteNetSoft - Stream collection-valued properties (primitive and complex) as JSON
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.sitenetsoft.olinguito.commons.api.data.EntityIterator;
import org.sitenetsoft.olinguito.commons.api.data.EntityMediaObject;
import org.sitenetsoft.olinguito.commons.api.data.PropertyIterator;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.server.api.ODataContent;
import org.sitenetsoft.olinguito.server.api.ODataContentWriteErrorCallback;
import org.sitenetsoft.olinguito.server.api.ODataContentWriteErrorContext;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.serializer.ComplexSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.sitenetsoft.olinguito.server.core.serializer.SerializerStreamResultImpl;
import org.sitenetsoft.olinguito.server.core.serializer.FixedFormatSerializerImpl;
import org.sitenetsoft.olinguito.server.core.serializer.json.ODataJsonSerializer;
import org.sitenetsoft.olinguito.server.core.serializer.xml.ODataXmlSerializer;

/**
 * Stream supporting implementation of the ODataContent
 * and contains the response content for the OData request.
 * <p/>
 * If an error occur during a <code>write</code> method <b>NO</b> exception
 * will be thrown but if registered the
 * org.sitenetsoft.olinguito.server.api.ODataContentWriteErrorCallback is called.
 */
public class ODataWritableContent implements ODataContent {
  private StreamContent streamContent;

  private static abstract class StreamContent {
    protected EntityIterator iterator;
    protected ServiceMetadata metadata;
    protected EdmEntityType entityType;
    protected EntityCollectionSerializerOptions options;
    protected EntityMediaObject mediaEntity;
    protected PropertyIterator properties;

    public StreamContent(EntityIterator iterator, EdmEntityType entityType, ServiceMetadata metadata,
        EntityCollectionSerializerOptions options) {
      this.iterator = iterator;
      this.entityType = entityType;
      this.metadata = metadata;
      this.options = options;
    }

    public StreamContent(EntityMediaObject mediaEntity) {
    	this.mediaEntity = mediaEntity;
    }

    protected StreamContent() {
    }

    protected abstract void writeEntity(EntityIterator entity, OutputStream outputStream) throws SerializerException;

    protected abstract void writeBinary(EntityMediaObject mediaEntity, OutputStream outputStream)
    		throws SerializerException;

    protected void writeProperties(OutputStream outputStream) throws SerializerException {
      throw new ODataRuntimeException("Not Implemented in Property Handling");
    }

    public void write(OutputStream out) {
      try {
    	  if (properties != null) {
    	    writeProperties(out);
    	  } else if (mediaEntity == null) {
    		  writeEntity(iterator, out);
    	  } else {
    		  writeBinary(mediaEntity, out);
    	  }
      } catch (SerializerException e) {
        final ODataContentWriteErrorCallback errorCallback =
            options == null ? null : options.getODataContentWriteErrorCallback();
        if (errorCallback != null) {
          final WriteErrorContext errorContext = new WriteErrorContext(e);
          errorCallback.handleError(errorContext, Channels.newChannel(out));
        }
      }
    }
  }

  private static class StreamContentForJson extends StreamContent {
    private ODataJsonSerializer jsonSerializer;

    public StreamContentForJson(EntityIterator iterator, EdmEntityType entityType,
        ODataJsonSerializer jsonSerializer, ServiceMetadata metadata,
        EntityCollectionSerializerOptions options) {
      super(iterator, entityType, metadata, options);

      this.jsonSerializer = jsonSerializer;
    }

    protected void writeEntity(EntityIterator entity, OutputStream outputStream) throws SerializerException {
      try {
        jsonSerializer.entityCollectionIntoStream(metadata, entityType, entity, options, outputStream);
        outputStream.flush();
      } catch (final IOException e) {
        throw new ODataRuntimeException("Failed entity serialization", e);
      }
    }
    @Override
	protected void writeBinary(EntityMediaObject mediaEntity,
			OutputStream outputStream) throws SerializerException {
		throw new ODataRuntimeException("Not Implemented in Entity Handling");
	}
  }

  /**
   * Streams a single collection-valued property (primitive or complex) as JSON. The `type` field
   * holds either an EdmPrimitiveType or an EdmComplexType; which one is decided by whether
   * `complexOptions` or `primitiveOptions` is set, exactly as the two serializer entry points are
   * distinct.
   */
  private static class StreamContentForJsonProperty extends StreamContent {
    private final ODataJsonSerializer jsonSerializer;
    private final ServiceMetadata propertyMetadata;
    private final EdmPrimitiveType primitiveType;
    private final EdmComplexType complexType;
    private final PrimitiveSerializerOptions primitiveOptions;
    private final ComplexSerializerOptions complexOptions;

    StreamContentForJsonProperty(PropertyIterator properties, EdmPrimitiveType primitiveType,
        ODataJsonSerializer jsonSerializer, ServiceMetadata metadata, PrimitiveSerializerOptions options) {
      this.properties = properties;
      this.primitiveType = primitiveType;
      this.complexType = null;
      this.jsonSerializer = jsonSerializer;
      this.propertyMetadata = metadata;
      this.primitiveOptions = options;
      this.complexOptions = null;
    }

    StreamContentForJsonProperty(PropertyIterator properties, EdmComplexType complexType,
        ODataJsonSerializer jsonSerializer, ServiceMetadata metadata, ComplexSerializerOptions options) {
      this.properties = properties;
      this.primitiveType = null;
      this.complexType = complexType;
      this.jsonSerializer = jsonSerializer;
      this.propertyMetadata = metadata;
      this.primitiveOptions = null;
      this.complexOptions = options;
    }

    @Override
    protected void writeEntity(EntityIterator entity, OutputStream outputStream) throws SerializerException {
      throw new ODataRuntimeException("Not Implemented in Property Handling");
    }

    @Override
    protected void writeBinary(EntityMediaObject mediaEntity, OutputStream outputStream)
        throws SerializerException {
      throw new ODataRuntimeException("Not Implemented in Property Handling");
    }

    @Override
    protected void writeProperties(final OutputStream outputStream) throws SerializerException {
      try {
        if (complexType != null) {
          jsonSerializer.complexCollectionIntoStream(propertyMetadata, complexType, properties, complexOptions,
              outputStream);
        } else {
          jsonSerializer.primitiveCollectionIntoStream(propertyMetadata, primitiveType, properties, primitiveOptions,
              outputStream);
        }
        outputStream.flush();
      } catch (IOException e) {
        throw new ODataRuntimeException("Failed property serialization", e);
      }
    }
  }

  private static class StreamContentForMedia extends StreamContent {
	    private FixedFormatSerializerImpl fixedFormatSerializer;

	    public StreamContentForMedia(EntityMediaObject mediaEntity, 
	    		FixedFormatSerializerImpl fixedFormatSerializer) {
	      super(mediaEntity);

	      this.fixedFormatSerializer = fixedFormatSerializer;
	    }

	    protected void writeEntity(EntityIterator entity, 
	        OutputStream outputStream) throws SerializerException {
	    	throw new ODataRuntimeException("Not Implemented in Entity Handling");
	    }

		@Override
		protected void writeBinary(EntityMediaObject mediaEntity, 
				OutputStream outputStream) throws SerializerException {
			fixedFormatSerializer.binaryIntoStreamed(mediaEntity, outputStream);
		}
	  }

  private static class StreamContentForXml extends StreamContent {
    private ODataXmlSerializer xmlSerializer;

    public StreamContentForXml(EntityIterator iterator, EdmEntityType entityType,
        ODataXmlSerializer xmlSerializer, ServiceMetadata metadata,
        EntityCollectionSerializerOptions options) {
      super(iterator, entityType, metadata, options);

      this.xmlSerializer = xmlSerializer;
    }

    protected void writeEntity(EntityIterator entity, OutputStream outputStream) throws SerializerException {
      try {
        xmlSerializer.entityCollectionIntoStream(metadata, entityType, entity, options, outputStream);
        outputStream.flush();
      } catch (final IOException e) {
        throw new ODataRuntimeException("Failed entity serialization", e);
      }
    }
    
	protected void writeBinary(EntityMediaObject mediaEntity, 
			OutputStream outputStream) throws SerializerException {
		throw new ODataRuntimeException("Not Implemented in XML Handling");
	}
  }

  @Override
  public void write(WritableByteChannel writeChannel) {
    this.streamContent.write(Channels.newOutputStream(writeChannel));
  }

  @Override
  public void write(OutputStream stream) {
    write(Channels.newChannel(stream));
  }

  private ODataWritableContent(StreamContent streamContent) {
    this.streamContent = streamContent;
  }

  public static ODataWritableContentBuilder with(EntityIterator iterator, EdmEntityType entityType,
      ODataSerializer serializer, ServiceMetadata metadata,
      EntityCollectionSerializerOptions options) {
    return new ODataWritableContentBuilder(iterator, entityType, serializer, metadata, options);
  }
  
  public static ODataWritableContentBuilder with(EntityMediaObject mediaEntity,
		  FixedFormatSerializerImpl fixedFormatSerializer) {
	  return new ODataWritableContentBuilder(mediaEntity, fixedFormatSerializer);
  }

  public static ODataWritableContentBuilder with(PropertyIterator properties, EdmPrimitiveType type,
      ODataSerializer serializer, ServiceMetadata metadata, PrimitiveSerializerOptions options) {
    return new ODataWritableContentBuilder(properties, type, serializer, metadata, options);
  }

  public static ODataWritableContentBuilder with(PropertyIterator properties, EdmComplexType type,
      ODataSerializer serializer, ServiceMetadata metadata, ComplexSerializerOptions options) {
    return new ODataWritableContentBuilder(properties, type, serializer, metadata, options);
  }

  public static class WriteErrorContext implements ODataContentWriteErrorContext {
    private ODataLibraryException exception;

    public WriteErrorContext(ODataLibraryException exception) {
      this.exception = exception;
    }

    @Override
    public Exception getException() {
      return exception;
    }

    @Override
    public ODataLibraryException getODataLibraryException() {
      return exception;
    }
  }

  public static class ODataWritableContentBuilder {
    private ODataSerializer serializer;
    private EntityIterator entities;
    private ServiceMetadata metadata;
    private EdmEntityType entityType;
    private EntityCollectionSerializerOptions options;
    private FixedFormatSerializerImpl fixedFormatSerializer;
    private EntityMediaObject mediaEntity;
    private PropertyIterator properties;
    private EdmPrimitiveType primitiveType;
    private EdmComplexType complexType;
    private PrimitiveSerializerOptions primitiveOptions;
    private ComplexSerializerOptions complexOptions;

    public ODataWritableContentBuilder(EntityIterator entities, EdmEntityType entityType,
        ODataSerializer serializer,
        ServiceMetadata metadata, EntityCollectionSerializerOptions options) {
      this.entities = entities;
      this.entityType = entityType;
      this.serializer = serializer;
      this.metadata = metadata;
      this.options = options;
    }

    public ODataWritableContentBuilder(EntityMediaObject mediaEntity, FixedFormatSerializerImpl fixedFormatSerializer) {
    	this.mediaEntity = mediaEntity;
    	this.fixedFormatSerializer = fixedFormatSerializer;
    }

    public ODataWritableContentBuilder(PropertyIterator properties, EdmPrimitiveType primitiveType,
        ODataSerializer serializer, ServiceMetadata metadata, PrimitiveSerializerOptions options) {
      this.properties = properties;
      this.primitiveType = primitiveType;
      this.serializer = serializer;
      this.metadata = metadata;
      this.primitiveOptions = options;
    }

    public ODataWritableContentBuilder(PropertyIterator properties, EdmComplexType complexType,
        ODataSerializer serializer, ServiceMetadata metadata, ComplexSerializerOptions options) {
      this.properties = properties;
      this.complexType = complexType;
      this.serializer = serializer;
      this.metadata = metadata;
      this.complexOptions = options;
    }

    public ODataContent buildContent() {
      if (properties != null) {
        if (serializer instanceof ODataJsonSerializer jsonSerializer) {
          StreamContent input = complexType != null
              ? new StreamContentForJsonProperty(properties, complexType, jsonSerializer, metadata, complexOptions)
              : new StreamContentForJsonProperty(properties, primitiveType, jsonSerializer, metadata,
                  primitiveOptions);
          return new ODataWritableContent(input);
        }
        throw new ODataRuntimeException("No suitable serializer found");
      }
      if (serializer instanceof ODataJsonSerializer jsonSerializer) {
        StreamContent input = new StreamContentForJson(entities, entityType,
            jsonSerializer, metadata, options);
        return new ODataWritableContent(input);
      }else if (serializer instanceof ODataXmlSerializer xmlSerializer) {
        StreamContentForXml input = new StreamContentForXml(entities, entityType,
            xmlSerializer, metadata, options);
        return new ODataWritableContent(input);
      } else if (fixedFormatSerializer instanceof FixedFormatSerializerImpl) {
    	  StreamContent input = new StreamContentForMedia(mediaEntity, fixedFormatSerializer);
    	  return new ODataWritableContent(input);
      }
      throw new ODataRuntimeException("No suitable serializer found");
    }

    public SerializerStreamResult build() {
      return SerializerStreamResultImpl.with().content(buildContent()).build();
    }
  }
}
