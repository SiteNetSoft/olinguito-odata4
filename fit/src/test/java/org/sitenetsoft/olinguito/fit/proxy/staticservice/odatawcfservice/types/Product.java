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
@EntityType(name = "Product",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Product
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Product>, StructuredQuery<Product> {

  @Key
  @Property(name = "ProductID",
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
  java.lang.Integer getProductID();

  void setProductID(java.lang.Integer _productID);

  @Property(name = "Name",
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
  java.lang.String getName();

  void setName(java.lang.String _name);

  @Property(name = "QuantityPerUnit",
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
  java.lang.String getQuantityPerUnit();

  void setQuantityPerUnit(java.lang.String _quantityPerUnit);

  @Property(name = "UnitPrice",
      type = "Edm.Single",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Float getUnitPrice();

  void setUnitPrice(java.lang.Float _unitPrice);

  @Property(name = "QuantityInStock",
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
  java.lang.Integer getQuantityInStock();

  void setQuantityInStock(java.lang.Integer _quantityInStock);

  @Property(name = "Discontinued",
      type = "Edm.Boolean",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Boolean getDiscontinued();

  void setDiscontinued(java.lang.Boolean _discontinued);

  @Property(name = "UserAccess",
      type = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  AccessLevel
      getUserAccess();

      void
      setUserAccess(
          AccessLevel _userAccess);

  @Property(name = "SkinColor",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Color",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  Color getSkinColor();

      void
      setSkinColor(
          Color _skinColor);

  @Property(name = "CoverColors",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Color",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  PrimitiveCollection<Color>
      getCoverColors();

      void
      setCoverColors(
          PrimitiveCollection<Color> _coverColors);

  @NavigationProperty(name = "Details",
      type = "Microsoft.Test.OData.Services.ODataWCFService.ProductDetail",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "ProductDetails",
      containsTarget = false)
  ProductDetailCollection
      getDetails();

      void
      setDetails(
          ProductDetailCollection _details);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(
        name = "GetProductDetails",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = ProductDetailCollection.class,
        returnType = "Collection(Microsoft.Test.OData.Services.ODataWCFService.ProductDetail)")
    ProductDetailCollectionComposableInvoker
        getProductDetails(
            @Parameter(name = "count", type = "Edm.Int32", nullable = true) java.lang.Integer count
        );

    @Operation(
        name = "AddAccessRight",
        type = OperationType.ACTION,
        referenceType = AccessLevel.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel")
    Invoker<AccessLevel>
        addAccessRight(
            @Parameter(name = "accessRight", type = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel",
                nullable = true) AccessLevel accessRight
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "ProductID",
        type = "Edm.Int32")
    Annotatable getProductIDAnnotations();

    @AnnotationsForProperty(name = "Name",
        type = "Edm.String")
    Annotatable getNameAnnotations();

    @AnnotationsForProperty(name = "QuantityPerUnit",
        type = "Edm.String")
    Annotatable getQuantityPerUnitAnnotations();

    @AnnotationsForProperty(name = "UnitPrice",
        type = "Edm.Single")
    Annotatable getUnitPriceAnnotations();

    @AnnotationsForProperty(name = "QuantityInStock",
        type = "Edm.Int32")
    Annotatable getQuantityInStockAnnotations();

    @AnnotationsForProperty(name = "Discontinued",
        type = "Edm.Boolean")
    Annotatable getDiscontinuedAnnotations();

    @AnnotationsForProperty(name = "UserAccess",
        type = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel")
    Annotatable getUserAccessAnnotations();

    @AnnotationsForProperty(name = "SkinColor",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Color")
    Annotatable getSkinColorAnnotations();

    @AnnotationsForProperty(name = "CoverColors",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Color")
    Annotatable getCoverColorsAnnotations();

    @AnnotationsForNavigationProperty(name = "Details",
        type = "Microsoft.Test.OData.Services.ODataWCFService.ProductDetail")
    Annotatable getDetailsAnnotations();
  }

}
