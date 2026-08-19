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
 * Copyright 2026 SiteNetSoft - Added the CSDL JSON annotation and annotation expression reader
 * Copyright 2026 SiteNetSoft - Kept the collection cast type and rejected unknown expression members
 */
package org.sitenetsoft.olinguito.server.core;

import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.AT;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.COLLECTION;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.DOLLAR;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.TYPE;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.child;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.flag;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.objectNode;
import static org.sitenetsoft.olinguito.server.core.MetadataJsonParser.requireText;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlApply;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCast;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNull;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlUrlRef;

/**
 * Reads the annotations of a CSDL JSON document (OData CSDL JSON section 14.2) and the constant and
 * dynamic expressions that are their values (sections 14.3 and 14.4).
 * <p>
 * It is a collaborator of {@link MetadataJsonParser}: the parser owns the document walk and calls in
 * once per model element it has built, handing over the JSON object that element came from. Alias
 * resolution, the facet reader, the error paths and the JSON accessors all come from the parser.
 * <p>
 * Two shapes are accepted on input that this library never writes, because every Olinguito release
 * before this one wrote them and those documents are still out there: the CSDL <em>XML</em> element
 * names as constant expression members ({@code $Binary}, {@code $Int}, ...) and a {@code $Type} member
 * on a record expression.
 */
final class MetadataJsonAnnotationReader {

  private static final String PATH = DOLLAR + "Path";
  private static final String ANNOTATION_PATH = DOLLAR + "AnnotationPath";
  private static final String NAVIGATION_PROPERTY_PATH = DOLLAR + "NavigationPropertyPath";
  private static final String PROPERTY_PATH = DOLLAR + "PropertyPath";
  private static final String NOT = DOLLAR + "Not";
  private static final String APPLY = DOLLAR + "Apply";
  private static final String FUNCTION = DOLLAR + "Function";
  private static final String CAST = DOLLAR + "Cast";
  private static final String IF = DOLLAR + "If";
  private static final String IS_OF = DOLLAR + "IsOf";
  private static final String LABELED_ELEMENT = DOLLAR + "LabeledElement";
  private static final String LABELED_ELEMENT_REFERENCE = DOLLAR + "LabeledElementReference";
  private static final String NAME = DOLLAR + "Name";
  private static final String NULL = DOLLAR + "Null";
  private static final String URL_REF = DOLLAR + "UrlRef";

  /**
   * Section 14.4.12: "The type of a record expression is represented as the @type control
   * information". These member names are JSON control information, never annotation terms.
   */
  private static final String AT_TYPE = "@type";
  private static final String ODATA_AT_TYPE = "@odata.type";
  private static final Set<String> CONTROL_INFORMATION = Set.of(AT_TYPE, ODATA_AT_TYPE);

  /**
   * Section 14.4.2: the operators whose value is an array of two operand expressions. $Has and $In are
   * in the spec's table but have no LogicalOrComparisonExpressionType constant in this codebase, so
   * they are reported rather than silently dropped.
   */
  private static final Set<String> BINARY_OPERATORS =
      Set.of(DOLLAR + "And", DOLLAR + "Or", DOLLAR + "Eq", DOLLAR + "Ne",
          DOLLAR + "Gt", DOLLAR + "Ge", DOLLAR + "Lt", DOLLAR + "Le");

  private static final Set<String> UNSUPPORTED_OPERATORS = Set.of(DOLLAR + "Has", DOLLAR + "In");

  private final MetadataJsonParser parser;

  MetadataJsonAnnotationReader(final MetadataJsonParser parser) {
    this.parser = parser;
  }

  // ---------------------------------------------------------------------- section 14.2: annotations

