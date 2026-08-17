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
 * Copyright 2026 SiteNetSoft - Replaced Calendar.getInstance() with java.time-based construction
 * Copyright 2026 SiteNetSoft - Removed unnecessary boxing and modernized length checks
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Use Valuable.asCollection() for entity collection parameters (OLINGO-1638)
 * Copyright 2026 SiteNetSoft - OData 4.01: functions with optional parameters
 * Copyright 2026 SiteNetSoft - OData 4.01: malformed optional-parameter default values are rejected with 400
 */
package org.sitenetsoft.olinguito.server.tecsvc.data;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Parameter;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.ValueType;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider.DataProviderException;
import org.sitenetsoft.olinguito.server.tecsvc.provider.ComplexTypeProvider;

public class FunctionData {

  private static final UUID GUID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");;

  protected static EntityCollection entityCollectionFunction(final String name,
      final Map<String, Parameter> parameters, final Map<String, EntityCollection> data)
      throws DataProviderException {
    if (name.equals("UFCRTCollETTwoKeyNavParam")) {
      final List<Entity> esTwoKeyNav = data.get("ESTwoKeyNav").getEntities();
      EntityCollection result = new EntityCollection();
      final int endIndex = parameters.isEmpty() ? 0 : getParameterInt16(parameters);
      result.getEntities().addAll(
          esTwoKeyNav.subList(0,
              endIndex < 0 ? 0 : endIndex > esTwoKeyNav.size() ? esTwoKeyNav.size() : endIndex));
      return result;
    } else if (name.equals("UFCRTCollETMixPrimCollCompTwoParam")) {
      return data.get("ESMixPrimCollComp");
    } else if (name.equals("UFCRTCollETMedia")) {
      return data.get("ESMedia");
    } else {
      throw new DataProviderException("Function " + name + " is not yet implemented.",
          HttpStatusCode.NOT_IMPLEMENTED);
    }
  }

  protected static Entity entityFunction(final String name, final Map<String, Parameter> parameters,
      final Map<String, EntityCollection> data) throws DataProviderException {
    final List<Entity> esTwoKeyNav = data.get("ESTwoKeyNav").getEntities();
    if (name.equals("UFCRTETKeyNav")) {
      return data.get("ESKeyNav").getEntities().get(0);
    } else if (name.equals("UFCRTETTwoKeyNav")) {
      return esTwoKeyNav.get(0);
    } else if (name.equals("UFCRTETTwoKeyNavParam")) {
      final int index = parameters.isEmpty() ? 0 : getParameterInt16(parameters);
      return index < 0 || index >= esTwoKeyNav.size() ? null : esTwoKeyNav.get(index);
    } else if (name.equals("UFCRTETMedia")) {
      final int index = parameters.isEmpty() ? 1 : getParameterInt16(parameters);
      final List<Entity> esMedia = data.get("ESMedia").getEntities();
      return index < 1 || index > esMedia.size() ? null : esMedia.get(index - 1);
    } else if (name.equals("UFCRTETTwoKeyNavParamCTTwoPrim")) {
      final List<Property> parameterProperties = parameters.get("ParameterCTTwoPrim").asComplex().getValue();
      if (parameterProperties == null || parameterProperties.size() < 2) {
        return null;
      }
      final Short parameterInt16 = (Short) parameterProperties.get(0).asPrimitive();
      final String parameterString = (String) parameterProperties.get(1).asPrimitive();
      for (final Entity entity : esTwoKeyNav) {
        if (entity.getProperty("PropertyInt16").asPrimitive().equals(parameterInt16)
            && entity.getProperty("PropertyString").asPrimitive().equals(parameterString)) {
          return entity;
        }
      }
      return null;
    } else {
      throw new DataProviderException("Function " + name + " is not yet implemented.",
          HttpStatusCode.NOT_IMPLEMENTED);
    }
  }

