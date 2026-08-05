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

import java.util.concurrent.Future;

import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;

@Namespace("ODataDemo")
@EntityType(name = "Customer",
    openType = false,
    hasStream = false,
    isAbstract = false,
    baseType = "ODataDemo.Person")
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
  @Override
  java.lang.Integer getID();

  @Override
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
  @Override
  java.lang.String getName();

  @Override
  void setName(java.lang.String _name);

  @Property(name = "TotalExpense",
      type = "Edm.Decimal",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.math.BigDecimal getTotalExpense();

  void setTotalExpense(java.math.BigDecimal _totalExpense);

  @Override
  @NavigationProperty(name = "PersonDetail",
      type = "ODataDemo.PersonDetail",
      targetSchema = "ODataDemo",
      targetContainer = "DemoService",
      targetEntitySet = "PersonDetails",
      containsTarget = false)
  PersonDetail getPersonDetail();

  @Override
  void setPersonDetail(PersonDetail _personDetail);

  @Override
  Operations operations();

  interface Operations extends Person.Operations {
    // No additional methods needed for now.
  }

  @Override
  Annotations annotations();

  interface Annotations extends Person.Annotations {

    @Override
    @AnnotationsForProperty(name = "ID",
        type = "Edm.Int32")
    Annotatable getIDAnnotations();

    @Override
    @AnnotationsForProperty(name = "Name",
        type = "Edm.String")
    Annotatable getNameAnnotations();

    @AnnotationsForProperty(name = "TotalExpense",
        type = "Edm.Decimal")
    Annotatable getTotalExpenseAnnotations();

    @Override
    @AnnotationsForNavigationProperty(name = "PersonDetail",
        type = "ODataDemo.PersonDetail")
    Annotatable getPersonDetailAnnotations();
  }

}
