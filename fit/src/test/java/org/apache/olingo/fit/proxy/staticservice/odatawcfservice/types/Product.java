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
import org.sitenetsoft.olinguito.ext.proxy.api.OperationType;
// CHECKSTYLE:ON (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Key;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@org.sitenetsoft.olinguito.ext.proxy.api.annotations.Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType(name = "Product",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Product
    extends org.sitenetsoft.olinguito.ext.proxy.api.Annotatable,
    org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Product>, org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery<Product> {

  @Key
  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "ProductID",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Name",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "QuantityPerUnit",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "UnitPrice",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "QuantityInStock",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Discontinued",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "UserAccess",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "SkinColor",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "CoverColors",
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
      org.sitenetsoft.olinguito.ext.proxy.api.PrimitiveCollection<Color>
      getCoverColors();

      void
      setCoverColors(
          org.sitenetsoft.olinguito.ext.proxy.api.PrimitiveCollection<Color> _coverColors);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.NavigationProperty(name = "Details",
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

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Operation(
        name = "GetProductDetails",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = ProductDetailCollection.class,
        returnType = "Collection(Microsoft.Test.OData.Services.ODataWCFService.ProductDetail)")
    ProductDetailCollectionComposableInvoker
        getProductDetails(
            @Parameter(name = "count", type = "Edm.Int32", nullable = true) java.lang.Integer count
        );

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Operation(
        name = "AddAccessRight",
        type = OperationType.ACTION,
        referenceType = AccessLevel.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel")
        org.sitenetsoft.olinguito.ext.proxy.api.Invoker<AccessLevel>
        addAccessRight(
            @Parameter(name = "accessRight", type = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel",
                nullable = true) AccessLevel accessRight
        );

  }

  Annotations annotations();

  interface Annotations {

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "ProductID",
        type = "Edm.Int32")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getProductIDAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Name",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getNameAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "QuantityPerUnit",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getQuantityPerUnitAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "UnitPrice",
        type = "Edm.Single")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getUnitPriceAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "QuantityInStock",
        type = "Edm.Int32")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getQuantityInStockAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Discontinued",
        type = "Edm.Boolean")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getDiscontinuedAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "UserAccess",
        type = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getUserAccessAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "SkinColor",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Color")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getSkinColorAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "CoverColors",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Color")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getCoverColorsAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForNavigationProperty(name = "Details",
        type = "Microsoft.Test.OData.Services.ODataWCFService.ProductDetail")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getDetailsAnnotations();
  }

}
