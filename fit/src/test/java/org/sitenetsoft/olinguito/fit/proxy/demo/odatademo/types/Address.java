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
import org.sitenetsoft.olinguito.ext.proxy.api.StructuredQuery;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.AnnotationsForProperty;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.ComplexType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Namespace;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property;
import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;

// CHECKSTYLE:ON (Maven checkstyle)

@Namespace("ODataDemo")
@ComplexType(name = "Address",
    isOpenType = false,
    isAbstract = false)
public interface Address
    extends org.sitenetsoft.olinguito.ext.proxy.api.ComplexType<Address>,
        StructuredQuery<Address> {

  @Property(name = "Street",
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
  java.lang.String getStreet();

  void setStreet(java.lang.String _street);

  @Property(name = "City",
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
  java.lang.String getCity();

  void setCity(java.lang.String _city);

  @Property(name = "State",
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
  java.lang.String getState();

  void setState(java.lang.String _state);

  @Property(name = "ZipCode",
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
  java.lang.String getZipCode();

  void setZipCode(java.lang.String _zipCode);

  @Property(name = "Country",
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
  java.lang.String getCountry();

  void setCountry(java.lang.String _country);

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "Street",
        type = "Edm.String")
    Annotatable getStreetAnnotations();

    @AnnotationsForProperty(name = "City",
        type = "Edm.String")
    Annotatable getCityAnnotations();

    @AnnotationsForProperty(name = "State",
        type = "Edm.String")
    Annotatable getStateAnnotations();

    @AnnotationsForProperty(name = "ZipCode",
        type = "Edm.String")
    Annotatable getZipCodeAnnotations();

    @AnnotationsForProperty(name = "Country",
        type = "Edm.String")
    Annotatable getCountryAnnotations();

  }

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }
}
