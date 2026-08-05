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

// CHECKSTYLE:ON (Maven checkstyle)

@KeyRef(ProductReviewKey.class)
@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "ProductReview",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface ProductReview
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<ProductReview>,
        StructuredQuery<ProductReview> {

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

  @Key
  @Property(name = "ReviewTitle",
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
  java.lang.String getReviewTitle();

  void setReviewTitle(java.lang.String _reviewTitle);

  @Key
  @Property(name = "RevisionID",
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
  java.lang.Integer getRevisionID();

  void setRevisionID(java.lang.Integer _revisionID);

  @Property(name = "Comment",
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
  java.lang.String getComment();

  void setComment(java.lang.String _comment);

  @Property(name = "Author",
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
  java.lang.String getAuthor();

  void setAuthor(java.lang.String _author);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "ProductID",
        type = "Edm.Int32")
    Annotatable getProductIDAnnotations();

    @AnnotationsForProperty(name = "ProductDetailID",
        type = "Edm.Int32")
    Annotatable getProductDetailIDAnnotations();

    @AnnotationsForProperty(name = "ReviewTitle",
        type = "Edm.String")
    Annotatable getReviewTitleAnnotations();

    @AnnotationsForProperty(name = "RevisionID",
        type = "Edm.Int32")
    Annotatable getRevisionIDAnnotations();

    @AnnotationsForProperty(name = "Comment",
        type = "Edm.String")
    Annotatable getCommentAnnotations();

    @AnnotationsForProperty(name = "Author",
        type = "Edm.String")
    Annotatable getAuthorAnnotations();

  }

}
