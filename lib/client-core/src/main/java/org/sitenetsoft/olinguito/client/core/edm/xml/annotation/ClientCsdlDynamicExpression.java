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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 */
package org.sitenetsoft.olinguito.client.core.edm.xml.annotation;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

import org.sitenetsoft.olinguito.client.core.edm.xml.AbstractClientCsdlEdmDeserializer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlDynamicExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
//CHECKSTYLE:OFF
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression.LogicalOrComparisonExpressionType;
//CHECKSTYLE:ON
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ClientCsdlDynamicExpression.DynamicExpressionDeserializer.class)
public abstract class ClientCsdlDynamicExpression extends CsdlDynamicExpression implements Serializable {

  @Serial
  private static final long serialVersionUID = 1093411847477874348L;

  static class DynamicExpressionDeserializer
      extends AbstractClientCsdlEdmDeserializer<CsdlDynamicExpression> {

    // OData CSDL JSON element names for dynamic expressions
    private static final String APPLY = "Apply";
    private static final String CAST = "Cast";
    private static final String COLLECTION = "Collection";
    private static final String IF = "If";
    private static final String IS_OF = "IsOf";
    private static final String LABELED_ELEMENT = "LabeledElement";
    private static final String NULL = "Null";
    private static final String RECORD = "Record";
    private static final String URL_REF = "UrlRef";

    private static final String ANNOTATION_PATH = "AnnotationPath";
    private static final String NAVIGATION_PROPERTY_PATH = "NavigationPropertyPath";
    private static final String PATH = "Path";
    private static final String PROPERTY_PATH = "PropertyPath";

    private CsdlExpression parseConstOrEnumExpression(final JsonParser jp) throws IOException {
      CsdlExpression result;
      if (isAnnotationConstExprConstruct(jp)) {
        result = parseAnnotationConstExprConstruct(jp);
      } else {
        result = jp.readValueAs(ClientCsdlDynamicExpression.class);
      }
      jp.nextToken();

      return result;
    }

    @Override
    protected CsdlDynamicExpression doDeserialize(final JsonParser jp,
        final DeserializationContext ctxt) throws IOException {

      CsdlDynamicExpression expression = null;

      if ("Not".equals(jp.currentName())) {
        final CsdlLogicalOrComparisonExpression not =
            new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.Not);
        jp.nextToken();
        // Search for field name
        while (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
          jp.nextToken();
        }
        not.setLeft(jp.readValueAs(ClientCsdlDynamicExpression.class));
        // Search for end object
        while (jp.getCurrentToken() != JsonToken.END_OBJECT || !"Not".equals(jp.currentName())) {
          jp.nextToken();
        }

        expression = not;
      } else if (LogicalOrComparisonExpressionType.fromString(jp.currentName()) != null) {
        final CsdlLogicalOrComparisonExpression logicalOrComparissonExp =
            new CsdlLogicalOrComparisonExpression(LogicalOrComparisonExpressionType.fromString(jp.currentName()));
        jp.nextToken();
        // Search for field name
        while (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
          jp.nextToken();
        }
        // Read left and right operands as dynamic expressions
        logicalOrComparissonExp.setLeft(jp.readValueAs(ClientCsdlDynamicExpression.class));
        logicalOrComparissonExp.setRight(jp.readValueAs(ClientCsdlDynamicExpression.class));
        // Search for expression
        while (jp.getCurrentToken() != JsonToken.END_OBJECT || !jp.currentName().equals(logicalOrComparissonExp
            .getType().name())) {
          jp.nextToken();
        }

        expression = logicalOrComparissonExp;
      } else if (PATH.equals(jp.currentName())) {
        expression = new CsdlPath().setValue(jp.nextTextValue());
      } else if (NAVIGATION_PROPERTY_PATH.equals(jp.currentName())) {
        expression = new CsdlNavigationPropertyPath().setValue(jp.nextTextValue());
      } else if (PROPERTY_PATH.equals(jp.currentName())) {
        expression = new CsdlPropertyPath().setValue(jp.nextTextValue());
      } else if (ANNOTATION_PATH.equals(jp.currentName())) {
        expression = new CsdlAnnotationPath().setValue(jp.nextTextValue());
      } else if (APPLY.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlApply.class);
      } else if (CAST.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlCast.class);
      } else if (COLLECTION.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlCollection.class);
        if(expression == null) {
          expression = new ClientCsdlCollection();
        }
      } else if (IF.equals(jp.currentName())) {
        jp.nextToken();
        jp.nextToken();

        final CsdlIf ifImpl = new CsdlIf();
        ifImpl.setGuard(parseConstOrEnumExpression(jp));
        ifImpl.setThen(parseConstOrEnumExpression(jp));
        ifImpl.setElse(parseConstOrEnumExpression(jp));

        expression = ifImpl;
      } else if (IS_OF.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlIsOf.class);
      } else if (LABELED_ELEMENT.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlLabeledElement.class);
      } else if (NULL.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlNull.class);
      } else if (RECORD.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlRecord.class);
      } else if (URL_REF.equals(jp.currentName())) {
        jp.nextToken();
        expression = jp.readValueAs(ClientCsdlUrlRef.class);
      }

      return expression;
    }
  }
}
