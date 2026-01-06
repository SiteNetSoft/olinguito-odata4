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
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntitySet;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType;
// CHECKSTYLE:ON (Maven checkstyle)


@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "PaymentInstrument",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface PaymentInstrument
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<PaymentInstrument>,
        StructuredQuery<PaymentInstrument> {

  @Key
  @Property(name = "PaymentInstrumentID",
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
  java.lang.Integer getPaymentInstrumentID();

  void setPaymentInstrumentID(java.lang.Integer _paymentInstrumentID);

  @Property(name = "FriendlyName",
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
  java.lang.String getFriendlyName();

  void setFriendlyName(java.lang.String _friendlyName);

  @Property(name = "CreatedDate",
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

  @NavigationProperty(name = "TheStoredPI",
      type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "StoredPIs",
      containsTarget = false)
  StoredPI
      getTheStoredPI();

      void
      setTheStoredPI(
          StoredPI _theStoredPI);

  @NavigationProperty(name = "BackupStoredPI",
      type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "StoredPIs",
      containsTarget = false)
  StoredPI
      getBackupStoredPI();

      void
      setBackupStoredPI(
          StoredPI _backupStoredPI);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {
    // No additional methods needed for now.
  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "PaymentInstrumentID",
        type = "Edm.Int32")
    Annotatable getPaymentInstrumentIDAnnotations();

    @AnnotationsForProperty(name = "FriendlyName",
        type = "Edm.String")
    Annotatable getFriendlyNameAnnotations();

    @AnnotationsForProperty(name = "CreatedDate",
        type = "Edm.DateTimeOffset")
    Annotatable getCreatedDateAnnotations();

    @AnnotationsForNavigationProperty(name = "TheStoredPI",
        type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI")
    Annotatable getTheStoredPIAnnotations();

    @AnnotationsForNavigationProperty(name = "BillingStatements",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Statement")
    Annotatable getBillingStatementsAnnotations();

    @AnnotationsForNavigationProperty(name = "BackupStoredPI",
        type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI")
    Annotatable getBackupStoredPIAnnotations();
  }

  @NavigationProperty(name = "BillingStatements",
      type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "StoredPIs",
      containsTarget = true)
      PaymentInstrument.BillingStatements
      getBillingStatements();

      void
      setBillingStatements(
          PaymentInstrument.BillingStatements _billingStatements);

  @EntitySet(name = "BillingStatements", contained = true)
  interface BillingStatements
      extends
          org.sitenetsoft.olinguito.ext.proxy.api.EntitySet<Statement, StatementCollection>,
          StructuredCollectionQuery<BillingStatements>,
  AbstractEntitySet<Statement, java.lang.Integer, StatementCollection> {
    // No additional methods needed for now.
  }

}
