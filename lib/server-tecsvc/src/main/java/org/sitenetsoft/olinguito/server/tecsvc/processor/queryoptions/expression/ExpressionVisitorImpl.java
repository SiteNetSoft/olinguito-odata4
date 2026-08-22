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
 * Copyright 2026 SiteNetSoft - Converted switch statements to switch expressions
 * Copyright 2026 SiteNetSoft - Evaluate dynamic (open-type) properties at query-option runtime
 * Copyright 2026 SiteNetSoft - OpenType CRUD Task 3: extracted dynamic-property EdmPrimitiveTypeKind
 * resolution into DynamicPropertyTypeResolver, shared with the tecsvc GET-dispatch path
 * Copyright 2026 SiteNetSoft - Dispatch matchesPattern to MethodCallOperator
 * Copyright 2026 SiteNetSoft - OData 4.01: dispatch geo.distance, geo.length and geo.intersects,
 * and type geo literals while their EdmType is still known
 * Copyright 2026 SiteNetSoft - Tier 7 Wave 2: evaluate the divby operator
 */
package org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.sitenetsoft.olinguito.commons.api.data.ComplexValue;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.Link;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmFunction;
import org.sitenetsoft.olinguito.commons.api.edm.EdmNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveType;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeKind;
import org.sitenetsoft.olinguito.commons.api.edm.EdmProperty;
import org.sitenetsoft.olinguito.commons.api.edm.EdmType;
import org.sitenetsoft.olinguito.commons.api.edm.constants.EdmTypeKind;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoResource;
import org.sitenetsoft.olinguito.server.api.uri.UriParameter;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceDynamicProperty;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceFunction;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceLambdaAny;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceLambdaVariable;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceNavigation;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceProperty;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Binary;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Expression;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Literal;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.Member;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.MethodKind;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.UnaryOperatorKind;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceLambdaVarImpl;
import org.sitenetsoft.olinguito.server.tecsvc.data.DataProvider;
import org.sitenetsoft.olinguito.server.tecsvc.data.DynamicPropertyTypeResolver;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.TypedOperand;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.UntypedOperand;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operand.VisitorOperand;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operation.BinaryOperator;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operation.GeoOperator;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operation.MethodCallOperator;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.operation.UnaryOperator;
import org.sitenetsoft.olinguito.server.tecsvc.processor.queryoptions.expression.primitive.EdmNull;

public class ExpressionVisitorImpl implements ExpressionVisitor<VisitorOperand> {

  private Entity entity;
  private final UriInfoResource uriInfo;
  private final Edm edm;
  private ComplexValue complexValue;

  public ExpressionVisitorImpl(final Entity entity, final UriInfoResource uriInfo, final Edm edm) {
    this.entity = entity;
    this.uriInfo = uriInfo;
    this.edm = edm;
  }

  public ExpressionVisitorImpl(final ComplexValue complexValue, final UriInfoResource uriInfo, final Edm edm) {
    this.complexValue = complexValue;
    this.uriInfo = uriInfo;
    this.edm = edm;
  }

  @Override
  public VisitorOperand visitBinaryOperator(final BinaryOperatorKind operator, final VisitorOperand left,
      final VisitorOperand right) throws ExpressionVisitException, ODataApplicationException {

    final BinaryOperator binaryOperator = new BinaryOperator(left, right);

    return switch (operator) {
      case AND -> binaryOperator.andOperator();
      case OR -> binaryOperator.orOperator();
      case EQ -> binaryOperator.equalsOperator();
      case NE -> binaryOperator.notEqualsOperator();
      case GE -> binaryOperator.greaterEqualsOperator();
      case GT -> binaryOperator.greaterThanOperator();
      case LE -> binaryOperator.lessEqualsOperator();
      case LT -> binaryOperator.lessThanOperator();
      case ADD, SUB, MUL, DIV, MOD -> binaryOperator.arithmeticOperator(operator);
      // divby has its own semantics ([OData-URL] 5.1.1.2.5: decimal promotion, and -INF/INF/NaN
      // instead of failing on division by zero), so it is not folded into arithmeticOperator.
      case DIVBY -> binaryOperator.divByOperator();
      case HAS -> binaryOperator.hasOperator();
      case IN -> binaryOperator.inOperator();
    };
  }