  /**
   * Section 14.2: "An annotation is represented as a member whose name consists of an at (@)
   * character, followed by the qualified name of a term, optionally followed by a hash (#) and a
   * qualifier." When the annotation targets something nested in this object rather than the object
   * itself - an enumeration member, a record member, {@code $OnDelete}, the dependent property of a
   * referential constraint, or an annotation - the member name is prefixed with the name of that
   * thing; {@code memberPrefix} is that name, and the empty string means "this object".
   *
   * @param node the JSON object carrying the annotation members
   * @param target the model element the annotations belong to
   * @param memberPrefix the target prefix the member names must carry, "" for the object itself
   * @param path the JSON path of {@code node}, for error reporting
   */
  void readAnnotations(final ObjectNode node, final CsdlAnnotatable target, final String memberPrefix,
      final String path) throws CsdlJsonParseException {
    if (!this.parser.isParseAnnotations() || node == null || target == null) {
      return;
    }
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (!isAnnotationOf(name, memberPrefix)) {
        continue;
      }
      final CsdlAnnotation annotation =
          readAnnotation(name.substring(memberPrefix.length() + 1), member.getValue(), child(path, name));
      target.getAnnotations().add(annotation);
      // "An annotation can itself be annotated": the member name of such an annotation is this
      // member's whole name followed by another @term.
      readAnnotations(node, annotation, name, path);
    }
  }

  /**
   * Whether {@code name} is {@code prefix} followed by exactly one {@code @term[#qualifier]}. A second
   * at sign means the member annotates the annotation named by everything before it, not this object.
   */
  private static boolean isAnnotationOf(final String name, final String prefix) {
    final int at = prefix.length();
    return name.length() > at + 1
        && name.startsWith(prefix)
        && name.charAt(at) == AT.charAt(0)
        && name.indexOf(AT, at + 1) < 0
        && !CONTROL_INFORMATION.contains(name.substring(at));
  }

  /**
   * @param member the annotation member name with its at sign and any target prefix stripped, so
   *        {@code term} or {@code term#qualifier}
   */
  CsdlAnnotation readAnnotation(final String member, final JsonNode value, final String path)
      throws CsdlJsonParseException {
    final CsdlAnnotation annotation = new CsdlAnnotation();
    final int hash = member.indexOf('#');
    if (hash < 0) {
      // The term name is a qualified name, so a document alias stands for its namespace.
      annotation.setTerm(this.parser.resolveName(member));
    } else {
      annotation.setTerm(this.parser.resolveName(member.substring(0, hash)));
      // Section 14.2.1: "The qualifier is a simple identifier."
      annotation.setQualifier(member.substring(hash + 1));
    }
    annotation.setExpression(readExpression(value, path));
    return annotation;
  }

  /**
   * Section 5.2: {@code $Annotations} is "an object with one member per annotation target", the member
   * name being the target path optionally followed by a hash and a qualifier.
   */
  void readAnnotationGroups(final ObjectNode node, final CsdlSchema schema, final String path)
      throws CsdlJsonParseException {
    if (!this.parser.isParseAnnotations()) {
      return;
    }
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      final String memberPath = child(path, name);
      final CsdlAnnotations group = new CsdlAnnotations();
      final int hash = name.indexOf('#');
      if (hash < 0) {
        group.setTarget(resolveTarget(name));
      } else {
        group.setTarget(resolveTarget(name.substring(0, hash)));
        group.setQualifier(name.substring(hash + 1));
      }
      readAnnotations(objectNode(member.getValue(), memberPath), group, "", memberPath);
      schema.getAnnotationGroups().add(group);
    }
  }

  /**
   * Section 14.2.2: a target path interleaves qualified names with the delimiters {@code / ( ) ,}, and
   * "All qualified names used in a target path MUST be in scope", so every one of them may be written
   * with a document alias. Only the qualified names are resolved; the simple identifiers between them
   * have no dot and pass through {@link MetadataJsonParser#resolveName} unchanged.
   */
  private String resolveTarget(final String target) {
    final StringBuilder resolved = new StringBuilder(target.length());
    int segment = 0;
    for (int i = 0; i < target.length(); i++) {
      final char c = target.charAt(i);
      if (c == '/' || c == '(' || c == ')' || c == ',') {
        resolved.append(this.parser.resolveName(target.substring(segment, i))).append(c);
        segment = i + 1;
      }
    }
    return resolved.append(this.parser.resolveName(target.substring(segment))).toString();
  }

  // ------------------------------------------------------- sections 14.3 and 14.4: the expressions

  /**
   * Sections 14.3 and 14.4. A CSDL JSON constant is a bare JSON value - there are no {@code $Binary},
   * {@code $Date}, {@code $Int}, ... members, those are CSDL XML element names - so the constant's
   * {@link ConstantExpressionType} is recovered from the JSON shape alone. What a string means is
   * governed by the declared type of the applied term, which a document that does not define the term
   * cannot tell us; this is a property of the format, not a limitation of this reader.
   */
  CsdlExpression readExpression(final JsonNode value, final String path) throws CsdlJsonParseException {
    if (value == null || value.isNull()) {
      // Section 14.4.11: "Null expressions that do not contain annotations are represented as the
      // literal null."
      return new CsdlNull();
    }
    if (value.isBoolean()) {
      // Section 14.3.2: "Boolean expressions are represented as the literals true or false."
      return new CsdlConstantExpression(ConstantExpressionType.Bool, value.asText());
    }
    if (value.isIntegralNumber()) {
      // Section 14.3.10: an integer is a JSON number (or, above the safe range, a JSON string, which
      // is indistinguishable from any other string and therefore read as one).
      return new CsdlConstantExpression(ConstantExpressionType.Int, value.asText());
    }
    if (value.isNumber()) {
      // Sections 14.3.5 and 14.3.8: a decimal or floating-point value as a JSON number.
      return new CsdlConstantExpression(ConstantExpressionType.Float, value.asText());
    }
    if (value.isTextual()) {
      // Sections 14.3.1/.3/.4/.6/.7/.9/.11/.12 all render as a JSON string.
      return new CsdlConstantExpression(ConstantExpressionType.String, value.asText());
    }
    if (value.isArray()) {
      // Section 14.4.6: "Collection expressions are represented as arrays with one array item per item
      // expression"; there is no $Collection wrapper.
      final List<CsdlExpression> items = new ArrayList<>();
      for (int i = 0; i < value.size(); i++) {
        items.add(readExpression(value.get(i), path + "[" + i + "]"));
      }
      return new CsdlCollection().setItems(items);
    }
    return readObjectExpression(objectNode(value, path), path);
  }

  private CsdlExpression readObjectExpression(final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    if (node.has(PATH)) {
      // Section 14.4.1: the value is a path, never a qualified name, so it is never alias-resolved.
      return new CsdlPath().setValue(requireText(node, PATH, path));
    }
    if (node.has(ANNOTATION_PATH)) {
      return new CsdlAnnotationPath().setValue(requireText(node, ANNOTATION_PATH, path));
    }
    if (node.has(NAVIGATION_PROPERTY_PATH)) {
      return new CsdlNavigationPropertyPath().setValue(requireText(node, NAVIGATION_PROPERTY_PATH, path));
    }
    if (node.has(PROPERTY_PATH)) {
      return new CsdlPropertyPath().setValue(requireText(node, PROPERTY_PATH, path));
    }
    for (final String operator : UNSUPPORTED_OPERATORS) {
      if (node.has(operator)) {
        throw new CsdlJsonParseException(child(path, operator),
            "the " + operator + " operator has no LogicalOrComparisonExpressionType in this model");
      }
    }
    for (final String operator : BINARY_OPERATORS) {
      if (node.has(operator)) {
        return readBinaryOperator(node, operator, path);
      }
    }
    if (node.has(NOT)) {
      // Section 14.4.2: "Negation expressions are represented as an object with a single member $Not
      // whose value is an annotation expression."
      final CsdlLogicalOrComparisonExpression not = new CsdlLogicalOrComparisonExpression(
          CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType.Not)
          .setLeft(readExpression(node.get(NOT), child(path, NOT)));
      readAnnotations(node, not, "", path);
      return not;
    }
    if (node.has(APPLY)) {
      return readApply(node, path);
    }
    if (node.has(CAST)) {
      final CsdlCast cast = new CsdlCast()
          .setValue(readExpression(node.get(CAST), child(path, CAST)))
          .setType(castType(node, path));
      this.parser.readFacets(node, facets(cast), path);
      readAnnotations(node, cast, "", path);
      return cast;
    }
    if (node.has(IS_OF)) {
      final CsdlIsOf isOf = new CsdlIsOf()
          .setValue(readExpression(node.get(IS_OF), child(path, IS_OF)))
          .setType(castType(node, path));
      this.parser.readFacets(node, facets(isOf), path);
      readAnnotations(node, isOf, "", path);
      return isOf;
    }
    if (node.has(IF)) {
      return readIf(node, path);
    }
    if (node.has(LABELED_ELEMENT)) {
      final CsdlLabeledElement labeled = new CsdlLabeledElement()
          .setValue(readExpression(node.get(LABELED_ELEMENT), child(path, LABELED_ELEMENT)))
          .setName(requireText(node, NAME, path));
      readAnnotations(node, labeled, "", path);
      return labeled;
    }
    if (node.has(LABELED_ELEMENT_REFERENCE)) {
      // Section 14.4.10: the value is a qualified name, so a document alias stands for its namespace.
      return new CsdlLabeledElementReference()
          .setValue(this.parser.resolveName(requireText(node, LABELED_ELEMENT_REFERENCE, path)));
    }
    if (node.has(NULL)) {
      // Section 14.4.11: "Null expression containing annotations are represented as an object with a
      // member $Null whose value is the literal null."
      final CsdlNull nullExpression = new CsdlNull();
      readAnnotations(node, nullExpression, "", path);
      return nullExpression;
    }
    if (node.has(URL_REF)) {
      final CsdlUrlRef urlRef =
          new CsdlUrlRef().setValue(readExpression(node.get(URL_REF), child(path, URL_REF)));
      readAnnotations(node, urlRef, "", path);
      return urlRef;
    }
    final CsdlExpression legacy = readLegacyConstant(node, path);
    if (legacy != null) {
      return legacy;
    }
    rejectUnknownExpression(node, path);
    return readRecord(node, path);
  }

  /**
   * Sections 14.4.5 and 14.4.8: "a member $Type whose value is a string containing the qualified type
   * name, and optionally a member $Collection with a value of true". The Csdl model has no collection
   * flag on a cast, it keeps the same {@code Collection(...)} type expression the CSDL XML attribute
   * carries, which is what {@code EdmCastImpl} parses; dropping $Collection would turn a cast to a
   * collection into a cast to a scalar.
   */
  private String castType(final ObjectNode node, final String path) throws CsdlJsonParseException {
    final String type = this.parser.resolveName(requireText(node, TYPE, path));
    return flag(node, COLLECTION) ? "Collection(" + type + ")" : type;
  }

  /**
   * Section 14.4.12: a record is "an object with one member per property value expression", and a
   * property name is a simple identifier - it never starts with a dollar. So an object whose only
   * members are $-prefixed ones is not a record; it is a dynamic expression this reader does not know,
   * either a form the Csdl model has no class for (such as $ModelElementPath) or a misspelling. It is
   * reported rather than silently turned into an empty record. The legacy record's $Type is the one
   * $-member that does belong to a record.
   */
  private static void rejectUnknownExpression(final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    String unknown = null;
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final String name = members.next().getKey();
      if (!name.startsWith(DOLLAR)) {
        if (!name.contains(AT)) {
          return; // a property value expression, so this really is a record
        }
      } else if (unknown == null && !TYPE.equals(name)) {
        unknown = name;
      }
    }
    if (unknown != null) {
      throw new CsdlJsonParseException(child(path, unknown),
          "the member " + unknown + " is not a constant or dynamic expression this parser knows");
    }
  }

  /**
   * Section 14.4.2: "represented as an object with a single member whose value is an array with two
   * annotation expressions".
   */
  private CsdlExpression readBinaryOperator(final ObjectNode node, final String operator, final String path)
      throws CsdlJsonParseException {
    final String operatorPath = child(path, operator);
    final JsonNode operands = node.get(operator);
    if (!operands.isArray() || operands.size() != 2) {
      throw new CsdlJsonParseException(operatorPath, "a comparison or logical operator takes two operands");
    }
    final CsdlLogicalOrComparisonExpression expression = new CsdlLogicalOrComparisonExpression(
        CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType.valueOf(operator.substring(1)))
        .setLeft(readExpression(operands.get(0), operatorPath + "[0]"))
        .setRight(readExpression(operands.get(1), operatorPath + "[1]"));
    readAnnotations(node, expression, "", path);
    return expression;
  }

  /**
   * Section 14.4.4: "an object with a member $Apply whose value is an array of annotation expressions,
   * and a member $Function whose value is a string containing the qualified name of the client-side
   * function".
   */
  private CsdlExpression readApply(final ObjectNode node, final String path) throws CsdlJsonParseException {
    final String applyPath = child(path, APPLY);
    final JsonNode arguments = node.get(APPLY);
    if (!arguments.isArray()) {
      throw new CsdlJsonParseException(applyPath, "$Apply is an array of annotation expressions");
    }
    final List<CsdlExpression> parameters = new ArrayList<>();
    for (int i = 0; i < arguments.size(); i++) {
      parameters.add(readExpression(arguments.get(i), applyPath + "[" + i + "]"));
    }
    // The client-side function name is a qualified name; odata.* is reserved by the specification.
    final CsdlApply apply = new CsdlApply()
        .setFunction(this.parser.resolveName(requireText(node, FUNCTION, path)))
        .setParameters(parameters);
    readAnnotations(node, apply, "", path);
    return apply;
  }

  /** Section 14.4.7: "an object with a member $If whose value is an array of two or three ... ". */
  private CsdlExpression readIf(final ObjectNode node, final String path) throws CsdlJsonParseException {
    final String ifPath = child(path, IF);
    final JsonNode branches = node.get(IF);
    if (!branches.isArray() || branches.size() < 2 || branches.size() > 3) {
      throw new CsdlJsonParseException(ifPath, "$If is an array of two or three annotation expressions");
    }
    final CsdlIf conditional = new CsdlIf()
        .setGuard(readExpression(branches.get(0), ifPath + "[0]"))
        .setThen(readExpression(branches.get(1), ifPath + "[1]"));
    if (branches.size() == 3) {
      conditional.setElse(readExpression(branches.get(2), ifPath + "[2]"));
    }
    readAnnotations(node, conditional, "", path);
    return conditional;
  }

  /**
   * Section 14.4.12: "Record expressions are represented as objects with one member per property value
   * expression", with the type in the {@code @type} control information and "Annotations for record
   * members ... prefixed with the member name".
   */
  private CsdlExpression readRecord(final ObjectNode node, final String path) throws CsdlJsonParseException {
    final CsdlRecord record = new CsdlRecord();
    final String type = recordType(node);
    if (type != null) {
      record.setType(this.parser.resolveName(type));
    }
    final List<CsdlPropertyValue> propertyValues = new ArrayList<>();
    final Iterator<Map.Entry<String, JsonNode>> members = node.fields();
    while (members.hasNext()) {
      final Map.Entry<String, JsonNode> member = members.next();
      final String name = member.getKey();
      if (name.startsWith(DOLLAR) || name.contains(AT)) {
        continue; // the legacy $Type, the @type control information and annotations
      }
      final String memberPath = child(path, name);
      final CsdlPropertyValue propertyValue = new CsdlPropertyValue()
          .setProperty(name)
          .setValue(readExpression(member.getValue(), memberPath));
      readAnnotations(node, propertyValue, name, path);
      propertyValues.add(propertyValue);
    }
    record.setPropertyValues(propertyValues);
    readAnnotations(node, record, "", path);
    return record;
  }

  /**
   * The record's type name. Section 14.4.12 carries it in the {@code @type} control information, whose
   * value may be a bare qualified name, the {@code #}-prefixed short form or - as in the spec's own
   * Example 86 - a URI with the qualified name as its fragment; everything after the last hash is the
   * name in all three. A {@code $Type} member is the pre-conformance Olinguito shape, read but never
   * written.
   */
  private static String recordType(final ObjectNode node) {
    JsonNode type = null;
    // The order is fixed: @type is the control information section 14.4.12 names, @odata.type is its
    // 4.0 spelling. Iterating the set would be salted per JVM and nondeterministic when both appear.
    for (final String member : new String[] {AT_TYPE, ODATA_AT_TYPE}) {
      if (type == null && node.hasNonNull(member) && node.get(member).isTextual()) {
        type = node.get(member);
      }
    }
    if (type == null && node.hasNonNull(TYPE) && node.get(TYPE).isTextual()) {
      type = node.get(TYPE);
    }
    return type == null ? null : type.asText().substring(type.asText().lastIndexOf('#') + 1);
  }

  /**
   * Legacy input tolerance: an object whose single non-annotation member is a CSDL XML element name is
   * the constant expression the pre-conformance Olinguito writer produced. Conformant CSDL JSON has no
   * such members, and nothing in this library writes them any more.
   */
  private static CsdlExpression readLegacyConstant(final ObjectNode node, final String path)
      throws CsdlJsonParseException {
    for (final ConstantExpressionType type : ConstantExpressionType.values()) {
      final String member = DOLLAR + type.name();
      if (node.has(member)) {
        final JsonNode value = node.get(member);
        if (value.isContainerNode()) {
          throw new CsdlJsonParseException(child(path, member), "a constant expression value is not an object");
        }
        return new CsdlConstantExpression(type, value.asText());
      }
    }
    return null;
  }

  private static MetadataJsonParser.FacetSink facets(final CsdlCast cast) {
    return new MetadataJsonParser.FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        cast.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        cast.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        cast.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        cast.setSrid(value);
      }
    };
  }

  private static MetadataJsonParser.FacetSink facets(final CsdlIsOf isOf) {
    return new MetadataJsonParser.FacetSink() {
      @Override
      public void maxLength(final Integer value) {
        isOf.setMaxLength(value);
      }

      @Override
      public void precision(final Integer value) {
        isOf.setPrecision(value);
      }

      @Override
      public void scale(final Integer value) {
        isOf.setScale(value);
      }

      @Override
      public void srid(final SRID value) {
        isOf.setSrid(value);
      }
    };
  }
}
