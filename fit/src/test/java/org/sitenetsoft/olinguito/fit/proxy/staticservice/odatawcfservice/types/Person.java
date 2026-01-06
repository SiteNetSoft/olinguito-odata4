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
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.ext.proxy.api.*;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
// CHECKSTYLE:ON (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "Person",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Person
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Person>, StructuredQuery<Person> {

  @Key
  @Property(name = "PersonID",
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
  java.lang.Integer getPersonID();

  void setPersonID(java.lang.Integer _personID);

  @Property(name = "FirstName",
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

  @Property(name = "LastName",
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

  @Property(name = "MiddleName",
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
  java.lang.String getMiddleName();

  void setMiddleName(java.lang.String _middleName);

  @Property(name = "HomeAddress",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Address",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  Address
      getHomeAddress();

      void
      setHomeAddress(
          Address _homeAddress);

  @Property(name = "Home",
      type = "Edm.GeographyPoint",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  Point getHome();

  void setHome(Point _home);

  @Property(name = "Numbers",
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
  PrimitiveCollection<String> getNumbers();

  void setNumbers(PrimitiveCollection<String> _numbers);

  @Property(name = "Emails",
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
  PrimitiveCollection<String> getEmails();

  void setEmails(PrimitiveCollection<String> _emails);

  @NavigationProperty(name = "Parent",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Person",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "People",
      containsTarget = false)
  Person getParent();

  void setParent(
      Person _parent);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(
        name = "GetHomeAddress",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = HomeAddress.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.HomeAddress")
    HomeAddressComposableInvoker
        getHomeAddress(
        );

    @Operation(
        name = "ResetAddress",
        type = OperationType.ACTION,
        referenceType = Person.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.Person")
    StructuredInvoker<Person>
        resetAddress(
            @Parameter(name = "addresses", type = "Collection(Microsoft.Test.OData.Services.ODataWCFService.Address)",
                nullable = false) AddressCollection addresses,
            @Parameter(name = "index", type = "Edm.Int32", nullable = false) java.lang.Integer index
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "PersonID",
        type = "Edm.Int32")
    Annotatable getPersonIDAnnotations();

    @AnnotationsForProperty(name = "FirstName",
        type = "Edm.String")
    Annotatable getFirstNameAnnotations();

    @AnnotationsForProperty(name = "LastName",
        type = "Edm.String")
    Annotatable getLastNameAnnotations();

    @AnnotationsForProperty(name = "MiddleName",
        type = "Edm.String")
    Annotatable getMiddleNameAnnotations();

    @AnnotationsForProperty(name = "HomeAddress",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Address")
    Annotatable getHomeAddressAnnotations();

    @AnnotationsForProperty(name = "Home",
        type = "Edm.GeographyPoint")
    Annotatable getHomeAnnotations();

    @AnnotationsForProperty(name = "Numbers",
        type = "Edm.String")
    Annotatable getNumbersAnnotations();

    @AnnotationsForProperty(name = "Emails",
        type = "Edm.String")
    Annotatable getEmailsAnnotations();

    @AnnotationsForNavigationProperty(name = "Parent",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Person")
    Annotatable getParentAnnotations();
  }

}