  @Override
  public VisitorOperand visitUnaryOperator(final UnaryOperatorKind operator, final VisitorOperand operand)
      throws ExpressionVisitException, ODataApplicationException {

    final UnaryOperator unaryOperator = new UnaryOperator(operand);

    return switch (operator) {
      case MINUS -> unaryOperator.minusOperation();
      case NOT -> unaryOperator.notOperation();
    };
  }

  @Override
  public VisitorOperand visitMethodCall(final MethodKind methodCall, final List<VisitorOperand> parameters)
      throws ExpressionVisitException, ODataApplicationException {

    final MethodCallOperator methodCallOperation = new MethodCallOperator(parameters);

    return switch (methodCall) {
      case ENDSWITH -> methodCallOperation.endsWith();
      case INDEXOF -> methodCallOperation.indexOf();
      case STARTSWITH -> methodCallOperation.startsWith();
      case TOLOWER -> methodCallOperation.toLower();
      case TOUPPER -> methodCallOperation.toUpper();
      case TRIM -> methodCallOperation.trim();
      case SUBSTRING -> methodCallOperation.substring();
      case CONTAINS -> methodCallOperation.contains();
      case CONCAT -> methodCallOperation.concat();
      case LENGTH -> methodCallOperation.length();
      case YEAR -> methodCallOperation.year();
      case MONTH -> methodCallOperation.month();
      case DAY -> methodCallOperation.day();
      case HOUR -> methodCallOperation.hour();
      case MINUTE -> methodCallOperation.minute();
      case SECOND -> methodCallOperation.second();
      case FRACTIONALSECONDS -> methodCallOperation.fractionalseconds();
      case ROUND -> methodCallOperation.round();
      case FLOOR -> methodCallOperation.floor();
      case CEILING -> methodCallOperation.ceiling();
      case SUBSTRINGOF -> methodCallOperation.substringof();
      case MATCHESPATTERN -> methodCallOperation.matchesPattern();
      case GEODISTANCE -> new GeoOperator(parameters).distance();
      case GEOLENGTH -> new GeoOperator(parameters).length();
      case GEOINTERSECTS -> new GeoOperator(parameters).intersects();
      default -> throwNotImplemented();
    };
  }

  @Override
  public VisitorOperand visitLambdaExpression(final String lambdaFunction, final String lambdaVariable,
      final Expression expression) throws ExpressionVisitException, ODataApplicationException {
    return throwNotImplemented();
  }

  @Override
  public VisitorOperand visitLiteral(final Literal literal) throws ExpressionVisitException, ODataApplicationException {
    // A geo literal must be typed here, while its EdmType is still known: UntypedOperand's own type
    // inference tries Edm.String first (UntypedOperand#determineType), which would classify
    // geography'SRID=4326;Point(1 2)' as a string. Every other literal keeps its existing handling.
    final EdmType type = literal.getType();
    if (type instanceof EdmPrimitiveType primitiveType && isGeoType(primitiveType)) {
      return new UntypedOperand(literal.getText()).asTypedOperand(primitiveType);
    }
    return new UntypedOperand(literal.getText());
  }

