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
import org.sitenetsoft.olinguito.ext.proxy.api.OperationType;
// CHECKSTYLE:ON (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Key;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@org.sitenetsoft.olinguito.ext.proxy.api.annotations.Namespace("ODataDemo")
@org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType(name = "Product",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Product
    extends org.sitenetsoft.olinguito.ext.proxy.api.Annotatable,
    org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Product>, org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery<Product> {

  @Key
  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "ID",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Name",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Description",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "ReleaseDate",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "DiscontinuedDate",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Rating",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "Price",
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

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.NavigationProperty(name = "Categories",
      type = "ODataDemo.Category",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "Categories",
      containsTarget = false)
  CategoryCollection getCategories();

  void setCategories(CategoryCollection _categories);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.NavigationProperty(name = "Supplier",
      type = "ODataDemo.Supplier",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "Suppliers",
      containsTarget = false)
  Supplier getSupplier();

  void setSupplier(Supplier _supplier);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.NavigationProperty(name = "ProductDetail",
      type = "ODataDemo.ProductDetail",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "ProductDetails",
      containsTarget = false)
  ProductDetail getProductDetail();

  void setProductDetail(ProductDetail _productDetail);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Operation(name = "Discount",
        type = OperationType.ACTION,
        referenceType = java.lang.Double.class, returnType = "Edm.Double")
        org.sitenetsoft.olinguito.ext.proxy.api.Invoker<java.lang.Double>
        discount(
            @Parameter(name = "discountPercentage", type = "Edm.Int32", nullable = false) java.lang.Integer discountPercentage
        );

  }

  Annotations annotations();

  interface Annotations {

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "ID",
        type = "Edm.Int32")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getIDAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Name",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getNameAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Description",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getDescriptionAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "ReleaseDate",
        type = "Edm.DateTimeOffset")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getReleaseDateAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "DiscontinuedDate",
        type = "Edm.DateTimeOffset")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getDiscontinuedDateAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Rating",
        type = "Edm.Int16")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getRatingAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "Price",
        type = "Edm.Double")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getPriceAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForNavigationProperty(name = "Categories",
        type = "ODataDemo.Category")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getCategoriesAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForNavigationProperty(name = "Supplier",
        type = "ODataDemo.Supplier")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getSupplierAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForNavigationProperty(name = "ProductDetail",
        type = "ODataDemo.ProductDetail")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getProductDetailAnnotations();
  }

}
