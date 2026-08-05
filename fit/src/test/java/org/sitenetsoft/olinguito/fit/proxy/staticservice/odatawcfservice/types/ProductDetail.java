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
import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;
import org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
import org.sitenetsoft.olinguito.ext.proxy.api.OperationType;

// CHECKSTYLE:ON (Maven checkstyle)


@KeyRef(ProductDetailKey.class)
@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "ProductDetail",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface ProductDetail
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<ProductDetail>,
        StructuredQuery<ProductDetail> {

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

  @Key
  @Property(name = "ProductDetailID",
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
  java.lang.Integer getProductDetailID();

  void setProductDetailID(java.lang.Integer _productDetailID);

  @Property(name = "ProductName",
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
  java.lang.String getProductName();

  void setProductName(java.lang.String _productName);

  @Property(name = "Description",
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
  java.lang.String getDescription();

  void setDescription(java.lang.String _description);

  @NavigationProperty(name = "RelatedProduct",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Product",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "Products",
      containsTarget = false)
  Product
      getRelatedProduct();

      void
      setRelatedProduct(
          Product _relatedProduct);

  @NavigationProperty(name = "Reviews",
      type = "Microsoft.Test.OData.Services.ODataWCFService.ProductReview",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "ProductReviews",
      containsTarget = false)
  ProductReviewCollection
      getReviews();

      void
      setReviews(
          ProductReviewCollection _reviews);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(
        name = "GetRelatedProduct",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = Product.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.Product")
    ProductComposableInvoker
        getRelatedProduct(
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "ProductID",
        type = "Edm.Int32")
    Annotatable getProductIDAnnotations();

    @AnnotationsForProperty(name = "ProductDetailID",
        type = "Edm.Int32")
    Annotatable getProductDetailIDAnnotations();

    @AnnotationsForProperty(name = "ProductName",
        type = "Edm.String")
    Annotatable getProductNameAnnotations();

    @AnnotationsForProperty(name = "Description",
        type = "Edm.String")
    Annotatable getDescriptionAnnotations();

    @AnnotationsForNavigationProperty(name = "RelatedProduct",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Product")
    Annotatable getRelatedProductAnnotations();

    @AnnotationsForNavigationProperty(name = "Reviews",
        type = "Microsoft.Test.OData.Services.ODataWCFService.ProductReview")
    Annotatable getReviewsAnnotations();
  }

}
