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
import org.sitenetsoft.olinguito.commons.api.edm.geo.Point;
import org.sitenetsoft.olinguito.ext.proxy.api.StructuredComposableInvoker;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.NavigationProperty;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Key;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Property;

// CHECKSTYLE:ON (Maven checkstyle)

public interface SupplierComposableInvoker
    extends StructuredComposableInvoker<Supplier, Supplier.Operations>
{

  @Override
  SupplierComposableInvoker select(String... select);

  @Override
  SupplierComposableInvoker expand(String... expand);

  @Key
  @Property(name = "ID",
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
  java.lang.Integer getID();

  void setID(java.lang.Integer _iD);

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
      type = "ODataDemo.Address",
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

  void setAddress(Address _address);

  @Property(name = "Location",
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
  Point getLocation();

  void setLocation(Point _location);

  @Property(name = "Concurrency",
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
  java.lang.Integer getConcurrency();

  void setConcurrency(java.lang.Integer _concurrency);

  @NavigationProperty(name = "Products",
      type = "ODataDemo.Product",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "Products",
      containsTarget = false)
  ProductCollection getProducts();

  void setProducts(ProductCollection _products);

}
