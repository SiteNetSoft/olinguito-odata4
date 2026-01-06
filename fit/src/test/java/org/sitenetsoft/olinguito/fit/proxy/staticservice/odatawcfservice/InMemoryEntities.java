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
package org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice;

// CHECKSTYLE:OFF (Maven checkstyle)
import java.io.InputStream;

// CHECKSTYLE:ON (Maven checkstyle)
import java.io.Serializable;

import org.sitenetsoft.olinguito.ext.proxy.api.*;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
import org.sitenetsoft.olinguito.ext.proxy.api.ComplexType;
import org.sitenetsoft.olinguito.ext.proxy.api.EdmStreamValue;
import org.sitenetsoft.olinguito.ext.proxy.api.EntityCollection;
import org.sitenetsoft.olinguito.ext.proxy.api.EntityType;
import org.sitenetsoft.olinguito.ext.proxy.api.PersistenceManager;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.AccessLevel;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.Address;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.Color;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.Company;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.Customer;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.LabourUnion;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.Person;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.PersonComposableInvoker;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.ProductCollection;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.ProductCollectionComposableInvoker;
import org.sitenetsoft.olinguito.fit.proxy.staticservice.odatawcfservice.types.StoredPI;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityContainer(name = "InMemoryEntities",
    namespace = "Microsoft.Test.OData.Services.ODataWCFService")
public interface InMemoryEntities extends PersistenceManager {

  Accounts getAccounts();

  StoredPIs getStoredPIs();

  Customers getCustomers();

  Products getProducts();

  OrderDetails getOrderDetails();

  Departments getDepartments();

  Employees getEmployees();

  Orders getOrders();

  People getPeople();

  SubscriptionTemplates getSubscriptionTemplates();

  ProductReviews getProductReviews();

  ProductDetails getProductDetails();

  @Singleton(
      name = "PublicCompany",
      container = "Microsoft.Test.OData.Services.ODataWCFService.InMemoryEntities")
  Company
      getPublicCompany();

  @Singleton(
      name = "DefaultStoredPI",
      container = "Microsoft.Test.OData.Services.ODataWCFService.InMemoryEntities")
  StoredPI
      getDefaultStoredPI();

  @Singleton(
      name = "VipCustomer",
      container = "Microsoft.Test.OData.Services.ODataWCFService.InMemoryEntities")
  Customer
      getVipCustomer();

  @Singleton(
      name = "Company",
      container = "Microsoft.Test.OData.Services.ODataWCFService.InMemoryEntities")
  Company getCompany();

  @Singleton(
      name = "Boss",
      container = "Microsoft.Test.OData.Services.ODataWCFService.InMemoryEntities")
  Person getBoss();

  @Singleton(
      name = "LabourUnion",
      container = "Microsoft.Test.OData.Services.ODataWCFService.InMemoryEntities")
  LabourUnion
      getLabourUnion();

  Operations operations();

  public interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(name = "GetBossEmails",
        type = OperationType.FUNCTION,
        isComposable = false,
        referenceType = PrimitiveCollection.class,
        returnType = "Collection(Edm.String)")
    PrimitiveCollectionInvoker<org.sitenetsoft.olinguito.ext.proxy.api.PrimitiveCollection<String>>
        getBossEmails(
            @Parameter(name = "start", type = "Edm.Int32", nullable = false) java.lang.Integer start,
            @Parameter(name = "count", type = "Edm.Int32", nullable = false) java.lang.Integer count
        );

    @Operation(
        name = "GetPerson2",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = Person.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.Person")
    PersonComposableInvoker
        getPerson2(
            @Parameter(name = "city", type = "Edm.String", nullable = false) java.lang.String city
        );

    @Operation(
        name = "GetDefaultColor",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = Color.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.Color")
    Invoker<Color>
        getDefaultColor(
        );

    @Operation(
        name = "GetPerson",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = Person.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.Person")
    PersonComposableInvoker
        getPerson(
            @Parameter(name = "address",
                type = "Microsoft.Test.OData.Services.ODataWCFService.Address", nullable = false) Address address
        );

    @Operation(name = "GetProductsByAccessLevel",
        type = OperationType.FUNCTION,
        isComposable = false,
        referenceType = PrimitiveCollection.class,
        returnType = "Collection(Edm.String)")
    PrimitiveCollectionInvoker<org.sitenetsoft.olinguito.ext.proxy.api.PrimitiveCollection<String>>
        getProductsByAccessLevel(
            @Parameter(name = "accessLevel",
                type = "Microsoft.Test.OData.Services.ODataWCFService.AccessLevel", nullable = false) AccessLevel accessLevel
        );

    @Operation(
        name = "GetAllProducts",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = ProductCollection.class,
        returnType = "Collection(Microsoft.Test.OData.Services.ODataWCFService.Product)")
    ProductCollectionComposableInvoker
        getAllProducts(
        );

    @Operation(
        name = "ResetBossAddress",
        type = OperationType.ACTION,
        referenceType = Address.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.Address")
    StructuredInvoker<Address>
        resetBossAddress(
            @Parameter(name = "address",
                type = "Microsoft.Test.OData.Services.ODataWCFService.Address", nullable = false) Address address
        );

    @Operation(name = "ResetDataSource",
        type = OperationType.ACTION)
    Invoker<Void> resetDataSource(
        );

    @Operation(name = "Discount",
        type = OperationType.ACTION)
    Invoker<Void> discount(
        @Parameter(name = "percentage", type = "Edm.Int32",
            nullable = false) java.lang.Integer percentage
        );

    @Operation(name = "ResetBossEmail",
        type = OperationType.ACTION,
        referenceType = PrimitiveCollection.class,
        returnType = "Collection(Edm.String)")
    PrimitiveCollectionInvoker<org.sitenetsoft.olinguito.ext.proxy.api.PrimitiveCollection<String>>
        resetBossEmail(
            @Parameter(name = "emails", type = "Collection(Edm.String)",
                nullable = false) PrimitiveCollection<String> emails
        );

  }

  <NE extends EntityType<?>> NE newEntityInstance(Class<NE> ref);

  <T extends EntityType<?>, NEC extends EntityCollection<T, ?, ?>> NEC newEntityCollection(Class<NEC> ref);

  <NE extends ComplexType<?>> NE newComplexInstance(Class<NE> ref);

  <T extends ComplexType<?>, NEC extends ComplexCollection<T, ?, ?>> NEC newComplexCollection(Class<NEC> ref);

  <T extends Serializable, NEC extends PrimitiveCollection<T>> NEC newPrimitiveCollection(Class<T> ref);

  EdmStreamValue newEdmStreamValue(String contentType, InputStream stream);
}
