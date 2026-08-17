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
 *
 * Copyright 2026 SiteNetSoft - Upgraded Apache HttpComponents 4.x to 5.x
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - Port OLINGO-1369: tests for blank-space and embedded-quote encoding
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 4: test for $schemaversion client URI builder
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 4: tests for key-as-segment URI building
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 3 Task 4 fix round 1: tests for key-segment encoding,
 * empty/null key values and the enum key overload
 */
package org.sitenetsoft.olinguito.client.core.uri;

import java.io.Serial;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.client.api.domain.ClientPrimitiveValue;
import org.sitenetsoft.olinguito.client.api.domain.ClientValue;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.api.uri.QueryOption;
import org.sitenetsoft.olinguito.client.api.uri.URIBuilder;
import org.sitenetsoft.olinguito.client.core.AbstractTest;
import org.sitenetsoft.olinguito.client.core.ODataClientFactory;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEnumType;
import org.junit.jupiter.api.Test;

class URIBuilderTest extends AbstractTest {

  private static final String SERVICE_ROOT = "http://host/service";

  @Test
  void metadata() throws URISyntaxException {
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendMetadataSegment().build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/$metadata").build(), uri);
  }

  @Test
  void keySegmentWithBlankSpaceIsEncoded() {
    // OLINGO-1369: blank spaces in a key segment must be percent-encoded
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("ObjektEbeneSet").
        appendKeySegment("Landwirtschaft und Tierzucht").build();

    assertEquals(SERVICE_ROOT + "/ObjektEbeneSet('Landwirtschaft%20und%20Tierzucht')", uri.toASCIIString());
  }

  @Test
  void multiKeySegmentWithBlankSpaceIsEncoded() {
    // OLINGO-1369: blank spaces in a multi-key segment must be percent-encoded
    final Map<String, Object> key = new LinkedHashMap<>();
    key.put("Nr", "121");
    key.put("Bezeichnung", "Landwirtschaft und Tierzucht");
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("ObjektEbeneSet").
        appendKeySegment(key).build();

    assertEquals(SERVICE_ROOT + "/ObjektEbeneSet(Nr='121',Bezeichnung='Landwirtschaft%20und%20Tierzucht')",
        uri.toASCIIString());
  }

  @Test
  void keySegmentWithEmbeddedSingleQuoteIsDoubled() {
    // OData literal escaping: an embedded single quote must be doubled
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Customers").
        appendKeySegment("O'Brien").build();

    assertEquals(SERVICE_ROOT + "/Customers('O''Brien')", uri.toASCIIString());
  }

  @Test
  void entity() throws URISyntaxException {
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("AnEntitySet").
        appendKeySegment(11).build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/AnEntitySet(11)").build(), uri);

    final Map<String, Object> multiKey = new LinkedHashMap<>();
    multiKey.put("OrderId", -10);
    multiKey.put("ProductId", -10);
    URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("OrderLine").appendKeySegment(multiKey).
        appendPropertySegment("Quantity").appendValueSegment();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/OrderLine(OrderId=-10,ProductId=-10)/Quantity/$value").build(), uriBuilder.build());

    uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Customer").appendKeySegment(-10).
        select("CustomerId", "Name", "Orders").expand("Orders");
    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Customer(-10)").
        addParameter("$select", "CustomerId,Name,Orders").addParameter("$expand", "Orders").build(),
        uriBuilder.build());

    uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Customer").appendKeySegment(-10).appendNavigationSegment("Orders").appendRefSegment();
    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Customer(-10)/Orders/$ref").build(),
        uriBuilder.build());
  }

  @Test
  void expandWithOptions() throws URISyntaxException {
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Products").appendKeySegment(5).
        expandWithOptions("ProductDetails", new LinkedHashMap<>() {
          @Serial
          private static final long serialVersionUID = 3109256773218160485L;

          {
            put(QueryOption.EXPAND, "ProductInfo");
            put(QueryOption.SELECT, "Price");
          }
        }).expand("Orders", "Customers").build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Products(5)").
        addParameter("$expand", "ProductDetails($expand=ProductInfo;$select=Price),Orders,Customers").build(), uri);
  }
  
  @Test
  void expandWithOptionsCount() throws URISyntaxException {
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Products").appendKeySegment(5).
        expandWithOptions("ProductDetails", false, true, new LinkedHashMap<>() {
          @Serial
          private static final long serialVersionUID = 3109256773218160485L;
          {
            put(QueryOption.EXPAND, "ProductInfo");
            put(QueryOption.SELECT, "Price");
          }
        }).expand("Orders", "Customers").build();
    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Products(5)").
        addParameter("$expand", "ProductDetails($expand=ProductInfo;$select=Price)/$count,Orders,Customers")
        .build(), uri);
  }  

  public void expandWithLevels() throws URISyntaxException {
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Products").appendKeySegment(1).
        expandWithOptions("Customer", Collections.<QueryOption, Object> singletonMap(QueryOption.LEVELS, 4)).
        build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Products(1)").
        addParameter("$expand", "Customer($levels=4)").build(), uri);
  }

  @Test
  void count() throws URISyntaxException {
    URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Products").count().build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Products/$count").build(), uri);

    uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Products").count(true).build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Products").
        addParameter("$count", "true").build(), uri);
  }

  @Test
  void filter() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("AnEntitySet").
        filter(client.getFilterFactory().lt("VIN", 16));

    assertEquals("http://host/service/AnEntitySet?%24filter=%28VIN%20lt%2016%29", uriBuilder.build().toASCIIString());

    //    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/AnEntitySet").
    //        addParameter("$filter", "(VIN lt 16)").build(),
    //        uriBuilder.build());
  }

  @Test
  void filterWithParameter() throws URISyntaxException {
    // http://host/service.svc/Employees?$filter=Region eq @p1&@p1='WA'
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Employees").
        filter(client.getFilterFactory().eq("Region", new ParameterAlias("p1"))).
        addParameterAlias("p1", "'WA'");

    assertEquals("http://host/service/Employees?%24filter=%28Region%20eq%20%40p1%29&%40p1='WA'", uriBuilder.build()
        .toASCIIString());

    //    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Employees").
    //        addParameter("$filter", "(Region eq @p1)").addParameter("@p1", "'WA'").build(),
    //        uriBuilder.build());
  }

  @Test
  void expandMoreThenOnce() throws URISyntaxException {
    URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Products").appendKeySegment(5).
        expand("Orders", "Customers").expand("Info").build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Products(5)").
        addParameter("$expand", "Orders,Customers,Info").build(), uri);
  }

  @Test
  void selectMoreThenOnce() throws URISyntaxException {
    URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Customers").appendKeySegment(5).
        select("Name", "Surname").expand("Info").select("Gender").build();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(SERVICE_ROOT + "/Customers(5)").
        addParameter("$select", "Name,Surname,Gender").addParameter("$expand", "Info").build(), uri);
  }

  @Test
  void singleton() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendSingletonSegment("BestProductEverCreated");

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/BestProductEverCreated").build(), uriBuilder.build());
  }

  @Test
  void entityId() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntityIdSegment("Products(0)");

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/$entity").addParameter("$id", "Products(0)").build(), uriBuilder.build());
  }

  @Test
  void boundAction() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment(1).
        appendNavigationSegment("Products").
        appendActionCallSegment("Model.AllOrders");

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/Categories(1)/Products/Model.AllOrders").build(), uriBuilder.build());
  }

  @Test
  void boundOperation() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment(1).
        appendNavigationSegment("Products").
        appendOperationCallSegment("Model.AllOrders");

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/Categories(1)/Products/Model.AllOrders()").build(), uriBuilder.build());
  }

  @Test
  void ref() throws URISyntaxException {
    URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment(1).
        appendNavigationSegment("Products").appendRefSegment();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/Categories(1)/Products/$ref").build(), uriBuilder.build());

    uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment(1).
        appendNavigationSegment("Products").appendRefSegment().id("../../Products(0)");

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/Categories(1)/Products/$ref").addParameter("$id", "../../Products(0)").build(),
        uriBuilder.build());
  }

  @Test
  void derived() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Customers").appendDerivedEntityTypeSegment("Model.VipCustomer").appendKeySegment(1);

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/Customers/Model.VipCustomer(1)").build(), uriBuilder.build());
  }

  @Test
  void crossjoin() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendCrossjoinSegment("Products", "Sales");

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/$crossjoin(Products,Sales)").build(), uriBuilder.build());
  }

  @Test
  void all() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).appendAllSegment();

    assertEquals(new org.apache.hc.core5.net.URIBuilder(
        SERVICE_ROOT + "/$all").build(), uriBuilder.build());
  }

  @Test
  void search() throws URISyntaxException {
    final URIBuilder uriBuilder = client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Products").search("blue OR green");

    assertEquals(new URI("http://host/service/Products?%24search=blue%20OR%20green"), uriBuilder.build());
  }
  
  @Test
  void test1OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).
        appendOperationCallSegment("functionName").count().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/functionName(parameterName%3D'parameterValue')"
        + "/%24count", newUri.toASCIIString());
  }
  
  @Test
  void test2OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendOperationCallSegment("functionName").
        filter("paramName eq 1").format("json").count().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/functionName(parameterName%3D'parameterValue')"
        + "/%24count?%24filter=paramName%20eq%201&%24format=json", newUri.toASCIIString());
  }
  
  @Test
  void test3OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendOperationCallSegment("functionName").
        filter("paramName eq 1").format("json").build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/functionName(parameterName%3D'parameterValue')"
        + "?%24filter=paramName%20eq%201&%24format=json", newUri.toASCIIString());
  }
  
  @Test
  void test4OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").count().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')"
        + "/%24count", newUri.toASCIIString());
  }
  
  @Test
  void test5OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").count().filter("PropertyString eq '1'").build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')"
        + "/%24count?%24filter=PropertyString%20eq%20'1'", newUri.toASCIIString());
  }
  
  @Test
  void test6OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).
        appendOperationCallSegment("functionName").count().filter("PropertyString eq '1'").build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/functionName()"
        + "/%24count?%24filter=PropertyString%20eq%20'1'", newUri.toASCIIString());
  }
  
  @Test
  void test7OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").filter("PropertyString eq '1'").
        appendNavigationSegment("NavSeg").count().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')/NavSeg"
        + "/%24count?%24filter=PropertyString%20eq%20'1'", newUri.toASCIIString());
  }
  
  @Test
  void test8OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").filter("PropertyString eq '1'").
        appendNavigationSegment("NavSeg").appendActionCallSegment("ActionName").count().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')/NavSeg/ActionName"
        + "/%24count?%24filter=PropertyString%20eq%20'1'", newUri.toASCIIString());
  }
  
  @Test
  void test9OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").
        appendNavigationSegment("NavSeg").appendActionCallSegment("ActionName").appendValueSegment().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')"
        + "/NavSeg/ActionName/%24value", newUri.toASCIIString());
  }
  
  @Test
  void test10OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").
        appendNavigationSegment("NavSeg").appendRefSegment().build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')"
        + "/NavSeg/%24ref", newUri.toASCIIString());
  }
  
  @Test
  void test11OLINGO753() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").
        appendNavigationSegment("NavSeg").count().addParameterAlias("first", "'1'").build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().setValue(new ParameterAlias("first")).build();
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D%40first)/"
        + "NavSeg/%24count?%40first='1'", newUri.toASCIIString());
  }
  
  @Test
  void testCustomQueryOption() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        addCustomQueryOption("x", "y").build();
    assertEquals("http://host/service/EntitySet?x=y", uri.toASCIIString());
  }
  
  @Test
  void testCustomQueryOptionWithFilter() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        filter("PropertyString eq '1'").
        addCustomQueryOption("x", "y").build();
    assertEquals("http://host/service/EntitySet?%24filter=PropertyString%20eq%20'1'&x=y", 
        uri.toASCIIString());
  }
  
  @Test
  void testDuplicateCustomQueryOption() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        addCustomQueryOption("x", "z").
        addCustomQueryOption("x", "y").build();
    assertEquals("http://host/service/EntitySet?x=y", uri.toASCIIString());
  }
  
  @Test
  void testCustomQueryOptionWithFilterAndFunction() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).
        appendOperationCallSegment("functionName").count().filter("PropertyString eq '1'").
        addCustomQueryOption("x", "y").build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/functionName()"
        + "/%24count?%24filter=PropertyString%20eq%20'1'&x=y", newUri.toASCIIString());
  }
  
  @Test
  void testCustomQueryOptionWithFunctionWithNavAndRef() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("EntitySet").
        appendOperationCallSegment("functionName").
        appendNavigationSegment("NavSeg").appendRefSegment().addCustomQueryOption("x", "y").build();
    final Map<String, ClientValue> parameters = new HashMap<>();
    final ClientPrimitiveValue value = client.getObjectFactory().
        newPrimitiveValueBuilder().buildString("parameterValue");
    parameters.put("parameterName", value);
    URI newUri = URIUtils.buildFunctionInvokeURI(uri, parameters);
    assertNotNull(newUri);
    assertEquals("http://host/service/EntitySet/functionName(parameterName%3D'parameterValue')"
        + "/NavSeg/%24ref?x=y", newUri.toASCIIString());
  }
  
  @Test
  void testCustomQueryOptionOnRoot() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).
        addCustomQueryOption("x", "y").build();
    assertEquals("http://host/service?x=y", uri.toASCIIString());
  }
  
  @Test
  void testTwoCustomQueryOption() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).
        addCustomQueryOption("x", "y").
        addCustomQueryOption("y", "z").build();
    assertEquals("http://host/service?x=y&y=z", uri.toASCIIString());
  }
  
  @Test
  void testCustomQueryOptionWithEncodedNameValue() throws ODataDeserializerException {
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).
        addCustomQueryOption("x!/?", "@?$").build();
    assertEquals("http://host/service?x%21%2F%3F=%40%3F%24", uri.toASCIIString());
  }

  @Test
  void schemaVersion() throws ODataDeserializerException {
    // Tier 5 Wave 2 Task 4: $schemaversion query option on the client URI builder
    final ODataClient client = ODataClientFactory.getClient();
    final URI uri = client.newURIBuilder(SERVICE_ROOT).appendMetadataSegment().schemaVersion("*").build();
    assertEquals("http://host/service/$metadata?%24schemaversion=%2A", uri.toASCIIString());
  }

  private static ODataClient keyAsSegmentClient() {
    final ODataClient client = ODataClientFactory.getClient();
    client.getConfiguration().setKeyAsSegment(true);
    return client;
  }

  @Test
  void keyAsSegmentSingle() {
    // Tier 5 Wave 3: a string key is emitted raw - no surrounding quotes, no doubled quote
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("People").
        appendKeySegment("O'Neil").build();

    assertEquals(SERVICE_ROOT + "/People/O'Neil", uri.toASCIIString());
    assertEquals("/service/People/O'Neil", uri.getPath());
  }

  @Test
  void keyAsSegmentSlashEncoded() {
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Categories").
        appendKeySegment("Smartphone/Tablet").build();

    assertEquals(SERVICE_ROOT + "/Categories/Smartphone%2FTablet", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentInt() {
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Employees").
        appendKeySegment(1).build();

    assertEquals(SERVICE_ROOT + "/Employees/1", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentGuid() {
    final UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Orders").
        appendKeySegment(uuid).build();

    assertEquals(SERVICE_ROOT + "/Orders/01234567-89ab-cdef-0123-456789abcdef", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentDateTimeOffset() {
    final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.clear();
    cal.set(2012, Calendar.FEBRUARY, 29, 1, 2, 3);
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Events").
        appendKeySegment(cal).build();

    assertEquals(SERVICE_ROOT + "/Events/2012-02-29T01%3A02%3A03Z", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentMultiPart() {
    final Map<String, Object> key = new LinkedHashMap<>();
    key.put("OrderID", 1);
    key.put("ItemNo", 2);
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("OrderItems").
        appendKeySegment(key).build();

    assertEquals(SERVICE_ROOT + "/OrderItems/1/2", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentCountAfterKey() {
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Employees").
        appendKeySegment(1).appendNavigationSegment("Orders").appendCountSegment().build();

    assertEquals(SERVICE_ROOT + "/Employees/1/Orders/$count", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentOffUnchanged() {
    final ODataClient client = ODataClientFactory.getClient();
    assertEquals(SERVICE_ROOT + "/Employees('A1')", client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Employees").appendKeySegment("A1").build().toASCIIString());

    final Map<String, Object> key = new LinkedHashMap<>();
    key.put("OrderID", 1);
    key.put("ItemNo", 2);
    assertEquals(SERVICE_ROOT + "/OrderItems(OrderID=1,ItemNo=2)", client.newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("OrderItems").appendKeySegment(key).build().toASCIIString());
  }

  @Test
  void keyAsSegmentReservedCharactersAreEncoded() {
    assertEquals(SERVICE_ROOT + "/Categories/a%3Fb%23c", keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment("a?b#c").build().toASCIIString());
    assertEquals(SERVICE_ROOT + "/Categories/50%25", keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment("50%").build().toASCIIString());
    assertEquals(SERVICE_ROOT + "/Categories/a%5Bb%5D%20c", keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Categories").appendKeySegment("a[b] c").build().toASCIIString());
  }

  @Test
  void keyAsSegmentNonAsciiIsUtf8PercentEncoded() {
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("Categories").
        appendKeySegment("\u00dcn\u00efcode").build();

    assertEquals(SERVICE_ROOT + "/Categories/%C3%9Cn%C3%AFcode", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentEmptyStringIsRejected() {
    final URIBuilder builder = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("People");

    final IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> builder.appendKeySegment(""));
    assertEquals("Empty key value cannot be expressed as a path segment", e.getMessage());
  }

  @Test
  void keyAsSegmentNullValueRendersNullLiteral() {
    // Same literal the parenthesized form uses for a null key value.
    final URI uri = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).appendEntitySetSegment("People").
        appendKeySegment((Object) null).build();

    assertEquals(SERVICE_ROOT + "/People/null", uri.toASCIIString());
  }

  @Test
  void keyAsSegmentNullOrEmptyMapAddsNoSegment() {
    assertEquals(SERVICE_ROOT + "/People", keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("People").appendKeySegment((Map<String, Object>) null).
        build().toASCIIString());
    assertEquals(SERVICE_ROOT + "/People", keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("People").appendKeySegment(Collections.<String, Object> emptyMap()).
        build().toASCIIString());
  }

  @Test
  void keyAsSegmentEnumKeyOverloadIsRejected() {
    final URIBuilder builder = keyAsSegmentClient().newURIBuilder(SERVICE_ROOT).
        appendEntitySetSegment("Orders");
    final Map<String, Map.Entry<EdmEnumType, String>> enumValues = Collections.emptyMap();
    final Map<String, Object> values = Collections.emptyMap();

    final IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> builder.appendKeySegment(enumValues, values));
    assertThat(e.getMessage(), containsString("appendKeySegment(Map<String, Object>)"));
  }
}
