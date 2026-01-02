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
@org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType(name = "StoredPI",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface StoredPI
    extends org.sitenetsoft.olinguito.ext.proxy.api.Annotatable,
    org.sitenetsoft.olinguito.ext.proxy.api.EntityType<StoredPI>, org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery<StoredPI> {

  @Key
  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "StoredPIID",
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
  java.lang.Integer getStoredPIID();

  void setStoredPIID(java.lang.Integer _storedPIID);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "PIName",
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
  java.lang.String getPIName();

  void setPIName(java.lang.String _pIName);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "PIType",
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
  java.lang.String getPIType();

  void setPIType(java.lang.String _pIType);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "CreatedDate",
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

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "StoredPIID",
        type = "Edm.Int32")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getStoredPIIDAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "PIName",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getPINameAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "PIType",
        type = "Edm.String")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getPITypeAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "CreatedDate",
        type = "Edm.DateTimeOffset")
    org.sitenetsoft.olinguito.ext.proxy.api.Annotatable getCreatedDateAnnotations();

  }

}