  @SuppressWarnings("unchecked")
  protected static Property primitiveComplexFunction(final String name, final Map<String, Parameter> parameters,
      final Map<String, EntityCollection> data) throws DataProviderException {
    if (name.equals("UFNRTInt16")) {
      return DataCreator.createPrimitive(name, (short) 12345);
    } else if (name.equals("UFCRTString")) {
      return DataCreator.createPrimitive(name, "UFCRTString string value");
    }else if ( name.equals("BFNESTwoKeyNavRTString") ) {
      return DataCreator.createPrimitive(name, "BFNESTwoKeyNavRTString string value");
    } else if (name.equals("UFCRTStringOptionalParam") || name.equals("UFCRTStringOptionalNoDefault")
        || name.equals("UFCRTStringOptionalBadDefault")) {
      // An omitted optional parameter is either replaced by its default value before we are called
      // or simply absent (if it has no default value), in which case no suffix is appended.
      return DataCreator.createPrimitive(name, getParameterString(parameters) + getParameterSuffix(parameters));
    } else if (name.equals("UFCRTCollString")) {
      return data.get("ESCollAllPrim").getEntities().get(0).getProperty("CollPropertyString");
    } else if (name.equals("UFCRTCTTwoPrim")) {
      return DataCreator.createComplex(name,
          ComplexTypeProvider.nameCTTwoPrim.getFullQualifiedNameAsString(),
          DataCreator.createPrimitive("PropertyInt16", (short) 16),
          DataCreator.createPrimitive("PropertyString", "UFCRTCTTwoPrim string value"));
    } else if (name.equals("UFCRTCTTwoPrimParam")) {
      return DataCreator.createComplex(name,
          ComplexTypeProvider.nameCTTwoPrim.getFullQualifiedNameAsString(),
          DataCreator.createPrimitive("PropertyInt16", getParameterInt16(parameters)),
          DataCreator.createPrimitive("PropertyString", getParameterString(parameters)));
    } else if (name.equals("UFCRTCollCTTwoPrim")) {
      return DataCreator.createComplexCollection(name,
          ComplexTypeProvider.nameCTTwoPrim.getFullQualifiedNameAsString(),
          List.of(DataCreator.createPrimitive("PropertyInt16", (short) 16),
              DataCreator.createPrimitive("PropertyString", "Test123")),
          List.of(DataCreator.createPrimitive("PropertyInt16", 17),
              DataCreator.createPrimitive("PropertyString", "Test456")),
          List.of(DataCreator.createPrimitive("PropertyInt16", 18),
              DataCreator.createPrimitive("PropertyString", "Test678")));
    } else if (name.equals("UFCRTStringTwoParam")) {
      final String parameterString = getParameterString(parameters);
      // ParameterString is not provided
      if (parameterString == null) {
        return DataCreator.createPrimitive(name, null);
      } else {
        final Short parameterInt16 = getParameterInt16(parameters);
        final StringBuilder builder = new StringBuilder();
        // if parameterInt16 <= 0 return an empty string
        for (short i = parameterInt16; i > 0; i--) {
          if (!builder.isEmpty()) {
            builder.append(',');
          }
          builder.append('"')
              .append(parameterString)
              .append('"');
        }
        return DataCreator.createPrimitive(name, builder.toString());
      }
    } else if (name.equals("UFCRTCollCTTwoPrimTwoParam")) {
      final Short parameterInt16 = getParameterInt16(parameters);
      final String parameterString = getParameterString(parameters);
      if (parameterString == null) {
        return DataCreator.createComplexCollection(name,
            ComplexTypeProvider.nameCTTwoPrim.getFullQualifiedNameAsString(),
            List.of(DataCreator.createPrimitive("PropertyInt16", 1),
                DataCreator.createPrimitive("PropertyString", name + " int16 value: " + parameterInt16)),
            List.of(DataCreator.createPrimitive("PropertyInt16", 2),
                DataCreator.createPrimitive("PropertyString", name + "string value: null")));
      } else {
        List<ComplexValue> complexValues = new ArrayList<>();
        short counter = 1;
        for (short i = parameterInt16; 0 < i; i--) {
          ComplexValue complexValue = new ComplexValue();
          complexValue.getValue().add(new Property(null, "PropertyInt16", ValueType.PRIMITIVE, counter++));
          complexValue.getValue().add(new Property(null, "PropertyString", ValueType.PRIMITIVE, 
              name + " string value: " + parameterString));
          complexValue.setId(URI.create(""));
          complexValues.add(complexValue);
        }
        return new Property(null, name, ValueType.COLLECTION_COMPLEX, complexValues);
      }
    } else if (name.equals("UFNRTByteNineParam")) {
      short count = 0;  // counts non-empty parameters
      for (final Parameter parameter : parameters.values()) {
        if (!(parameter.isNull()
            || !parameter.isCollection()
                && (parameter.isComplex() && parameter.asComplex().getValue().isEmpty()
                    || parameter.isEntity() && ((Entity) parameter.getValue()).getProperties().isEmpty())
            || parameter.isCollection()
                && parameter.asCollection().isEmpty())) {
          count++;
        }
      }
      return DataCreator.createPrimitive(null, count);
    } else if (name.equals("_FC_RTTimeOfDay_")) {
      return DataCreator.createPrimitive(name,  getParameterTimeOfDay(parameters));
    } else if (name.equals("BFNESAllPrimRTCTAllPrim")) {
      return DataCreator.createComplex(name,
          ComplexTypeProvider.nameCTAllPrim.getFullQualifiedNameAsString(),
          DataCreator.createPrimitive("PropertyString", "First Resource - first"),
          DataCreator.createPrimitive("PropertyBinary",
              new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }),
          DataCreator.createPrimitive("PropertyBoolean", true),
          DataCreator.createPrimitive("PropertyByte", (short) 255),
          DataCreator.createPrimitive("PropertyDate", getDate(2012, 10, 3)),
          DataCreator.createPrimitive("PropertyDateTimeOffset", getTimestamp(2012, 10, 3, 7, 16, 23, 123456700)),
          DataCreator.createPrimitive("PropertyDecimal", BigDecimal.valueOf(34.27)),
          DataCreator.createPrimitive("PropertySingle", (float) 1.79000000E+20),
          DataCreator.createPrimitive("PropertyDouble", -1.7900000000000000E+19),
          DataCreator.createPrimitive("PropertyDuration", BigDecimal.valueOf(6)),
          DataCreator.createPrimitive("PropertyGuid", GUID),
          DataCreator.createPrimitive("PropertyInt16", Short.MAX_VALUE),
          DataCreator.createPrimitive("PropertyInt32", Integer.MAX_VALUE),
          DataCreator.createPrimitive("PropertyInt64", Long.MAX_VALUE),
          DataCreator.createPrimitive("PropertySByte", Byte.MAX_VALUE),
          DataCreator.createPrimitive("PropertyTimeOfDay", getTime(1, 0, 1)));
    } else if (name.equals("BFCESTwoKeyNavRTCollCTNavFiveProp")) {
      return DataCreator.createComplexCollection(name,
          ComplexTypeProvider.nameCTNavFiveProp.getFullQualifiedNameAsString(),
          List.of(
              DataCreator.createPrimitive("PropertyInt16", (short) 1)));
    } else if (name.equals("BFCESTwoKeyNavRTCollDecimal")) {
      return DataCreator.createPrimitiveCollection(name,
         1d);
    } else if (name.equals("BFCETTwoKeyNavRTCTTwoPrim")) {
      return DataCreator.createComplex(name,
          ComplexTypeProvider.nameCTTwoPrim.getFullQualifiedNameAsString(),
          DataCreator.createPrimitive("PropertyInt16", (short) 16),
          DataCreator.createPrimitive("PropertyString", "BFCETTwoKeyNavRTCTTwoPrim string value"));
    }  else {
      throw new DataProviderException("Function " + name + " is not yet implemented.",
          HttpStatusCode.NOT_IMPLEMENTED);
    }
  }

  private static Short getParameterInt16(final Map<String, Parameter> parameters) {
    return parameters.containsKey("ParameterInt16") ? (Short) parameters.get("ParameterInt16").getValue() : null;
  }

  /** Returns the value of the optional parameter "ParameterSuffix", the empty string if absent or null. */
  private static String getParameterSuffix(final Map<String, Parameter> parameters) {
    final Parameter parameter = parameters.get("ParameterSuffix");
    return parameter == null || parameter.getValue() == null ? "" : (String) parameter.getValue();
  }

  private static String getParameterString(final Map<String, Parameter> parameters) {
    return parameters.containsKey("ParameterString") ? (String) parameters.get("ParameterString").getValue() : null;
  }
  
  private static Calendar getParameterTimeOfDay(final Map<String, Parameter> parameters) {
    return parameters.containsKey("ParameterTimeOfDay") ? (Calendar) parameters.get("ParameterTimeOfDay")
        .getValue() : null;
  }
  
  private static Calendar getDate(final int year, final int month, final int day) {
    return GregorianCalendar.from(
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()));
  }

  private static Calendar getTime(final int hour, final int minute, final int second) {
    return GregorianCalendar.from(
        LocalTime.of(hour, minute, second).atDate(LocalDate.of(1970, 1, 1))
            .atZone(ZoneId.systemDefault()));
  }

  private static Timestamp getTimestamp(final int year, final int month, final int day,
      final int hour, final int minute, final int second, final int nanosecond) {
    Timestamp timestamp = new Timestamp(getDateTime(year, month, day, hour, minute, second).getTimeInMillis());
    timestamp.setNanos(nanosecond);
    return timestamp;
  }


  private static Calendar getDateTime(final int year, final int month, final int day,
      final int hour, final int minute, final int second) {
    return GregorianCalendar.from(
        ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC));
  }
}
