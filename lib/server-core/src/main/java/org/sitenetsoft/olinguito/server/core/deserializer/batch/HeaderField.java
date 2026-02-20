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
 * Copyright 2026 SiteNetSoft - Removed unnecessary boxing and modernized length checks
 * Copyright 2026 SiteNetSoft - Replaced manual hashCode with Objects.hash()
 * Copyright 2026 SiteNetSoft - Modernized equals/hashCode with Objects utility methods
 */
package org.sitenetsoft.olinguito.server.core.deserializer.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HeaderField implements Cloneable {
  private final String fieldName;
  private final int lineNumber;
  private List<String> values;

  public HeaderField(final String fieldName, final int lineNumber) {
    this(fieldName, new ArrayList<>(), lineNumber);
  }

  public HeaderField(final String fieldName, final List<String> values, final int lineNumber) {
    this.fieldName = fieldName;
    this.values = values;
    this.lineNumber = lineNumber;
  }

  public String getFieldName() {
    return fieldName;
  }

  public List<String> getValues() {
    return values;
  }

  public String getValue() {
    final StringBuilder result = new StringBuilder();

    for (final String value : values) {
      result.append(value);
      result.append(", ");
    }

    if (!result.isEmpty()) {
      result.delete(result.length() - 2, result.length());
    }

    return result.toString();
  }

  @Override
  public HeaderField clone() throws CloneNotSupportedException{
    HeaderField clone = (HeaderField) super.clone();
    clone.values = new ArrayList<>(values.size());
    clone.values.addAll(values);
    return clone;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, lineNumber, values);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    HeaderField other = (HeaderField) obj;
    return Objects.equals(fieldName, other.fieldName)
        && lineNumber == other.lineNumber
        && Objects.equals(values, other.values);
  }
}
