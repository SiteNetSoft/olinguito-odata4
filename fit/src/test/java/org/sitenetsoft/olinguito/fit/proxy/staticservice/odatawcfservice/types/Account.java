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
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntitySet;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.EntityType;
import org.sitenetsoft.olinguito.ext.proxy.api.annotations.Parameter;

@Namespace("Microsoft.Test.OData.Services.ODataWCFService")
@EntityType(name = "Account",
    openType = false,
    hasStream = false,
    isAbstract = false)
public interface Account
    extends Annotatable,
        org.sitenetsoft.olinguito.ext.proxy.api.EntityType<Account>, StructuredQuery<Account> {

  @Key
  @Property(name = "AccountID",
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
  java.lang.Integer getAccountID();

  void setAccountID(java.lang.Integer _accountID);

  @Property(name = "Country",
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
  java.lang.String getCountry();

  void setCountry(java.lang.String _country);

  @Property(name = "AccountInfo",
      type = "Microsoft.Test.OData.Services.ODataWCFService.AccountInfo",
      nullable = true,
      defaultValue = "",
      maxLenght = Integer.MAX_VALUE,
      fixedLenght = false,
      precision = 0,
      scale = 0,
      unicode = true,
      collation = "",
      srid = "")
  AccountInfo
      getAccountInfo();

      void
      setAccountInfo(
          AccountInfo _accountInfo);

  @NavigationProperty(name = "MyGiftCard",
      type = "Microsoft.Test.OData.Services.ODataWCFService.GiftCard",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "",
      targetEntitySet = "",
      containsTarget = true)
  GiftCard
      getMyGiftCard();

      void
      setMyGiftCard(
          GiftCard _myGiftCard);

  @NavigationProperty(name = "AvailableSubscriptionTemplatess",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Subscription",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "SubscriptionTemplates",
      containsTarget = false)
  SubscriptionCollection
      getAvailableSubscriptionTemplatess();

      void
      setAvailableSubscriptionTemplatess(
          SubscriptionCollection _availableSubscriptionTemplatess);

  Operations operations();

  interface Operations extends org.sitenetsoft.olinguito.ext.proxy.api.Operations {

    @Operation(
        name = "GetDefaultPI",
        type = OperationType.FUNCTION,
        isComposable = false,
        referenceType = PaymentInstrument.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.PaymentInstrument")
    StructuredInvoker<PaymentInstrument>
        getDefaultPI(
        );

    @Operation(
        name = "GetAccountInfo",
        type = OperationType.FUNCTION,
        isComposable = true,
        referenceType = AccountInfo.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.AccountInfo")
    AccountInfoComposableInvoker
        getAccountInfo(
        );

    @Operation(
        name = "RefreshDefaultPI",
        type = OperationType.ACTION,
        referenceType = PaymentInstrument.class,
        returnType = "Microsoft.Test.OData.Services.ODataWCFService.PaymentInstrument")
    StructuredInvoker<PaymentInstrument>
        refreshDefaultPI(
            @Parameter(name = "newDate", type = "Edm.DateTimeOffset", nullable = true) java.sql.Timestamp newDate
        );

  }

  Annotations annotations();

  interface Annotations {

    @AnnotationsForProperty(name = "AccountID",
        type = "Edm.Int32")
    Annotatable getAccountIDAnnotations();

    @AnnotationsForProperty(name = "Country",
        type = "Edm.String")
    Annotatable getCountryAnnotations();

    @AnnotationsForProperty(name = "AccountInfo",
        type = "Microsoft.Test.OData.Services.ODataWCFService.AccountInfo")
    Annotatable getAccountInfoAnnotations();

    @AnnotationsForNavigationProperty(name = "MyGiftCard",
        type = "Microsoft.Test.OData.Services.ODataWCFService.GiftCard")
    Annotatable getMyGiftCardAnnotations();

    @AnnotationsForNavigationProperty(name = "MyPaymentInstruments",
        type = "Microsoft.Test.OData.Services.ODataWCFService.PaymentInstrument")
    Annotatable getMyPaymentInstrumentsAnnotations();

    @AnnotationsForNavigationProperty(name = "ActiveSubscriptions",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Subscription")
    Annotatable getActiveSubscriptionsAnnotations();

    @AnnotationsForNavigationProperty(
        name = "AvailableSubscriptionTemplatess",
        type = "Microsoft.Test.OData.Services.ODataWCFService.Subscription")
    Annotatable getAvailableSubscriptionTemplatessAnnotations();
  }

  @NavigationProperty(name = "MyPaymentInstruments",
      type = "Microsoft.Test.OData.Services.ODataWCFService.Subscription",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "SubscriptionTemplates",
      containsTarget = true)
      Account.MyPaymentInstruments
      getMyPaymentInstruments();

      void
      setMyPaymentInstruments(
          Account.MyPaymentInstruments _myPaymentInstruments);

  @EntitySet(name = "MyPaymentInstruments", contained = true)
  interface MyPaymentInstruments
      extends
          org.sitenetsoft.olinguito.ext.proxy.api.EntitySet<PaymentInstrument, PaymentInstrumentCollection>,
          StructuredCollectionQuery<MyPaymentInstruments>,
  AbstractEntitySet<PaymentInstrument, java.lang.Integer, PaymentInstrumentCollection> {
    // No additional methods needed for now.
  }

  @NavigationProperty(name = "ActiveSubscriptions",
      type = "java.lang.Integer",
      targetSchema = "Microsoft.Test.OData.Services.ODataWCFService",
      targetContainer = "InMemoryEntities",
      targetEntitySet = "SubscriptionTemplates",
      containsTarget = true)
      Account.ActiveSubscriptions
      getActiveSubscriptions();

      void
      setActiveSubscriptions(
          Account.ActiveSubscriptions _activeSubscriptions);

  @EntitySet(name = "ActiveSubscriptions", contained = true)
  interface ActiveSubscriptions
      extends
          org.sitenetsoft.olinguito.ext.proxy.api.EntitySet<Subscription, SubscriptionCollection>,
          StructuredCollectionQuery<ActiveSubscriptions>,
  AbstractEntitySet<Subscription, java.lang.Integer, SubscriptionCollection> {
    // No additional methods needed for now.
  }

}
