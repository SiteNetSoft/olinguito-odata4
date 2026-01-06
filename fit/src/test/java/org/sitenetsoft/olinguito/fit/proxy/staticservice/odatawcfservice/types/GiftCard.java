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
import org.sitenetsoft.olinguito.ext.proxy.api.*;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
// CHECKSTYLE:ON (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "GiftCard",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface GiftCard
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<GiftCard>, StructuredQuery<GiftCard> {

  @Key
  @Property(name = "GiftCardID",
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
  java.lang.Integer getGiftCardID();

  void setGiftCardID(java.lang.Integer _giftCardID);

  @Property(name = "GiftCardNO",
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
  java.lang.String getGiftCardNO();

  void setGiftCardNO(java.lang.String _giftCardNO);

  @Property(name = "Amount",
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

  @Property(name = "ExperationDate",
      type = "Edm.DateTimeOffset",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.sql.Timestamp getExperationDate();

  void setExperationDate(java.sql.Timestamp _experationDate);

  @Property(name = "OwnerName",
      type = "Edm.String",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.String getOwnerName();

  void setOwnerName(java.lang.String _ownerName);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(name = "GetActualAmount",
        type = OperationType.FUNCTION,
        isComposable = false,
        referenceType = java.lang.Double.class, returnType = "Edm.Double")
    Invoker<Double> getActualAmount(
        @Parameter(name = "bonusRate", type = "Edm.Double", nullable = true) java.lang.Double bonusRate
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "GiftCardID",
        type = "Edm.Int32")
    Annotatable getGiftCardIDAnnotations();

    @AnnotationsForProperty(name = "GiftCardNO",
        type = "Edm.String")
    Annotatable getGiftCardNOAnnotations();

    @AnnotationsForProperty(name = "Amount",
        type = "Edm.Double")
    Annotatable getAmountAnnotations();

    @AnnotationsForProperty(name = "ExperationDate",
        type = "Edm.DateTimeOffset")
    Annotatable getExperationDateAnnotations();

    @AnnotationsForProperty(name = "OwnerName",
        type = "Edm.String")
    Annotatable getOwnerNameAnnotations();

  }

}
