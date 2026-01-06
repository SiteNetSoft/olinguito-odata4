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

import org.sitenetsoft.olinguito.ext.proxy.api.Annotatable;
import org.sitenetsoft.olinguito.ext.proxy.api.StructuredCollectionQuery;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.*;
import org.sitenetsoft.olinguito.ext.proxy.api.AbstractEntitySet;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "CreditCardPI",
    openType = false,
    hasStream = false,
    isAbstract = false,
    baseType = "Microsoft.Test.OData.Services.ODataWCFService.PaymentInstrument")
public interface CreditCardPI extends PaymentInstrument {

  @Override
  CreditCardPI load();

  @Override
  Future<? extends CreditCardPI> loadAsync();

  @Override
  CreditCardPI refs();

  @Override
  CreditCardPI expand(String... expand);

  @Override
  CreditCardPI select(String... select);

  @Override
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

  @Override
  void setPaymentInstrumentID(java.lang.Integer _paymentInstrumentID);

  @Override
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

  @Override
  void setFriendlyName(java.lang.String _friendlyName);

  @Override
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

  @Override
  void setCreatedDate(java.sql.Timestamp _createdDate);

  @Property(name = "CardNumber",
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
  java.lang.String getCardNumber();

  void setCardNumber(java.lang.String _cardNumber);

  @Property(name = "CVV",
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
  java.lang.String getCVV();

  void setCVV(java.lang.String _cVV);

  @Property(name = "HolderName",
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
  java.lang.String getHolderName();

  void setHolderName(java.lang.String _holderName);

  @Property(name = "Balance",
      type = "Edm.Double",
      nullable = false,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  java.lang.Double getBalance();

  void setBalance(java.lang.Double _balance);

  @Property(name = "ExperationDate",
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
  java.sql.Timestamp getExperationDate();

  void setExperationDate(java.sql.Timestamp _experationDate);

  @Override
  @NavigationProperty(name = "TheStoredPI",
      type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "StoredPIs",
      containsTarget = false)
  StoredPI
      getTheStoredPI();

  @Override
      void
      setTheStoredPI(
          StoredPI _theStoredPI);

  @Override
  @NavigationProperty(name = "BackupStoredPI",
      type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "StoredPIs",
      containsTarget = false)
  StoredPI
      getBackupStoredPI();

  @Override
      void
      setBackupStoredPI(
          StoredPI _backupStoredPI);

  @Override
  Operations operations();

  interface Operations
      extends
          PaymentInstrument.Operations {
    // No additional methods needed for now.
  }

  @Override
  Annotations annotations();

  interface Annotations
      extends
          PaymentInstrument.Annotations {

    @Override
    @AnnotationsForProperty(name = "PaymentInstrumentID",
        type = "Edm.Int32")
    Annotatable getPaymentInstrumentIDAnnotations();

    @Override
    @AnnotationsForProperty(name = "FriendlyName",
        type = "Edm.String")
    Annotatable getFriendlyNameAnnotations();

    @Override
    @AnnotationsForProperty(name = "CreatedDate",
        type = "Edm.DateTimeOffset")
    Annotatable getCreatedDateAnnotations();

    @AnnotationsForProperty(name = "CardNumber",
        type = "Edm.String")
    Annotatable getCardNumberAnnotations();

    @AnnotationsForProperty(name = "CVV",
        type = "Edm.String")
    Annotatable getCVVAnnotations();

    @AnnotationsForProperty(name = "HolderName",
        type = "Edm.String")
    Annotatable getHolderNameAnnotations();

    @AnnotationsForProperty(name = "Balance",
        type = "Edm.Double")
    Annotatable getBalanceAnnotations();

    @AnnotationsForProperty(name = "ExperationDate",
        type = "Edm.DateTimeOffset")
    Annotatable getExperationDateAnnotations();

    @Override
    @AnnotationsForNavigationProperty(name = "TheStoredPI",
        type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI")
    Annotatable getTheStoredPIAnnotations();

    @Override
    @AnnotationsForNavigationProperty(name = "BillingStatements",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Statement")
    Annotatable getBillingStatementsAnnotations();

    @Override
    @AnnotationsForNavigationProperty(name = "BackupStoredPI",
        type = "Microsoft.Test.OData.Services.ODataWCFService.StoredPI")
    Annotatable getBackupStoredPIAnnotations();

    @AnnotationsForNavigationProperty(name = "CreditRecords",
        type = "Microsoft.Test.OData.Services.ODataWCFService.CreditRecord")
    Annotatable getCreditRecordsAnnotations();
  }

  @NavigationProperty(name = "CreditRecords",
      type = "Microsoft.Test.OData.Services.ODataWCFService.CreditRecord",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "",
      targetEntitySet = "",
      containsTarget = true)
      CreditCardPI.CreditRecords
      getCreditRecords();

      void
      setCreditRecords(
          CreditCardPI.CreditRecords _creditRecords);

  @EntitySet(name = "CreditRecords", contained = true)
  interface CreditRecords
      extends
          org.sitenetsoft.olinguito.ext.proxy.api.EntitySet<CreditRecord, CreditRecordCollection>,
          StructuredCollectionQuery<CreditRecords>,
  AbstractEntitySet<CreditRecord, java.lang.Integer, CreditRecordCollection> {
    // No additional methods needed for now.
  }

}
