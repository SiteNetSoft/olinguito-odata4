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
 * Copyright 2026 SiteNetSoft - Fixed resource leak and hardcoded charset string
 * Copyright 2026 SiteNetSoft - OData 4.01: shared resolver for operation-parameter URI literals
 */
package org.sitenetsoft.olinguito.server.core.deserializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.data.Parameter;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmParameter;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.FixedFormatDeserializer;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchDeserializerException;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchOptions;
import org.sitenetsoft.olinguito.server.api.deserializer.batch.BatchRequestPart;
import org.sitenetsoft.olinguito.server.core.deserializer.batch.BatchParser;
import org.sitenetsoft.olinguito.server.core.deserializer.json.ODataJsonDeserializer;
import org.sitenetsoft.olinguito.server.core.uri.parser.OptionalParameterDefaults;

public class FixedFormatDeserializerImpl implements FixedFormatDeserializer {

  private static final int DEFAULT_BUFFER_SIZE = 128;

  @Override
  public byte[] binary(final InputStream content) throws DeserializerException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    int count;
    try {
      while ((count = content.read(buffer)) > -1) {
        result.write(buffer, 0, count);
      }
      result.flush();
    } catch (final IOException e) {
      throw new DeserializerException("An I/O exception occurred.", e,
          DeserializerException.MessageKeys.IO_EXCEPTION);
    }
    return result.toByteArray();
  }

  @Override
  public Object primitiveValue(final InputStream content, final EdmProperty property) throws DeserializerException {
    if (property == null || !property.isPrimitive()) {
      throw new DeserializerException("Wrong property.", DeserializerException.MessageKeys.NOT_IMPLEMENTED);
    }
    try (InputStreamReader reader = new InputStreamReader(content, StandardCharsets.UTF_8)) {
      StringWriter writer = new StringWriter();
      int c = -1;
      while ((c = reader.read()) != -1) {
        writer.append((char) c);
      }
      final EdmPrimitiveType type = (EdmPrimitiveType) property.getType();
      return type.valueOfString(writer.toString(),
          property.isNullable(), property.getMaxLength(), property.getPrecision(), property.getScale(),
          property.isUnicode(), type.getDefaultType());
    } catch (final EdmPrimitiveTypeException e) {
      throw new DeserializerException("The value is not valid.", e,
          DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, property.getName());
    } catch (final IOException e) {
      throw new DeserializerException("An I/O exception occurred.", e,
          DeserializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  @Override
  public Parameter parameter(final String content, final EdmParameter parameter) throws DeserializerException {
    final EdmType type = parameter.getType();
    if (OptionalParameterDefaults.isPrimitiveLike(parameter)) {
      // The content is a primitive URI literal.
      Parameter result = new Parameter();
      result.setName(parameter.getName());
      result.setType(type.getFullQualifiedName().getFullQualifiedNameAsString());
      final EdmPrimitiveType primitiveType = (EdmPrimitiveType) type;
      try {
        result.setValue(type.getKind() == EdmTypeKind.ENUM ? ValueType.ENUM : ValueType.PRIMITIVE,
            OptionalParameterDefaults.valueOfUriLiteral(parameter, primitiveType, content));
      } catch (final EdmPrimitiveTypeException e) {
        throw new DeserializerException(
            "Invalid value '" + content + "' for parameter " + parameter.getName(), e,
            DeserializerException.MessageKeys.INVALID_VALUE_FOR_PROPERTY, parameter.getName());
      }
      return result;
    } else {
      // The content is a JSON array or object.
      return new ODataJsonDeserializer(ContentType.JSON).parameter(content, parameter);
    }
  }

  @Override
  public List<BatchRequestPart> parseBatchRequest(final InputStream content, final String boundary,
      final BatchOptions options)
          throws BatchDeserializerException {
    final BatchParser parser = new BatchParser();

    return parser.parseBatchRequest(content, boundary, options);
  }
}
