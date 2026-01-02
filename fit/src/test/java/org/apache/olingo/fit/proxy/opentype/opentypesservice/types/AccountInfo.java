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
package org.sitenetsoft.olinguito.fit.proxy.opentype.opentypesservice.types;

// CHECKSTYLE:OFF (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.AbstractOpenType;
import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;

// CHECKSTYLE:ON (Maven checkstyle)

@org.sitenetsoft.olinguito.ext.proxy.api.annotations.Namespace("Microsoft.Test.OData.Services.OpenTypesServiceV4")
@org.sitenetsoft.olinguito.ext.proxy.api.annotations.ComplexType(name = "AccountInfo",
    isOpenType = true,
    isAbstract = false)
public interface AccountInfo
    extends org.sitenetsoft.olinguito.ext.proxy.api.ComplexType<AccountInfo>,
    org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery<AccountInfo>, AbstractOpenType {

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "FirstName",
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
  java.lang.String getFirstName();

  void setFirstName(java.lang.String _firstName);

  @org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property(name = "LastName",
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
  java.lang.String getLastName();

  void setLastName(java.lang.String _lastName);

  Annotations annotations();

  interface Annotations {

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "FirstName",
        type = "Edm.String")
    Annotatable getFirstNameAnnotations();

    @org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty(name = "LastName",
        type = "Edm.String")
    Annotatable getLastNameAnnotations();

  }

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }
}
