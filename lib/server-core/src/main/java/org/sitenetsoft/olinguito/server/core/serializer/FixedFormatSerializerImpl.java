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
 * Copyright 2026 SiteNetSoft - Replace hardcoded charset strings with StandardCharsets
 */
package org.sitenetsoft.olinguito.server.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.data.EntityMediaObject;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.ODataResponsePart;
import org.sitenetsoft.olinguito.server.api.serializer.BatchSerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.FixedFormatSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.PrimitiveValueSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.sitenetsoft.olinguito.server.core.ODataWritableContent;

public class FixedFormatSerializerImpl implements FixedFormatSerializer {

  @Override
  public InputStream binary(final byte[] binary) throws SerializerException {
    return new ByteArrayInputStream(binary);
  }
  
  protected void binary(final EntityMediaObject mediaEntity, 
		  OutputStream outputStream) throws SerializerException {
	  try {
		outputStream.write(mediaEntity.getBytes());
	} catch (IOException e) {
		throw new SerializerException("IO Exception occured ", e, SerializerException.MessageKeys.IO_EXCEPTION);
	}
  }
  
  public void binaryIntoStreamed(final EntityMediaObject mediaEntity, 
		  final OutputStream outputStream) throws SerializerException {
	binary(mediaEntity, outputStream);
  }
  
  @Override
  public SerializerStreamResult mediaEntityStreamed(EntityMediaObject mediaEntity) throws SerializerException {
	  return ODataWritableContent.with(mediaEntity, this).build();
  }

  @Override
  public InputStream count(final Integer count) throws SerializerException {
    return new ByteArrayInputStream(count.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public InputStream primitiveValue(final EdmPrimitiveType type, final Object value,
      final PrimitiveValueSerializerOptions options) throws SerializerException {
    try {
      final String result = type.valueToString(value,
          options.isNullable(), options.getMaxLength(),
          options.getPrecision(), options.getScale(), options.isUnicode());
      return new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    } catch (final EdmPrimitiveTypeException e) {
      throw new SerializerException("Error in primitive-value formatting.", e,
          SerializerException.MessageKeys.WRONG_PRIMITIVE_VALUE,
          type.getFullQualifiedName().getFullQualifiedNameAsString(), value.toString());
    }
  }

  @Override
  public InputStream asyncResponse(final ODataResponse odataResponse) throws SerializerException {
    AsyncResponseSerializer serializer = new AsyncResponseSerializer();
    return serializer.serialize(odataResponse);
  }

  @Override
  public InputStream batchResponse(final List<ODataResponsePart> batchResponses, final String boundary)
      throws BatchSerializerException {
    final BatchResponseSerializer serializer = new BatchResponseSerializer();

    return serializer.serialize(batchResponses, boundary);
  }
}
