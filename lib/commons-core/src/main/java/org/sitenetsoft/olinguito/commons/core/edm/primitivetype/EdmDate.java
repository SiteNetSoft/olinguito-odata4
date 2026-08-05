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
 * Copyright 2026 SiteNetSoft - Fixed date/time conversions for pre-Julian dates (OLINGO-1646)
 */
package org.sitenetsoft.olinguito.commons.core.edm.primitivetype;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;

/**
 * Implementation of the EDM primitive type Date.
 */
public final class EdmDate extends SingletonPrimitiveType {

  private static final EdmDate INSTANCE = new EdmDate();

  public static EdmDate getInstance() {
    return INSTANCE;
  }

  @Override
  public Class<?> getDefaultType() {
    return Calendar.class;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> T internalValueOfString(final String value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode, final Class<T> returnType)
      throws EdmPrimitiveTypeException {
    LocalDate date;
    try {
      date = LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
    }

    // appropriate types
    if (LocalDate.class.isAssignableFrom(returnType)) {
      return (T) date;
    } else if (java.sql.Date.class.isAssignableFrom(returnType)) {
      return (T) new java.sql.Date(LocalDateTime.of(date, LocalTime.MIDNIGHT)
          .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    // inappropriate types, which need to be supported for backward compatibility
    ZonedDateTime zdt = LocalDateTime.of(date, LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault());
    if (Calendar.class.isAssignableFrom(returnType)) {
      return (T) GregorianCalendar.from(zdt);
    } else if (Long.class.isAssignableFrom(returnType)) {
      return (T) Long.valueOf(zdt.toInstant().toEpochMilli());
    } else if (java.sql.Timestamp.class.isAssignableFrom(returnType)) {
      return (T) java.sql.Timestamp.from(zdt.toInstant());
    } else if (java.util.Date.class.isAssignableFrom(returnType)) {
      return (T) java.util.Date.from(zdt.toInstant());
    } else {
      throw new EdmPrimitiveTypeException("The value type " + returnType + " is not supported.");
    }
  }

  @Override
  protected <T> String internalValueToString(final T value, final Boolean isNullable, final Integer maxLength,
      final Integer precision, final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {
    // appropriate types
    if (value instanceof LocalDate) {
      return value.toString();
    } else if (value instanceof java.sql.Date sqlDate) {
      return Instant.ofEpochMilli(sqlDate.getTime())
          .atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    // inappropriate types, which need to be supported for backward compatibility
    if (value instanceof GregorianCalendar calendar) {
      return calendar.toZonedDateTime().toLocalDate().toString();
    }

    long millis;
    if (value instanceof Long longVal) {
      millis = longVal;
    } else if (value instanceof java.util.Date date) {
      millis = date.getTime();
    } else {
      throw new EdmPrimitiveTypeException("The value type " + value.getClass() + " is not supported.");
    }

    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString();
  }
}
