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
 * Copyright 2026 SiteNetSoft - Added XML instance annotation serialization support (OLINGO-1581)
 */
package org.sitenetsoft.olinguito.server.core.serializer.xml;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.data.Annotation;
import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.data.Valuable;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;

/**
 * Serializer for instance annotations in OData Atom/XML format.
 * Produces {@code <m:annotation>} elements per OData Atom Format Section 14.
 */
public class ODataXmlInstanceAnnotationSerializer {

  private static final String METADATA = Constants.PREFIX_METADATA;
  private static final String NS_METADATA = Constants.NS_METADATA;
  private static final String DATA = Constants.PREFIX_DATASERVICES;
  private static final String NS_DATA = Constants.NS_DATASERVICES;

  /**
   * Write instance annotations on an entity as {@code <m:annotation>} elements.
   * @param annotations list of annotations on the entity
   * @param writer the XML stream writer
   * @throws XMLStreamException if XML writing fails
   * @throws SerializerException if annotation values cannot be serialized
   */
  public void writeInstanceAnnotationsOnEntity(final List<Annotation> annotations,
      final XMLStreamWriter writer) throws XMLStreamException, SerializerException {
    for (Annotation annotation : annotations) {
      writeAnnotation(writer, annotation);
    }
  }

  /**
   * Write instance annotations on a property as {@code <m:annotation>} elements.
   * @param edmProperty the EDM property definition
   * @param property the property data (may be null)
   * @param writer the XML stream writer
   * @throws XMLStreamException if XML writing fails
   * @throws SerializerException if annotation values cannot be serialized
   */
  public void writeInstanceAnnotationsOnProperties(final EdmProperty edmProperty,
      final Property property, final XMLStreamWriter writer)
      throws XMLStreamException, SerializerException {
    if (property != null) {
      for (Annotation annotation : property.getAnnotations()) {
        writeAnnotation(writer, annotation);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void writeAnnotation(final XMLStreamWriter writer, final Annotation annotation)
      throws XMLStreamException, SerializerException {
    try {
      writer.writeStartElement(METADATA, Constants.ANNOTATION, NS_METADATA);
      writer.writeAttribute(Constants.ATOM_ATTR_TERM, annotation.getTerm());

      switch (annotation.getValueType()) {
      case PRIMITIVE:
        writeAnnotationType(writer, annotation.getType());
        writeAnnotationPrimitiveValue(writer, annotation, annotation.getValue());
        break;
      case COLLECTION_PRIMITIVE:
        writeAnnotationType(writer, "Collection(" + annotation.getType() + ")");
        List list = annotation.asCollection();
        for (Object value : list) {
          writer.writeStartElement(METADATA, Constants.ELEM_ELEMENT, NS_METADATA);
          writeAnnotationPrimitiveValue(writer, annotation, value);
          writer.writeEndElement();
        }
        break;
      case COMPLEX:
        writeAnnotationType(writer, annotation.getType());
        ComplexValue complexValue = annotation.asComplex();
        writeAnnotationComplexValue(writer, complexValue);
        break;
      case COLLECTION_COMPLEX:
        writeAnnotationType(writer, "Collection(" + annotation.getType() + ")");
        List<ComplexValue> complexValues = (List<ComplexValue>) annotation.asCollection();
        for (ComplexValue cv : complexValues) {
          writer.writeStartElement(METADATA, Constants.ELEM_ELEMENT, NS_METADATA);
          writeAnnotationComplexValue(writer, cv);
          writer.writeEndElement();
        }
        break;
      default:
        break;
      }

      writer.writeEndElement(); // annotation
    } catch (final EdmPrimitiveTypeException e) {
      throw new SerializerException("Wrong value for instance annotation!", e,
          SerializerException.MessageKeys.WRONG_PROPERTY_VALUE,
          annotation.getTerm(),
          annotation.getValue().toString());
    }
  }

  private void writeAnnotationType(final XMLStreamWriter writer, final String type)
      throws XMLStreamException {
    if (type != null) {
      writer.writeAttribute(METADATA, NS_METADATA, Constants.ATTR_TYPE, "#" + type);
    }
  }

  private void writeAnnotationComplexValue(final XMLStreamWriter writer,
      final ComplexValue complexValue)
      throws XMLStreamException, SerializerException, EdmPrimitiveTypeException {
    List<Property> properties = complexValue.getValue();
    for (Property prop : properties) {
      writeAnnotationProperty(writer, prop);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void writeAnnotationProperty(final XMLStreamWriter writer, final Valuable property)
      throws XMLStreamException, SerializerException, EdmPrimitiveTypeException {
    String name = (property instanceof Property) ? ((Property) property).getName() : "";

    switch (property.getValueType()) {
    case PRIMITIVE:
      writer.writeStartElement(DATA, name, NS_DATA);
      if (property.getType() != null) {
        EdmPrimitiveType type = EdmPrimitiveTypeFactory.getInstance(
            EdmPrimitiveTypeKind.getByName(property.getType()));
        if (type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.String)) {
          writer.writeAttribute(METADATA, NS_METADATA, Constants.ATTR_TYPE, property.getType());
        }
      }
      writeAnnotationPrimitiveValue(writer, property, property.getValue());
      writer.writeEndElement();
      break;
    case COLLECTION_PRIMITIVE:
      writer.writeStartElement(DATA, name, NS_DATA);
      writer.writeAttribute(METADATA, NS_METADATA, Constants.ATTR_TYPE,
          "#Collection(" + property.getType() + ")");
      List list = property.asCollection();
      for (Object value : list) {
        writer.writeStartElement(METADATA, Constants.ELEM_ELEMENT, NS_METADATA);
        writeAnnotationPrimitiveValue(writer, property, value);
        writer.writeEndElement();
      }
      writer.writeEndElement();
      break;
    case COMPLEX:
      writer.writeStartElement(DATA, name, NS_DATA);
      if (property.getType() != null) {
        writer.writeAttribute(METADATA, NS_METADATA, Constants.ATTR_TYPE, "#" + property.getType());
      }
      writeAnnotationComplexValue(writer, property.asComplex());
      writer.writeEndElement();
      break;
    case COLLECTION_COMPLEX:
      writer.writeStartElement(DATA, name, NS_DATA);
      writer.writeAttribute(METADATA, NS_METADATA, Constants.ATTR_TYPE,
          "#Collection(" + property.getType() + ")");
      List<ComplexValue> complexValues = (List<ComplexValue>) property.asCollection();
      for (ComplexValue cv : complexValues) {
        writer.writeStartElement(METADATA, Constants.ELEM_ELEMENT, NS_METADATA);
        writeAnnotationComplexValue(writer, cv);
        writer.writeEndElement();
      }
      writer.writeEndElement();
      break;
    default:
      break;
    }
  }

  private void writeAnnotationPrimitiveValue(final XMLStreamWriter writer,
      final Valuable annotation, final Object value)
      throws XMLStreamException, EdmPrimitiveTypeException {
    if (annotation.getType() == null) {
      if (value != null) {
        writer.writeCharacters(value.toString());
      }
      return;
    }
    EdmPrimitiveType type = EdmPrimitiveTypeFactory.getInstance(
        EdmPrimitiveTypeKind.getByName(annotation.getType()));
    final String stringValue = type.valueToString(value, null, null, null, null, null);
    if (stringValue == null) {
      writer.writeAttribute(METADATA, NS_METADATA, Constants.ATTR_NULL, "true");
    } else {
      writer.writeCharacters(stringValue);
    }
  }
}