  private static boolean isGeoType(final EdmPrimitiveType type) {
    try {
      return EdmPrimitiveTypeKind.valueOfFQN(
          type.getFullQualifiedName().getFullQualifiedNameAsString()).isGeospatial();
    } catch (final IllegalArgumentException e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public VisitorOperand visitMember(final Member member) throws ExpressionVisitException,
      ODataApplicationException {

    final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

    // UriResourceParts contains at least one UriResource.
    final UriResource initialPart = uriResourceParts.get(0);
    if (initialPart instanceof UriResourceProperty uriResourceProp) {
      EdmProperty currentEdmProperty = uriResourceProp.getProperty();
      Property currentProperty = entity.getProperty(currentEdmProperty.getName());
      for (int i = 1; i < uriResourceParts.size(); i++) {
        if (uriResourceParts.get(i) instanceof UriResourceDynamicProperty dynamicProperty) {
          // Dynamic (undeclared) property segments are always leaves of the resource path
          // (OData open-type property paths cannot continue past an unresolved member),
          // so it is safe to resolve and return immediately here. currentProperty (or the
          // complex property leading to it) can be entirely absent on a given entity - only
          // some entities in a collection may have seeded the parent complex property at all -
          // so both null-property and non-complex-property must fall back to "no value" rather
          // than NPE.
          return resolveDynamicProperty(
              currentProperty != null && currentProperty.isComplex() ? currentProperty.asComplex().getValue() : null,
              dynamicProperty.getPropertyName());
        } else if (currentProperty.isComplex()) {
          if (uriResourceParts.get(i) instanceof UriResourceLambdaAny any) {
            if (any.getExpression() instanceof Binary expression) {
              if (currentProperty.isCollection()) {
                final List<ComplexValue> complex = (List<ComplexValue>) currentProperty.asCollection();
                Iterator<ComplexValue> itr = complex.iterator();
                while (itr.hasNext()) {
                  final ComplexValue value = itr.next();
                  VisitorOperand operand = expression.accept(new ExpressionVisitorImpl(value, uriInfo, edm));
                  final TypedOperand typedOperand = operand.asTypedOperand();
                  if (typedOperand.is(OData.newInstance().createPrimitiveTypeInstance
                      (EdmPrimitiveTypeKind.Boolean))) {
                    if (Boolean.TRUE.equals(typedOperand.getTypedValue(Boolean.class))) {
                      return operand;
                    }
                  }
                }
              } 
            }
          } else {
            currentEdmProperty = ((UriResourceProperty) uriResourceParts.get(i)).getProperty();
            final List<Property> complex = currentProperty.asComplex().getValue();
            for (final Property innerProperty : complex) {
              if (innerProperty.getName().equals(currentEdmProperty.getName())) {
                currentProperty = innerProperty;
                break;
              }
            }
          }
        }
      }
      return new TypedOperand(currentProperty.getValue(), currentEdmProperty.getType(), currentEdmProperty);
    } else if (initialPart instanceof UriResourceDynamicProperty dynamicProperty) {
      // Undeclared property of an open type, resolved directly against the current entity's
      // property list; resolveDynamicProperty() returns a typed-null operand (mirroring how
      // missing nullable primitives are represented) when the property is not present.
      return resolveDynamicProperty(entity == null ? null : entity.getProperties(),
          dynamicProperty.getPropertyName());
    } else if (initialPart instanceof UriResourceFunction uriResourceFunc) {
      final EdmFunction function = uriResourceFunc.getFunction();
      if (uriResourceParts.size() > 1) {
        return throwNotImplemented();
      }
      final EdmType type = function.getReturnType().getType();
      final DataProvider dataProvider = new DataProvider(OData.newInstance(), edm);
      final List<UriParameter> parameters = uriResourceFunc.getParameters();
      return new TypedOperand(
        type.getKind() == EdmTypeKind.ENTITY ?
            function.getReturnType().isCollection() ?
                dataProvider.readFunctionEntityCollection(function, parameters, uriInfo) :
                dataProvider.readFunctionEntity(function, parameters, uriInfo) :
            dataProvider.readFunctionPrimitiveComplex(function, parameters, uriInfo),
        type);

    } else if (initialPart instanceof UriResourceLambdaVariable) {
      EdmComplexType complexType = (EdmComplexType) ((UriResourceLambdaVarImpl)initialPart).getTypeFilter();
      EdmProperty currentEdmProperty = ((UriResourceProperty) uriResourceParts.get(1)).getProperty();
      Property currentProperty = null;
      List<Property> properties = complexValue.getValue();
      for (final Property innerProperty : properties) {
        if (innerProperty.getName().equals(currentEdmProperty.getName()) && 
            complexType.getProperty(innerProperty.getName()) != null) {
          currentProperty = innerProperty;
          break;
        }
      }
      return new TypedOperand(currentProperty == null ? null : currentProperty.getValue(), 
          currentEdmProperty.getType(), currentEdmProperty);
    } else if (initialPart instanceof UriResourceNavigation uriResourceNav) {
      EdmNavigationProperty currentEdmNavProperty = uriResourceNav.getProperty();
      EdmProperty currentEdmProperty = null;
      Link link = entity.getNavigationLink(currentEdmNavProperty.getName());
      Entity inlineEntity = link != null ? link.getInlineEntity() : null;
      Property currentProperty = null;
      for (int i = 1; i < uriResourceParts.size(); i++) {
        currentEdmProperty = ((UriResourceProperty) uriResourceParts.get(i)).getProperty();
        if (null != inlineEntity) {
          for (Property property : inlineEntity.getProperties()) {
            if (property.getName().equalsIgnoreCase(currentEdmProperty.getName())) {
              currentProperty = property;
              break;
            } 
          }
        }
      }
      return new TypedOperand(currentProperty != null ? currentProperty.getValue() : null, 
          currentEdmProperty.getType(), currentEdmProperty);
    } else {
      return throwNotImplemented();
    }
  }

  @Override
  public VisitorOperand visitAlias(final String aliasName) throws ExpressionVisitException, ODataApplicationException {
    if (entity.getProperty(uriInfo.getValueForAlias(aliasName)) != null) {
      return new UntypedOperand(String.valueOf(entity.getProperty(uriInfo.getValueForAlias(aliasName)).getValue()));
    } else {
      return new UntypedOperand(uriInfo.getValueForAlias(aliasName));
    }
  }

  @Override
  public VisitorOperand visitTypeLiteral(final EdmType type)
      throws ExpressionVisitException, ODataApplicationException {
    return throwNotImplemented();
  }

  @Override
  public VisitorOperand visitLambdaReference(final String variableName) throws ExpressionVisitException,
      ODataApplicationException {
    return throwNotImplemented();
  }

  @Override
  public VisitorOperand visitEnum(final EdmEnumType type, final List<String> enumValues)
      throws ExpressionVisitException, ODataApplicationException {
    Long result = null;
    try {
      for (final String enumValue : enumValues) {
        final Long value = type.valueOfString(enumValue, null, null, null, null, null, Long.class);
        result = result == null ? value : result | value;
      }
    } catch (final EdmPrimitiveTypeException e) {
      throw new ODataApplicationException("Illegal enum value.",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT, e);
    }
    return new TypedOperand(result, type);
  }

  private VisitorOperand throwNotImplemented() throws ODataApplicationException {
    throw new ODataApplicationException("Not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
        Locale.ROOT);
  }

  /**
   * Looks up a dynamic (undeclared, open-type) property by name in the given property list.
   * Returns a typed-null operand ({@link EdmNull}-typed, null value) when the list is
   * null/empty or the property is absent, mirroring how a null comparison operand is
   * represented elsewhere in this package (see {@code BinaryOperator}/{@code TypedOperand}).
   */
  private VisitorOperand resolveDynamicProperty(final List<Property> properties, final String propertyName) {
    if (properties != null) {
      for (final Property property : properties) {
        if (property.getName().equals(propertyName)) {
          return new TypedOperand(property.getValue(), inferDynamicPropertyType(property));
        }
      }
    }
    return new TypedOperand(null, EdmNull.getInstance());
  }

  /**
   * Dynamic properties carry no EDM property definition, so their runtime type has to be
   * derived: from {@link DynamicPropertyTypeResolver}, which resolves a stored type-name string
   * (set when the value came in with an explicit {@code @type} annotation) or, failing that,
   * infers a kind from the Java value's class.
   */
  private EdmType inferDynamicPropertyType(final Property property) {
    if (property.getValue() == null) {
      return EdmNull.getInstance();
    }
    return OData.newInstance().createPrimitiveTypeInstance(DynamicPropertyTypeResolver.resolveKind(property));
  }

  @Override
  public VisitorOperand visitBinaryOperator(BinaryOperatorKind operator, VisitorOperand left,
      List<VisitorOperand> right) throws ExpressionVisitException, ODataApplicationException {
    BinaryOperator binaryOperator = new BinaryOperator(left, right);
    return switch (operator) {
      case IN -> binaryOperator.inOperator();
      default -> throwNotImplemented();
    };
  }
}
