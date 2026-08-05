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
import org.sitenetsoft.olinguito.ext.proxy.api.*;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
// CHECKSTYLE:ON (Maven checkstyle)
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "Company",
    openType = true,
    hasStream = false,
    isAbstract = false)
public interface Company
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Company>, StructuredQuery<Company>,
    AbstractOpenType {

  @Key
  @Property(name = "CompanyID",
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
  java.lang.Integer getCompanyID();

  void setCompanyID(java.lang.Integer _companyID);

  @Property(name = "CompanyCategory",
      type = "Microsoft.Test.OData.Services.ODataWCFService.CompanyCategory",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  CompanyCategory
      getCompanyCategory();

      void
      setCompanyCategory(
          CompanyCategory _companyCategory);

  @Property(name = "Revenue",
      type = "Edm.Int64",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Long getRevenue();

  void setRevenue(java.lang.Long _revenue);

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

  @Property(name = "Address",
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
  Address getAddress();

      void
      setAddress(
          Address _address);

  @NavigationProperty(name = "Employees",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Employee",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "Employees",
      containsTarget = false)
  EmployeeCollection
      getEmployees();

      void
      setEmployees(
          EmployeeCollection _employees);

  @NavigationProperty(name = "VipCustomer",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Customer",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "VipCustomer",
      containsTarget = false)
  Customer
      getVipCustomer();

      void
      setVipCustomer(
          Customer _vipCustomer);

  @NavigationProperty(name = "Departments",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Department",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "Departments",
      containsTarget = false)
  DepartmentCollection
      getDepartments();

      void
      setDepartments(
          DepartmentCollection _departments);

  @NavigationProperty(name = "CoreDepartment",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Department",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "Departments",
      containsTarget = false)
  Department
      getCoreDepartment();

      void
      setCoreDepartment(
          Department _coreDepartment);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(name = "GetEmployeesCount",
        type = OperationType.FUNCTION,
        isComposable = false,
        referenceType = java.lang.Integer.class, returnType = "Edm.Int32")
    Invoker<Integer> getEmployeesCount(
        );

    @Operation(name = "IncreaseRevenue",
        type = OperationType.ACTION,
        referenceType = java.lang.Long.class, returnType = "Edm.Int64")
    Invoker<Long> increaseRevenue(
        @Parameter(name = "IncreaseValue", type = "Edm.Int64", nullable = true) java.lang.Long increaseValue
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "CompanyID",
        type = "Edm.Int32")
    Annotatable getCompanyIDAnnotations();

    @AnnotationsForProperty(name = "CompanyCategory",
        type = "Microsoft.Test.OData.Services.ODataWCFService.CompanyCategory")
    Annotatable getCompanyCategoryAnnotations();

    @AnnotationsForProperty(name = "Revenue",
        type = "Edm.Int64")
    Annotatable getRevenueAnnotations();

    @AnnotationsForProperty(name = "Name",
        type = "Edm.String")
    Annotatable getNameAnnotations();

    @AnnotationsForProperty(name = "Address",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Address")
    Annotatable getAddressAnnotations();

    @AnnotationsForNavigationProperty(name = "Employees",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Employee")
    Annotatable getEmployeesAnnotations();

    @AnnotationsForNavigationProperty(name = "VipCustomer",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Customer")
    Annotatable getVipCustomerAnnotations();

    @AnnotationsForNavigationProperty(name = "Departments",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Department")
    Annotatable getDepartmentsAnnotations();

    @AnnotationsForNavigationProperty(name = "CoreDepartment",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Department")
    Annotatable getCoreDepartmentAnnotations();
  }

}
