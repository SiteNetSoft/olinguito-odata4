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

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "Subscription",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Subscription
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Subscription>,
        StructuredQuery<Subscription> {

  @Key
  @Property(name = "SubscriptionID",
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
  java.lang.Integer getSubscriptionID();

  void setSubscriptionID(java.lang.Integer _subscriptionID);

  @Property(name = "TemplateGuid",
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
  java.lang.String getTemplateGuid();

  void setTemplateGuid(java.lang.String _templateGuid);

  @Property(name = "Title",
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
  java.lang.String getTitle();

  void setTitle(java.lang.String _title);

  @Property(name = "Category",
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
  java.lang.String getCategory();

  void setCategory(java.lang.String _category);

  @Property(name = "CreatedDate",
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
  java.sql.Timestamp getCreatedDate();

  void setCreatedDate(java.sql.Timestamp _createdDate);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "SubscriptionID",
        type = "Edm.Int32")
    Annotatable getSubscriptionIDAnnotations();

    @AnnotationsForProperty(name = "TemplateGuid",
        type = "Edm.String")
    Annotatable getTemplateGuidAnnotations();

    @AnnotationsForProperty(name = "Title",
        type = "Edm.String")
    Annotatable getTitleAnnotations();

    @AnnotationsForProperty(name = "Category",
        type = "Edm.String")
    Annotatable getCategoryAnnotations();

    @AnnotationsForProperty(name = "CreatedDate",
        type = "Edm.DateTimeOffset")
    Annotatable getCreatedDateAnnotations();

  }

}
