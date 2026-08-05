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

import java.util.concurrent.Future;

import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;
import org.sitenetsoft.olinguito.ext.proxy.api.PrimitiveCollection;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "Customer",
    openType = false,
    hasStream = false,
    isAbstract = false,
    baseType = "Microsoft.Test.OData.Services.ODataWCFService.Person")
public interface Customer extends Person {

  @Override
  Customer load();

  @Override
  Future<? extends Customer> loadAsync();

  @Override
  Customer refs();

  @Override
  Customer expand(String... expand);

  @Override
  Customer select(String... select);

  @Override
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

  @Override
  void setPersonID(java.lang.Integer _personID);

  @Override
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

  @Override
  void setFirstName(java.lang.String _firstName);

  @Override
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

  @Override
  void setLastName(java.lang.String _lastName);

  @Override
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

  @Override
  void setMiddleName(java.lang.String _middleName);

  @Override
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

  @Override
      void
      setHomeAddress(
          Address _homeAddress);

  @Override
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

  @Override
  void setHome(Point _home);

  @Override
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

  @Override
  void setNumbers(PrimitiveCollection<String> _numbers);

  @Override
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

  @Override
  void setEmails(PrimitiveCollection<String> _emails);

  @Property(name = "City",
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
  java.lang.String getCity();

  void setCity(java.lang.String _city);

  @Property(name = "Birthday",
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
  java.sql.Timestamp getBirthday();

  void setBirthday(java.sql.Timestamp _birthday);

  @Property(name = "TimeBetweenLastTwoOrders",
      type = "Edm.Duration",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.math.BigDecimal getTimeBetweenLastTwoOrders();

  void setTimeBetweenLastTwoOrders(java.math.BigDecimal _timeBetweenLastTwoOrders);

  @Override
  @NavigationProperty(name = "Parent",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Person",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "People",
      containsTarget = false)
  Person getParent();

  @Override
  void setParent(
      Person _parent);

  @NavigationProperty(name = "Orders",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Order",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "Orders",
      containsTarget = false)
  OrderCollection
      getOrders();

      void
      setOrders(
          OrderCollection _orders);

  @NavigationProperty(name = "Company",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Company",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "Company",
      containsTarget = false)
  Company getCompany();

      void
      setCompany(
          Company _company);

  @Override
  Operations operations();

  interface Operations
      extends
      Person.Operations {
    // No additional methods needed for now.
  }

  @Override
  Annotations annotations();

  interface Annotations
      extends
      Person.Annotations {

    @Override
    @AnnotationsForProperty(name = "PersonID",
        type = "Edm.Int32")
    Annotatable getPersonIDAnnotations();

    @Override
    @AnnotationsForProperty(name = "FirstName",
        type = "Edm.String")
    Annotatable getFirstNameAnnotations();

    @Override
    @AnnotationsForProperty(name = "LastName",
        type = "Edm.String")
    Annotatable getLastNameAnnotations();

    @Override
    @AnnotationsForProperty(name = "MiddleName",
        type = "Edm.String")
    Annotatable getMiddleNameAnnotations();

    @Override
    @AnnotationsForProperty(name = "HomeAddress",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Address")
    Annotatable getHomeAddressAnnotations();

    @Override
    @AnnotationsForProperty(name = "Home",
        type = "Edm.GeographyPoint")
    Annotatable getHomeAnnotations();

    @Override
    @AnnotationsForProperty(name = "Numbers",
        type = "Edm.String")
    Annotatable getNumbersAnnotations();

    @Override
    @AnnotationsForProperty(name = "Emails",
        type = "Edm.String")
    Annotatable getEmailsAnnotations();

    @AnnotationsForProperty(name = "City",
        type = "Edm.String")
    Annotatable getCityAnnotations();

    @AnnotationsForProperty(name = "Birthday",
        type = "Edm.DateTimeOffset")
    Annotatable getBirthdayAnnotations();

    @AnnotationsForProperty(name = "TimeBetweenLastTwoOrders",
        type = "Edm.Duration")
    Annotatable getTimeBetweenLastTwoOrdersAnnotations();

    @Override
    @AnnotationsForNavigationProperty(name = "Parent",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Person")
    Annotatable getParentAnnotations();

    @AnnotationsForNavigationProperty(name = "Orders",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Order")
    Annotatable getOrdersAnnotations();

    @AnnotationsForNavigationProperty(name = "Company",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Company")
    Annotatable getCompanyAnnotations();
  }

}
