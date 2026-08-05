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
package org.sitenetsoft.olinguito.fit.proxy.demo.odatademo.types;

// CHECKSTYLE:OFF (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.*;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
// CHECKSTYLE:ON (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@Namespace("ODataDemo")
@EntityType(name = "Product",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Product
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Product>, StructuredQuery<Product> {

  @Key
  @Property(name = "ID",
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
  java.lang.Integer getID();

  void setID(java.lang.Integer _iD);

  @Property(name = "Name",
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
  java.lang.String getName();

  void setName(java.lang.String _name);

  @Property(name = "Description",
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
  java.lang.String getDescription();

  void setDescription(java.lang.String _description);

  @Property(name = "ReleaseDate",
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
  java.sql.Timestamp getReleaseDate();

  void setReleaseDate(java.sql.Timestamp _releaseDate);

  @Property(name = "DiscontinuedDate",
      type = "Edm.DateTimeOffset",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.sql.Timestamp getDiscontinuedDate();

  void setDiscontinuedDate(java.sql.Timestamp _discontinuedDate);

  @Property(name = "Rating",
      type = "Edm.Int16",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Short getRating();

  void setRating(java.lang.Short _rating);

  @Property(name = "Price",
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
  java.lang.Double getPrice();

  void setPrice(java.lang.Double _price);

  @NavigationProperty(name = "Categories",
      type = "ODataDemo.Category",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "Categories",
      containsTarget = false)
  CategoryCollection getCategories();

  void setCategories(CategoryCollection _categories);

  @NavigationProperty(name = "Supplier",
      type = "ODataDemo.Supplier",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "Suppliers",
      containsTarget = false)
  Supplier getSupplier();

  void setSupplier(Supplier _supplier);

  @NavigationProperty(name = "ProductDetail",
      type = "ODataDemo.ProductDetail",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "ProductDetails",
      containsTarget = false)
  ProductDetail getProductDetail();

  void setProductDetail(ProductDetail _productDetail);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(name = "Discount",
        type = OperationType.ACTION,
        referenceType = java.lang.Double.class, returnType = "Edm.Double")
    Invoker<Double>
        discount(
            @Parameter(name = "discountPercentage", type = "Edm.Int32", nullable = false) java.lang.Integer discountPercentage
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "ID",
        type = "Edm.Int32")
    Annotatable getIDAnnotations();

    @AnnotationsForProperty(name = "Name",
        type = "Edm.String")
    Annotatable getNameAnnotations();

    @AnnotationsForProperty(name = "Description",
        type = "Edm.String")
    Annotatable getDescriptionAnnotations();

    @AnnotationsForProperty(name = "ReleaseDate",
        type = "Edm.DateTimeOffset")
    Annotatable getReleaseDateAnnotations();

    @AnnotationsForProperty(name = "DiscontinuedDate",
        type = "Edm.DateTimeOffset")
    Annotatable getDiscontinuedDateAnnotations();

    @AnnotationsForProperty(name = "Rating",
        type = "Edm.Int16")
    Annotatable getRatingAnnotations();

    @AnnotationsForProperty(name = "Price",
        type = "Edm.Double")
    Annotatable getPriceAnnotations();

    @AnnotationsForNavigationProperty(name = "Categories",
        type = "ODataDemo.Category")
    Annotatable getCategoriesAnnotations();

    @AnnotationsForNavigationProperty(name = "Supplier",
        type = "ODataDemo.Supplier")
    Annotatable getSupplierAnnotations();

    @AnnotationsForNavigationProperty(name = "ProductDetail",
        type = "ODataDemo.ProductDetail")
    Annotatable getProductDetailAnnotations();
  }

}
