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
 */
package org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types;

// CHECKSTYLE:OFF (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Key;

// CHECKSTYLE:ON (Maven checkstyle)

@org.sitenetsoft.olinguito.ext.proxy.api.annotations.Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType(name = "Statement",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Statement
    extends org.sitenetsoft.olinguito.ext.proxy.api.Annotatable,
    org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Statement>, org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery<Statement> {

  @Key
  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "StatementID",
      type = "Edm.Int32",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Integer getStatementID();

  void setStatementID(java.lang.Integer _statementID);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "TransactionType",
      type = "Edm.String",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.String getTransactionType();

  void setTransactionType(java.lang.String _transactionType);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "TransactionDescription",
      type = "Edm.String",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.String getTransactionDescription();

  void setTransactionDescription(java.lang.String _transactionDescription);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Amount",
      type = "Edm.Double",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Double getAmount();

  void setAmount(java.lang.Double _amount);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }

  Annotations annotations();

  interface Annotations {

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "StatementID",
        type = "Edm.Int32")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getStatementIDAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "TransactionType",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getTransactionTypeAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "TransactionDescription",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getTransactionDescriptionAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Amount",
        type = "Edm.Double")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getAmountAnnotations();

  }

}
