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
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 * Copyright 2026 SiteNetSoft - OLINGO-1184: Test bound function after type filter in $filter
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 1: $schemaversion system query option
 */
package org.sitenetsoft.olinguito.server.core.uri.parser;

import java.util.Collections;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoKind;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.sitenetsoft.olinguito.server.core.uri.parser.UriParserSemanticException.MessageKeys;
import org.sitenetsoft.olinguito.server.core.uri.testutil.FilterValidator;
import org.sitenetsoft.olinguito.server.core.uri.testutil.ResourceValidator;
import org.sitenetsoft.olinguito.server.core.uri.testutil.TestUriValidator;
import org.sitenetsoft.olinguito.server.core.uri.validator.UriValidationException;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EntityTypeProvider;
import org.sitenetsoft.olinguito.server.tecsvc.provider.PropertyProvider;
import org.junit.jupiter.api.Test;

/** Tests of the URI parser as a whole - please put more specific tests elsewhere. */
class UriParserTest {

  private static final Edm edm = OData.newInstance().createServiceMetadata(
      new EdmTechProvider(), Collections.emptyList()).getEdm();

  private final TestUriValidator testUri = new TestUriValidator().setEdm(edm);
  private final ResourceValidator testRes = new ResourceValidator().setEdm(edm);
  private final FilterValidator testFilter = new FilterValidator().setEdm(edm);


  @Test
  void systemQueryOptionsWithoutDollarAndInAnyCase() throws Exception {
    // [OData-Protocol] 13.2.1 items 6 and 7: names are case-insensitive and the "$" is optional.
    testUri.run("ESAllPrim", "filter=PropertyInt16 eq 1")
        .isKind(UriInfoKind.resource);
    testUri.run("ESAllPrim", "$FILTER=PropertyInt16 eq 1")
        .isKind(UriInfoKind.resource);
    testUri.run("ESAllPrim", "Top=1")
        .isKind(UriInfoKind.resource);

    // A name that is not a system query option keeps its existing treatment.
    testUri.runEx("ESAllPrim", "$nonsense=1")
        .isExSyntax(UriParserSyntaxException.MessageKeys.UNKNOWN_SYSTEM_QUERY_OPTION);
    testUri.run("ESAllPrim", "nonsense=1")
        .isKind(UriInfoKind.resource);

    // [OData-URL] 5.1: the same option, irrespective of casing or the $ prefix,
    // MUST NOT be specified more than once for any resource.
    testUri.runEx("ESAllPrim", "$filter=PropertyInt16 eq 1&FILTER=PropertyInt16 eq 2")
        .isExSyntax(UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESAllPrim", "top=1&$top=2")
        .isExSyntax(UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION);
  }

  @Test
  void quotedKeyValuesForNonStringKeys() throws Exception {
    // [OData-Protocol] 13.2.1 item 9a: strings are cast to primitive types in URLs.
    // The key predicate's text is normalised to a literal of the key's own type, because that is
    // what consumers hand to fromUriLiteral -- tecsvc's DataProvider could not otherwise find the
    // entity addressed by the quoted form.
    testRes.run("ESAllPrim('1')")
        .isEntitySet("ESAllPrim")
        .isKeyPredicate(0, "PropertyInt16", "1");
    // The unquoted 4.0 spelling is unchanged.
    testRes.run("ESAllPrim(1)")
        .isEntitySet("ESAllPrim")
        .isKeyPredicate(0, "PropertyInt16", "1");
    // A string that is not a literal of the key type is still rejected.
    testUri.runEx("ESAllPrim('abc')")
        .isExValidation(UriValidationException.MessageKeys.INVALID_KEY_PROPERTY);
  }

  @Test
  void unprefixedEnumAndDurationKeyValues() throws Exception {
    // [OData-Protocol] 13.2.1 item 9b, alongside the prefixed spelling it already supported.
    testRes.run("ESMixEnumDefCollComp(PropertyEnumString='String1',PropertyDefString='abc')")
        .isEntitySet("ESMixEnumDefCollComp");
    testRes.run("ESMixEnumDefCollComp(PropertyEnumString=olingo.odata.test1.ENString'String1',"
        + "PropertyDefString='abc')")
        .isEntitySet("ESMixEnumDefCollComp");
  }

  @Test
  void misc() throws Exception {
    testUri.run("")
        .isKind(UriInfoKind.service);
    testUri.run("/")
        .isKind(UriInfoKind.service);

    testUri.run("$all")
        .isKind(UriInfoKind.all);

    testUri.run("$batch")
        .isKind(UriInfoKind.batch);

    testUri.runEx("//").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$metadata/").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("//$metadata").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("ESKeyNav//$count").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);

    testUri.runEx("$metadata/$ref").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$batch/$ref").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$all/$ref").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$entity/olingo.odata.test1.ETKeyNav/$ref")
        .isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$entity/olingo.odata.test1.ETKeyNav")
        .isExSyntax(UriParserSyntaxException.MessageKeys.ENTITYID_MISSING_SYSTEM_QUERY_OPTION_ID);
    
    testUri.runEx("$wrong").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
    testUri.runEx("", "$wrong").isExSyntax(UriParserSyntaxException.MessageKeys.UNKNOWN_SYSTEM_QUERY_OPTION);

    testUri.runEx("ESKeyNav()").isExSemantic(MessageKeys.WRONG_NUMBER_OF_KEY_PROPERTIES);

    testRes.run("ESTwoKeyNav/olingo.odata.test1.BFC_RTESTwoKeyNav_()")
        .isEntitySet("ESTwoKeyNav")
        .n().isFunction("BFC_RTESTwoKeyNav_");

    testRes.run("ESAllPrim/olingo.odata.test1.BAESAllPrimRTETAllPrim")
        .isEntitySet("ESAllPrim")
        .n().isAction("BAESAllPrimRTETAllPrim");

    testRes.run("ESTwoKeyNav/olingo.odata.test1.BFC_RTESTwoKeyNav_()")
        .isEntitySet("ESTwoKeyNav")
        .n().isFunction("BFC_RTESTwoKeyNav_");

    testRes.run("ESMedia(1)/$value")
        .isEntitySet("ESMedia")
        .n().isValue();

    testRes.run("ESAllPrim/olingo.odata.test1.BAESAllPrimRTETAllPrim")
        .isEntitySet("ESAllPrim")
        .n().isAction("BAESAllPrimRTETAllPrim");

    testRes.run("ESTwoKeyNav/olingo.odata.test1.BFC_RTESTwoKeyNav_()")
        .isEntitySet("ESTwoKeyNav")
        .n().isFunction("BFC_RTESTwoKeyNav_");

    testRes.run("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/olingo.odata.test1.ETBaseTwoKeyNav")
        .isEntitySet("ESTwoKeyNav")
        .isKeyPredicate(0, "PropertyInt16", "1")
        .isKeyPredicate(1, "PropertyString", "'2'")
        .isType(EntityTypeProvider.nameETTwoKeyNav)
        .isTypeFilterOnEntry(EntityTypeProvider.nameETBaseTwoKeyNav);

    testRes.run("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/olingo.odata.test1.ETBaseTwoKeyNav/$ref")
        .isEntitySet("ESTwoKeyNav")
        .isKeyPredicate(0, "PropertyInt16", "1")
        .isKeyPredicate(1, "PropertyString", "'2'")
        .isType(EntityTypeProvider.nameETTwoKeyNav)
        .isTypeFilterOnEntry(EntityTypeProvider.nameETBaseTwoKeyNav)
        .n().isRef();
    
    testRes.run("ESKeyNav(1)/NavPropertyETTwoKeyNavMany(PropertyInt16=1,PropertyString='1')")
    .isEntityType("ETTwoKeyNav")
    .isKeyPredicateForNavSeg(0, "PropertyInt16", "1")
    .isKeyPredicateForNavSeg(1, "PropertyString", "'1'");
    
    testRes.run("ESKeyNav(1)/NavPropertyETMediaMany(1)")
    .isEntityType("ETMedia")
    .isKeyPredicateForNavSeg(0, "PropertyInt16", "1");
  }

  @Test
  void enumAndTypeDefAsKey() throws Exception {
    testRes
        .run("ESMixEnumDefCollComp(PropertyEnumString=olingo.odata.test1.ENString'String1',PropertyDefString='abc')")
        .isEntitySet("ESMixEnumDefCollComp")
        .isKeyPredicate(0, "PropertyEnumString", "olingo.odata.test1.ENString'String1'")
        .isKeyPredicate(1, "PropertyDefString", "'abc'");

    testFilter.runOnETMixEnumDefCollComp("PropertyEnumString has Namespace1_Alias.ENString'String1'")
        .is("<<PropertyEnumString> has <olingo.odata.test1.ENString<String1>>>");

    testRes
        .run("ESMixEnumDefCollComp(PropertyEnumString=Namespace1_Alias.ENString'String1',PropertyDefString='abc')")
        .isEntitySet("ESMixEnumDefCollComp")
        .isKeyPredicate(0, "PropertyEnumString", "Namespace1_Alias.ENString'String1'")
        .isKeyPredicate(1, "PropertyDefString", "'abc'");
  }

  @Test
  void crossjoin() throws Exception {
    testUri.run("$crossjoin(ESKeyNav)")
        .isKind(UriInfoKind.crossjoin)
        .isCrossJoinEntityList(List.of("ESKeyNav"));

    testUri.run("$crossjoin(ESKeyNav,ESTwoKeyNav)")
        .isKind(UriInfoKind.crossjoin)
        .isCrossJoinEntityList(List.of("ESKeyNav", "ESTwoKeyNav"));
  }

  @Test
  void crossjoinFilter() throws Exception {
    testUri.run("$crossjoin(ESTwoPrim,ESMixPrimCollComp)",
        "$filter=ESTwoPrim/PropertyString eq ESMixPrimCollComp/PropertyComp/PropertyString")
        .goFilter()
        .isBinary(BinaryOperatorKind.EQ)
        .is("<<ESTwoPrim/PropertyString> eq <ESMixPrimCollComp/PropertyComp/PropertyString>>");
  }

  @Test
  void crossjoinExpand() throws Exception {
    testUri.run("$crossjoin(ESTwoPrim,ESAllPrim)",
        "$expand=ESTwoPrim")
        .goExpand()
        .first().goPath().first().isEntitySet("ESTwoPrim");

    testUri.run("$crossjoin(ESTwoPrim,ESAllPrim)",
        "$expand=ESTwoPrim,ESAllPrim")
        .goExpand()
        .first().goPath().first().isEntitySet("ESTwoPrim")
        .goUpExpandValidator().next().goPath().first().isEntitySet("ESAllPrim");

    // TODO: Once crossjoin is implemented these tests should no longer result in errors
//    testUri.run("$crossjoin(ESTwoPrim,ESAllPrim)",
//        "$expand=ESAllPrim/NavPropertyETTwoPrimOne")
//        .goExpand()
//        .first().goPath().at(0).isEntitySet("ESAllPrim")
//        .at(1).isNavProperty("NavPropertyETTwoPrimOne", EntityTypeProvider.nameETTwoPrim, false);
  }

  @Test
  void crossjoinError() throws Exception {
    testUri.runEx("$crossjoin").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
    testUri.runEx("$crossjoin/error").isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$crossjoin()").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
    testUri.runEx("$crossjoin(ESKeyNav, ESTwoKeyNav)/invalid")
        .isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
    testUri.runEx("$crossjoin(ESKeyNav)/$ref")
        .isExSyntax(UriParserSyntaxException.MessageKeys.MUST_BE_LAST_SEGMENT);
  }

  @Test
  void entityId() throws Exception {
    // simple entity set
    testUri.run("$entity", "$id=ESAllPrim(1)").isKind(UriInfoKind.entityId)
        .isKind(UriInfoKind.entityId)
        .isIdText("ESAllPrim(1)");

    // simple entity set; $format before $id
    testUri.run("$entity", "$format=xml&$id=ESAllPrim(1)").isKind(UriInfoKind.entityId)
        .isFormatText("xml")
        .isIdText("ESAllPrim(1)");

    testUri.run("$entity", "$format=xml&abc=123&$id=ESAllPrim(1)").isKind(UriInfoKind.entityId)
        .isFormatText("xml")
        .isCustomParameter(0, "abc", "123")
        .isIdText("ESAllPrim(1)");

    // simple entity set; $format after $id
    testUri.run("$entity", "$id=ESAllPrim(1)&$format=xml").isKind(UriInfoKind.entityId)
        .isIdText("ESAllPrim(1)")
        .isFormatText("xml");

    // simple entity set; $format and custom parameter after $id
    testUri.run("$entity", "$id=ESAllPrim(1)&$format=xml&abc=123").isKind(UriInfoKind.entityId)
        .isIdText("ESAllPrim(1)")
        .isFormatText("xml")
        .isCustomParameter(0, "abc", "123");

    // simple entity set; $format before $id and custom parameter after $id
    testUri.run("$entity", "$format=xml&$id=ESAllPrim(1)&abc=123").isKind(UriInfoKind.entityId)
        .isFormatText("xml")
        .isIdText("ESAllPrim(1)")
        .isCustomParameter(0, "abc", "123");

    // simple entity set; with qualifiedentityTypeName
    testUri.run("$entity/olingo.odata.test1.ETTwoPrim", "$id=ESBase(111)")
        .isEntityType(EntityTypeProvider.nameETTwoPrim)
        .isIdText("ESBase(111)");

    // simple entity set; with qualifiedentityTypeName;
    testUri.run("$entity/olingo.odata.test1.ETBase", "$id=ESTwoPrim(1)")
        .isEntityType(EntityTypeProvider.nameETBase)
        .isKind(UriInfoKind.entityId)
        .isIdText("ESTwoPrim(1)");

    // simple entity set; with qualifiedentityTypeName; with format
    testUri.run("$entity/olingo.odata.test1.ETBase", "$id=ESTwoPrim(1)&$format=atom")
        .isKind(UriInfoKind.entityId)
        .isEntityType(EntityTypeProvider.nameETBase)
        .isIdText("ESTwoPrim(1)")
        .isFormatText("atom");

    // simple entity set; with qualifiedentityTypeName; with select
    testUri.run("$entity/olingo.odata.test1.ETBase", "$id=ESTwoPrim(1)&$select=*")
        .isKind(UriInfoKind.entityId)
        .isEntityType(EntityTypeProvider.nameETBase)
        .isIdText("ESTwoPrim(1)")
        .isSelectItemStar(0);

    // simple entity set; with qualifiedentityTypeName; with expand
    testUri.run("$entity/olingo.odata.test1.ETBase", "$id=ESTwoPrim(1)&$expand=*")
        .isKind(UriInfoKind.entityId)
        .isEntityType(EntityTypeProvider.nameETBase)
        .isIdText("ESTwoPrim(1)")
        .goExpand().first().isSegmentStar();
    
    try {
      testUri.run("$entity/olingo.odata.test1.ETAllNullable")
      .isKind(UriInfoKind.entityId)
      .isEntityType(EntityTypeProvider.nameETAllNullable);
    } catch (UriParserSyntaxException e) {
      testUri.isExSyntax(UriParserSyntaxException.MessageKeys.ENTITYID_MISSING_SYSTEM_QUERY_OPTION_ID);
    }
    testUri.run("$entity/Namespace1_Alias.ETAllPrim", "$id=ESAllPrim(32767)")
    .isKind(UriInfoKind.entityId)
    .isEntityType(EntityTypeProvider.nameETAllPrim)
    .isIdText("ESAllPrim(32767)");
    try {
      testUri.run("ESAllPrim/$entity")
    .isKind(UriInfoKind.resource);
    } catch (UriParserSyntaxException e) {
      testUri.isExSyntax(UriParserSyntaxException.MessageKeys.ENTITYID_MISSING_SYSTEM_QUERY_OPTION_ID);
    }
    try {
      testUri.run("ESAllPrim(32767)/NavPropertyETTwoPrimOne/$entity")
    .isKind(UriInfoKind.resource);
    } catch(UriParserSyntaxException e) {
      testUri.isExSyntax(UriParserSyntaxException.MessageKeys.ENTITYID_MISSING_SYSTEM_QUERY_OPTION_ID);
    }
    testUri.run("$entity", "$id=ESAllPrim(32767)/NavPropertyETTwoPrimOne")
      .isKind(UriInfoKind.entityId)
      .isEntityType(EntityTypeProvider.nameETTwoPrim)
    .isIdText("ESAllPrim(32767)/NavPropertyETTwoPrimOne");
    testUri.run("$entity", "$id=ESAllPrim(32767)", "$select=PropertyString", null)
    .isKind(UriInfoKind.entityId)
    .isEntityType(EntityTypeProvider.nameETAllPrim)
      .isIdText("ESAllPrim(32767)");
    testUri.run("$entity", "$id=ESAllPrim(32767)", "$expand=NavPropertyETTwoPrimOne", null)
    .isKind(UriInfoKind.entityId)
    .isEntityType(EntityTypeProvider.nameETAllPrim)
      .isIdText("ESAllPrim(32767)");  
    testUri.run("$entity", "$id=http://localhost:8080/odata-server-tecsvc/odata.svc/"
        + "ESAllPrim(32767)/NavPropertyETTwoPrimOne", null, 
        "http://localhost:8080/odata-server-tecsvc/odata.svc")
    .isKind(UriInfoKind.entityId)
    .isEntityType(EntityTypeProvider.nameETTwoPrim)
    .isIdText("http://localhost:8080/odata-server-tecsvc/odata.svc/"
        + "ESAllPrim(32767)/NavPropertyETTwoPrimOne");
    try {
      testUri.run("$entity/olingo.odata.test1.ETKeyNav", "$id=http://localhost:90/tecsvc/ESKeyNav(1)",
            null, "http://localhost:80/tecsvc")
          .isKind(UriInfoKind.entityId)
          .isEntityType(EntityTypeProvider.nameETKeyNav)
          .isIdText("http://localhost:90/tecsvc/ESKeyNav(1)");
    } catch (UriParserSemanticException e) {
      testUri.isExSemantic(UriParserSemanticException.MessageKeys.NOT_IMPLEMENTED_SYSTEM_QUERY_OPTION);
    }
    try {
      testUri.run("$entity/olingo.odata.test1.ETKeyNav", "$id=http://localhost:90/tecs%27v; c/ESKeyNav(1)",
            null, "http://localhost:80/tecs%27v; c")
          .isKind(UriInfoKind.entityId)
          .isEntityType(EntityTypeProvider.nameETKeyNav)
          .isIdText("http://localhost:90/tecs%27v; c/ESKeyNav(1)");
    } catch (UriParserSemanticException e) {
      testUri.isExSemantic(UriParserSemanticException.MessageKeys.NOT_IMPLEMENTED_SYSTEM_QUERY_OPTION);
    }
    testUri.run("$entity/olingo.odata.test1.ETKeyNav", "$id=http://localhost:90/tecs%27v%20c/ESKeyNav(1)",
        null, "http://localhost:90/tecs%27v%20c")
      .isKind(UriInfoKind.entityId)
      .isEntityType(EntityTypeProvider.nameETKeyNav);
    String idOption = UriDecoder.decode("http://localhost:90/tecs%27v%20c/ESKeyNav(1)");
    testUri.isIdText(idOption);
    testUri.run("$entity/olingo.odata.test1.ETKeyNav", "$id=http://localhost:90/tecs'v c/ESKeyNav(1)",
        null, "http://localhost:90/tecs'v c")
      .isKind(UriInfoKind.entityId)
      .isEntityType(EntityTypeProvider.nameETKeyNav)
      .isIdText("http://localhost:90/tecs'v c/ESKeyNav(1)");
  }

  @Test
  void entityIdFailOnValidation() throws Exception {
    testUri.runEx("$entity/olingo.odata.test1.ETTwoPrim", "$filter=PropertyInt16 eq 123&$id=ESAllPrim(1)")
        .isExValidation(UriValidationException.MessageKeys.SYSTEM_QUERY_OPTION_NOT_ALLOWED);
  }

  @Test
  void resourcePathWithApostrophe() throws Exception {
    testUri.runEx("ESAllPrim'").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
    testUri.runEx("ESAllPrim'InvalidStuff").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
    testFilter.runOnETKeyNavEx("PropertyInt16' eq 0").isExSemantic(MessageKeys.TYPES_NOT_COMPATIBLE);
    testFilter.runOnETKeyNavEx("PropertyInt16 eq' 0").isExSemantic(MessageKeys.TYPES_NOT_COMPATIBLE);
    testFilter.runOnETKeyNavEx("PropertyInt16 eq 0'")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testFilter.runOnETKeyNavEx("PropertyInt16 eq 'dsd''")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
  }

  @Test
  void typeMismatchMessageHasPropertyAndEntityContext() throws Exception {
    // OLINGO-1288: the type-incompatibility message must name the offending property and the data object.
    testFilter.runOnETKeyNavEx("PropertyInt16 eq 'abc'")
        .isExSemantic(MessageKeys.TYPES_NOT_COMPATIBLE)
        .isExMessageText("Incompatible type for property '[PropertyInt16]' in data object "
            + "'olingo.odata.test1.ETKeyNav'. Expected: 'Edm.Int16' Actual: 'Edm.String'.");
  }

  @Test
  void metaData() throws Exception {
    // Parsing the fragment may be used if a uri has to be parsed on the consumer side.
    // On the producer side this feature is currently not supported, so the context fragment
    // part is only available as text.

    testUri.run("$metadata")
        .isKind(UriInfoKind.metadata);

    testUri.run("$metadata", "$format=atom")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom");

    // with context (client usage)
    testUri.run("$metadata", null, "$ref")
        .isKind(UriInfoKind.metadata)
        .isFragmentText("$ref");

    testUri.run("$metadata", "$format=atom", "$ref")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("$ref");

    testUri.run("$metadata", "$format=atom", "Collection($ref)")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("Collection($ref)");

    testUri.run("$metadata", "$format=atom", "Collection(Edm.EntityType)")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("Collection(Edm.EntityType)");

    testUri.run("$metadata", "$format=atom", "Collection(Edm.ComplexType)")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("Collection(Edm.ComplexType)");

    testUri.run("$metadata", "$format=atom", "SINav")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav");

    testUri.run("$metadata", "$format=atom", "SINav/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "SINav/NavPropertyETKeyNavOne")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/NavPropertyETKeyNavOne");

    testUri.run("$metadata", "$format=atom", "SINav/NavPropertyETKeyNavMany(1)")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/NavPropertyETKeyNavMany(1)");

    testUri.run("$metadata", "$format=atom", "SINav/NavPropertyETKeyNavOne/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/NavPropertyETKeyNavOne/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "SINav/NavPropertyETKeyNavMany(1)/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/NavPropertyETKeyNavMany(1)/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "SINav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavOne/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavOne/PropertyInt16");

    testUri.run("$metadata", "$format=atom",
        "SINav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavMany(1)/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("SINav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavMany(1)/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "olingo.odata.test1.ETAllKey")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("olingo.odata.test1.ETAllKey");

    testUri.run("$metadata", "$format=atom", "ESTwoPrim/$deletedEntity")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESTwoPrim/$deletedEntity");

    testUri.run("$metadata", "$format=atom", "ESTwoPrim/$link")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESTwoPrim/$link");

    testUri.run("$metadata", "$format=atom", "ESTwoPrim/$deletedLink")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESTwoPrim/$deletedLink");

    testUri.run("$metadata", "$format=atom", "ESKeyNav")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/NavPropertyETKeyNavOne")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/NavPropertyETKeyNavOne");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/NavPropertyETKeyNavMany(1)")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/NavPropertyETKeyNavMany(1)");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/NavPropertyETKeyNavOne/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/NavPropertyETKeyNavOne/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/NavPropertyETKeyNavMany(1)/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/NavPropertyETKeyNavMany(1)/PropertyInt16");

    testUri.run("$metadata", "$format=atom",
        "ESKeyNav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavOne/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavOne/PropertyInt16");

    testUri.run(
        "$metadata", "$format=atom", "ESKeyNav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavMany(1)/PropertyInt16")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/olingo.odata.test1.ETTwoPrim/NavPropertyETKeyNavMany(1)/PropertyInt16");

    testUri.run("$metadata", "$format=atom", "ESKeyNav(PropertyInt16,PropertyString)")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav(PropertyInt16,PropertyString)");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/$entity")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/$entity");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/$delta")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/$delta");

    testUri.run("$metadata", "$format=atom", "ESKeyNav/(PropertyInt16,PropertyString)/$delta")
        .isKind(UriInfoKind.metadata)
        .isFormatText("atom")
        .isFragmentText("ESKeyNav/(PropertyInt16,PropertyString)/$delta");
  }

  @Test
  void top() throws Exception {
    testUri.run("ESKeyNav", "$top=1")
        .isKind(UriInfoKind.resource)
        .goPath().isEntitySet("ESKeyNav")
        .goUpUriValidator().isTop(1);

    testUri.run("ESKeyNav", "$top=0")
        .isKind(UriInfoKind.resource)
        .goPath().isEntitySet("ESKeyNav")
        .goUpUriValidator().isTop(0);

    testUri.runEx("ESKeyNav", "$top=undefined")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESKeyNav", "$top=")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESKeyNav", "$top=-3")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
  }

  @Test
  void format() throws Exception {
    testUri.run("ESKeyNav(1)", "$format=atom")
        .isKind(UriInfoKind.resource)
        .isFormatText("atom");
    testUri.run("ESKeyNav(1)", "$format=json")
        .isKind(UriInfoKind.resource)
        .isFormatText("json");
    testUri.run("ESKeyNav(1)", "$format=xml")
        .isKind(UriInfoKind.resource)
        .isFormatText("xml");
    testUri.run("ESKeyNav(1)", "$format=IANA_content_type/must_contain_a_slash")
        .isKind(UriInfoKind.resource)
        .isFormatText("IANA_content_type/must_contain_a_slash");
    testUri.run("ESKeyNav(1)", "$format=Test_all_valid_signsSpecified_for_format_signs%26-._~$@%27/Aa123%26-._~$@%27")
        .isKind(UriInfoKind.resource)
        .isFormatText("Test_all_valid_signsSpecified_for_format_signs&-._~$@'/Aa123&-._~$@'");
    testUri.run("ESKeyNav(1)", "$format=" + ContentType.APPLICATION_ATOM_XML_ENTRY_UTF8)
        .isKind(UriInfoKind.resource)
        .isFormatText(ContentType.APPLICATION_ATOM_XML_ENTRY_UTF8.toContentTypeString());
    testUri.runEx("ESKeyNav(1)", "$format=noSlash")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION_FORMAT);
    testUri.runEx("ESKeyNav(1)", "$format=slashAtEnd/")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION_FORMAT);
    testUri.runEx("ESKeyNav(1)", "$format=/startsWithSlash")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION_FORMAT);
    testUri.runEx("ESKeyNav(1)", "$format=two/Slashes/tooMuch")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION_FORMAT);
    testUri.runEx("ESKeyNav(1)", "$format=")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION_FORMAT);
  }

  @Test
  void count() throws Exception {
    testUri.run("ESAllPrim", "$count=true")
        .isKind(UriInfoKind.resource)
        .isInlineCount(true);
    testUri.run("ESAllPrim", "$count=false")
        .isKind(UriInfoKind.resource)
        .isInlineCount(false);
    testUri.runEx("ESAllPrim", "$count=undefined")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESAllPrim", "$count=")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
  }

  @Test
  void skip() throws Exception {
    testUri.run("ESAllPrim", "$skip=3")
        .isKind(UriInfoKind.resource)
        .isSkip(3);
    testUri.run("ESAllPrim", "$skip=0")
        .isKind(UriInfoKind.resource)
        .isSkip(0);

    testUri.runEx("ESAllPrim", "$skip=F")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESAllPrim", "$skip=")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESAllPrim", "$skip=-3")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
  }

  @Test
  void skiptoken() throws Exception {
    testUri.run("ESAllPrim", "$skiptoken=foo")
        .isKind(UriInfoKind.resource)
        .isSkipTokenText("foo");

    testUri.runEx("ESAllPrim", "$skiptoken=")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
  }

  @Test
  void notExistingSystemQueryOption() throws Exception {
    testUri.runEx("ESAllPrim", "$wrong=error")
        .isExSyntax(UriParserSyntaxException.MessageKeys.UNKNOWN_SYSTEM_QUERY_OPTION);
  }

  @Test
  void errors() {
    testUri.runEx("FICRTString(wrong1='ABC')/olingo.odata.test1.BFCStringRTESTwoKeyNav()")
        .isExSemantic(MessageKeys.FUNCTION_NOT_FOUND);
    testUri.runEx("FICRTString(wrong1='ABC',wrong2=1)/olingo.odata.test1.BFCStringRTESTwoKeyNav()")
        .isExSemantic(MessageKeys.FUNCTION_NOT_FOUND);

    // type filter for entity incompatible
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/olingo.odata.test1.ETBase")
        .isExSemantic(MessageKeys.INCOMPATIBLE_TYPE_FILTER);

    // type filter for entity double on entry
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')"
        + "/olingo.odata.test1.ETBaseTwoKeyNav/olingo.odata.test1.ETBaseTwoKeyNav")
        .isExSemantic(MessageKeys.TYPE_FILTER_NOT_CHAINABLE);
    // type filter for entity double on collection
    testUri.runEx("ESTwoKeyNav/olingo.odata.test1.ETBaseTwoKeyNav/olingo.odata.test1.ETBaseTwoKeyNav")
        .isExSemantic(MessageKeys.TYPE_FILTER_NOT_CHAINABLE);
    // type filter for entity double on non key pred
    testUri.runEx("SINav/olingo.odata.test1.ETBaseTwoKeyNav/olingo.odata.test1.ETBaseTwoKeyNav")
        .isExSemantic(MessageKeys.TYPE_FILTER_NOT_CHAINABLE);

    // type filter for complex incompatible
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/PropertyCompTwoPrim"
        + "/olingo.odata.test1.CTCollAllPrim")
        .isExSemantic(MessageKeys.INCOMPATIBLE_TYPE_FILTER);

    // type filter for complex double on entry
    testUri.runEx("FICRTCTTwoPrimTwoParam(ParameterInt16=1,ParameterString='2')"
        + "/olingo.odata.test1.CTBase/olingo.odata.test1.CTBase")
        .isExSemantic(MessageKeys.TYPE_FILTER_NOT_CHAINABLE);

    // type filter for complex double on collection
    testUri.runEx("FICRTCollCTTwoPrimTwoParam(ParameterInt16=1,ParameterString='2')"
        + "/olingo.odata.test1.CTBase/olingo.odata.test1.CTBase")
        .isExSemantic(MessageKeys.TYPE_FILTER_NOT_CHAINABLE);

    // type filter for complex double on non key pred
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/PropertyCompTwoPrim"
        + "/olingo.odata.test1.CTBase/olingo.odata.test1.CTBase")
        .isExSemantic(MessageKeys.TYPE_FILTER_NOT_CHAINABLE);

    testUri.runEx("ESTwoKeyNav/olingo.odata.test1.BFC_RTESTwoKeyNav_")
        .isExSemantic(MessageKeys.UNKNOWN_TYPE);

    // $ref
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/PropertyCompTwoPrim/$ref")
        .isExSemantic(MessageKeys.ONLY_FOR_ENTITY_TYPES);

    // $count
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/PropertyCompTwoPrim/$count")
        .isExSemantic(MessageKeys.ONLY_FOR_COLLECTIONS);

    // Actions must not be followed by anything.
    testUri.runEx("ESTwoKeyNav(PropertyInt16=1,PropertyString='2')/"
        + "olingo.odata.test1.BA_RTETTwoKeyNav/olingo.odata.test1.ETTwoKeyNav")
        .isExValidation(UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH);
    testUri.runEx("ESTwoKeyNav/olingo.odata.test1.BAESTwoKeyNavRTESTwoKeyNav/$count")
        .isExValidation(UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH);
  }

  @Test
  void doublePercentDecoding() throws Exception {
    testUri.runEx("ESAllPrim%252832767%29").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
  }

  @Test
  void multipleKeysInResourcePath() throws Exception {
    // See OLINGO-730
    testUri.runEx("ESAllPrim(32767)(1)(2)").isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
  }

  @Test
  void startElementsInsteadOfNavigationProperties() {
    testUri.runEx("ESAllPrim(0)/ESAllPrim(0)/ESAllPrim(0)").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("ESAllPrim(0)/SINav").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("ESAllPrim(0)/FICRTString()").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("ESAllPrim(0)/AIRTString").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("SI/ESAllPrim(0)").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("SI/SINav").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("SI/FICRTString()").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("SI/AIRTString").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("FICRTETKeyNav()/ESAllPrim(0)").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("FICRTETKeyNav()/SINav").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("FICRTETKeyNav()/FICRTString()").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("FICRTETKeyNav()/AIRTString").isExSemantic(MessageKeys.PROPERTY_NOT_IN_TYPE);
    testUri.runEx("AIRTESAllPrimParam/ESAllPrim(0)")
        .isExValidation(UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH);
    testUri.runEx("AIRTESAllPrimParam/SINav")
        .isExValidation(UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH);
    testUri.runEx("AIRTESAllPrimParam/FICRTString()")
        .isExValidation(UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH);
    testUri.runEx("AIRTESAllPrimParam/AIRTString")
        .isExValidation(UriValidationException.MessageKeys.UNALLOWED_RESOURCE_PATH);
  }

  @Test
  void firstResourcePathWithNamespace() {
    testUri.runEx("olingo.odata.test1.ESAllPrim").isExSemantic(MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT);
    testUri.runEx("olingo.odata.test1.ESAllPrim(0)").isExSemantic(MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT);
    testUri.runEx("olingo.odata.test1.FINRTInt16()").isExSemantic(MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT);
    testUri.runEx("olingo.odata.test1.AIRTString").isExSemantic(MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT);
    testUri.runEx("olingo.odata.test1.SINav").isExSemantic(MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT);
  }

  @Test
  void alias() throws Exception {
    testUri.run("ESTwoKeyNav(PropertyInt16=1,PropertyString=@A)", "@A='2'").goPath()
        .isKeyPredicate(0, "PropertyInt16", "1")
        .isKeyPredicateAlias(1, "PropertyString", "@A")
        .goUpUriValidator().isInAliasToValueMap("@A", "'2'");
    testUri.run("ESAllPrim(PropertyInt16=@p1)", "@p1=1").goPath()
        .isKeyPredicateAlias(0, "PropertyInt16", "@p1")
        .goUpUriValidator().isInAliasToValueMap("@p1", "1");
    testUri.run("ESAllPrim(@p1)", "@p1=-2").goPath()
        .isKeyPredicateAlias(0, "PropertyInt16", "@p1")
        .goUpUriValidator().isInAliasToValueMap("@p1", "-2");

    testFilter.runOnETAllPrim("PropertyInt16 gt @alias&@alias=1")
        .is("<<PropertyInt16> gt <@alias>>")
        .right().isAlias("@alias");
    testFilter.runOnETAllPrim("@alias&@alias=@otherAlias&@otherAlias=true")
        .isAlias("@alias");

    testUri.runEx("ESAllPrim(@p1)")
        .isExValidation(UriValidationException.MessageKeys.MISSING_ALIAS);
    // 'ewe' is not a literal of the Int16 key type, so it is rejected -- since OData 4.01 lets a
    // quoted string stand for a primitive value (13.2.1 item 9a), the rejection now comes from key
    // validation rather than from alias parsing. Both answer 400.
    testUri.runEx("ESAllPrim(PropertyInt16=@p1)", "@p1='ewe'")
        .isExValidation(UriValidationException.MessageKeys.INVALID_KEY_PROPERTY);
    testUri.runEx("ESAllPrim(PropertyInt16=@p1)", "@p1='ewe")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
    testFilter.runOnETKeyNavEx("PropertyInt16 gt @alias")
        .isInAliasToValueMap("@alias", null);
    testFilter.runOnETKeyNavEx("PropertyInt16 gt @alias&@alias=@alias")
        .isInAliasToValueMap("@alias", "@alias");
    testFilter.runOnETKeyNavEx("@alias&@alias=@alias2&@alias2=true or @alias")
        .isInAliasToValueMap("@alias", "@alias2");
  }

  @Test
  void functionImportParameterAlias() throws Exception {
    testUri.run("FICRTCollESTwoKeyNavParam(ParameterInt16=@parameterAlias)", "@parameterAlias=1");
    testUri.run("FICRTCollESTwoKeyNavParam(ParameterInt16=@parameterAlias)/$count", "@parameterAlias=1");
    testUri.runEx("FICRTCollESTwoKeyNavParam(ParameterInt16=@invalidAlias)", "@validAlias=1")
        .isExValidation(UriValidationException.MessageKeys.MISSING_ALIAS);
  }

  @Test
  void functionsWithComplexParameters() throws Exception {
    testUri.run("ESTwoKeyNav/olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1)", "@p1={\"PropertyInt16\":1,\"PropertyString\":\"1\"}")
        .goPath()
        .first().isEntitySet("ESTwoKeyNav")
        .n().isFunction("BFCESTwoKeyNavRTStringParam").isParameterAlias(0, "ParameterComp", "@p1")
        .goUpUriValidator().isInAliasToValueMap("@p1", "{\"PropertyInt16\":1,\"PropertyString\":\"1\"}");

    // Test JSON String lexer rule =\"3,Int16=abc},\\\nabc&test%test\b\f\r\t\u0022\\}\\{\\)\\(\\]\\[}
    final String stringValueEncoded = "=\\\"3,Int16=abc},\\\\\\nabc%26test%25test\\b\\f\\r\\t\\u0022\\\\}\\\\{\\\\)"
        + "\\\\(\\\\]\\\\[}";
    final String stringValueDecoded = "=\\\"3,Int16=abc},\\\\\\nabc&test%test\\b\\f\\r\\t\\u0022\\\\}\\\\{\\\\)"
        + "\\\\(\\\\]\\\\[}";

    testUri.run("ESTwoKeyNav/olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1)", "@p1={\"PropertyInt16\":1,\"PropertyString\":\"" + stringValueEncoded + "\"}")
        .goPath()
        .first().isEntitySet("ESTwoKeyNav")
        .n().isFunction("BFCESTwoKeyNavRTStringParam").isParameterAlias(0, "ParameterComp", "@p1")
        .goUpUriValidator()
        .isInAliasToValueMap("@p1", "{\"PropertyInt16\":1,\"PropertyString\":\"" + stringValueDecoded + "\"}");

    testFilter.runOnETTwoKeyNav("olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp={\"PropertyString\":\"Test\",\"PropertyInt16\":1}) eq 'Test'")
        .is("<<BFCESTwoKeyNavRTStringParam> eq <'Test'>>")
        .isBinary(BinaryOperatorKind.EQ)
        .left().isParameterText(0, "{\"PropertyString\":\"Test\",\"PropertyInt16\":1}");

    testFilter.runOnETTwoKeyNav("olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp={\"PropertyString\":\"" + stringValueEncoded + "\",\"PropertyInt16\":1}) eq 'Test'")
        .is("<<BFCESTwoKeyNavRTStringParam> eq <'Test'>>")
        .left().isParameterText(0, "{\"PropertyString\":\"" + stringValueDecoded + "\",\"PropertyInt16\":1}");

    testUri.run("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":1,\"PropertyString\":\"1\"}");

    testUri.run("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":1,\"PropertyString\":null}")
        .goFilter().left().isParameterText(0, null);

    testUri.run("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={}");

    testUri.run("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":[1,2,3],\"PropertyString\":\"1\"}");

    testUri.run("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":[\"1\",\"2\",\"3\"],\"PropertyString\":\"1\"}");

    testUri.run("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":[{\"Prop1\":123,\"Prop2\":\"Test\",\"Prop3\":[1,2,3]},"
        + "{\"Prop1\":{\"Prop1\":[\"Prop\\\":{]\"]}}],\"PropertyString\":\"1\"}");

    testUri.run("FINRTByteNineParam(ParameterEnum=null,ParameterDef='x',ParameterComp=@c,"
        + "ParameterETTwoPrim=@c,CollParameterByte=@e,CollParameterEnum=@e,CollParameterDef=@e,"
        + "CollParameterComp=@e,CollParameterETTwoPrim=@e)",
        "@c={}&@e=[]");

    testUri.runEx("ESTwoKeyNav/olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1)", "@p1={\"PropertyInt16\":1,\"PropertyString\":'1'}")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);

    testUri.runEx("ESTwoKeyNav/olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp={\"PropertyInt16\":1,\"PropertyString\":\"Test\"})")
        .isExSemantic(MessageKeys.COMPLEX_PARAMETER_IN_RESOURCE_PATH);

    testUri.runEx("FICRTCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=null)")
        .isExValidation(UriValidationException.MessageKeys.MISSING_PARAMETER);

    testUri.runEx("FICRTCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)")
        .isExValidation(UriValidationException.MessageKeys.MISSING_ALIAS);

    testUri.runEx("FICRTCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)", "@test=null")
        .isExValidation(UriValidationException.MessageKeys.MISSING_ALIAS);

    testUri.run("FICRTCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)", "@test='null'");

    testUri.runEx("FICRTCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test,UnknownParam=1)", "@test='null'")
        .isExSemantic(MessageKeys.FUNCTION_NOT_FOUND);

    testUri.run("FICRTCollCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)", "@test='null'");
    testUri.run("FICRTCollCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)", "@test=null");
    testUri.run("FICRTCollCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)");
    testUri.run("FICRTCollCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=null)");

    testUri.runEx("FICRTCollCTTwoPrimTwoParam(ParameterInt16=1,ParameterString=@test)", "@test=null&@test='1'")
        .isExSyntax(UriParserSyntaxException.MessageKeys.DUPLICATED_ALIAS);

    testFilter.runOnETKeyNavEx("FINRTInt16() eq 0")
        .isExSemantic(MessageKeys.EXPRESSION_PROPERTY_NOT_IN_TYPE);

    testUri.runEx("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":1,\"PropertyString\":\"1\"")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);

    testUri.runEx("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":1,\"PropertyString\":\"1\"}}")
        .isExSemantic(MessageKeys.UNKNOWN_PART);

    testUri.runEx("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":[1,2,3]],\"PropertyString\":\"1\"}")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);

    testUri.runEx("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":[1,2,3,\"PropertyString\":\"1\"}")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);

    testUri.runEx("ESTwoKeyNav", "$filter=olingo.odata.test1.BFCESTwoKeyNavRTStringParam"
        + "(ParameterComp=@p1) eq '0'&@p1={\"PropertyInt16\":[1,2,3},\"PropertyString\":\"1\"}")
        .isExSyntax(UriParserSyntaxException.MessageKeys.SYNTAX);
  }

  @Test
  void allowedSystemQueryOptionsOnAll() throws Exception {
    testUri.run("$all", "$count=true&$format=json&$search=abc&$skip=5&$top=5&$skiptoken=abc")
        .isKind(UriInfoKind.all)
        .isInlineCount(true)
        .isFormatText("json")
        .isSearchSerialized("'abc'")
        .isSkip(5)
        .isTop(5)
        .isSkipTokenText("abc");
  }

  @Test
  void allowedSystemQueryOptionsOnCrossjoin() throws Exception {
    testUri.run("$crossjoin(ESAllPrim,ESTwoPrim)", "$count=true&$expand=ESAllPrim"
        + "&$filter=ESAllPrim/PropertyInt16 eq 2&$format=json&$orderby=ESAllPrim/PropertyInt16"
        + "&$search=abc&$skip=5&$top=5&$skiptoken=abc")
        .isKind(UriInfoKind.crossjoin)
        .isInlineCount(true)
        .goExpand().goPath().isEntitySet("ESAllPrim")
        .goUpExpandValidator().goUpToUriValidator()
        .goFilter().left().goPath().first().isEntitySet("ESAllPrim")
        .n().isPrimitiveProperty("PropertyInt16", PropertyProvider.nameInt16, false)
        .goUpFilterValidator().goUpToUriValidator()
        .isFormatText("json")
        .isSearchSerialized("'abc'")
        .isSkip(5)
        .isTop(5)
        .isSkipTokenText("abc");
  }

  @Test
  void trimQueryOptionsValue() throws Exception {
    // OLINGO-846 trim query option value
    testUri.run("ESAllPrim", "$filter= PropertyInt16 eq 12 ")
        .isKind(UriInfoKind.resource).goPath()
        .first().isEntitySet("ESAllPrim");
    // OLINGO-846 trim query option value
    testUri.run("ESAllPrim", "$filter= PropertyInt16 eq 12 ")
        .isKind(UriInfoKind.resource).goFilter().isBinary(BinaryOperatorKind.EQ).is("<<PropertyInt16> eq <12>>");
    
    testUri.run("ESAllPrim", "%20$filter%20=%20PropertyInt16%20%20eq%2012%20")
      .isKind(UriInfoKind.resource).goFilter().isBinary(BinaryOperatorKind.EQ).is("<<PropertyInt16> eq <12>>");    
  }

  @Test
  void customQueryOption() throws Exception {
    testUri.run("ESTwoKeyNav", "custom")
        .isCustomParameter(0, "custom", "");
    testUri.run("ESTwoKeyNav", "custom=ABC")
        .isCustomParameter(0, "custom", "ABC");
  }
  
  @Test
  void testValidationOnFunctions() throws Exception {
    // A quoted string holding a literal of the parameter's own type is a valid 4.01 call
    // ([OData-Protocol] 13.2.1 item 9a); one that is not a literal of that type still fails.
    testUri.run("FICRTETTwoKeyNavParam(ParameterInt16='32')");
    testUri.runEx("FICRTETTwoKeyNavParam(ParameterInt16='ewe')")
    .isExValidation(UriValidationException.MessageKeys.INVALID_VALUE_FOR_PROPERTY);
  
    testUri.runEx("FICRTETTwoKeyNavParam(ParameterInt16=null)")
    .isExValidation(UriValidationException.MessageKeys.MISSING_PARAMETER);
  
    // '32' IS a literal of the Int16 parameter type, so 13.2.1 item 9a makes this a valid call.
    testUri.run("FICRTETTwoKeyNavParam(ParameterInt16=@p1)", "@p1='32'");
    
    testUri.run("FICRTETTwoKeyNavParam(ParameterInt16=32)");
  }

  /** OLINGO-1184: Bound function after type filter in $filter must parse correctly. */
  @Test
  void boundFunctionAfterTypeFilterInFilter() throws Exception {
    // Navigate to ETTwoKeyNav, type-filter to ETBaseTwoKeyNav, call bound function, access property
    testUri.run("ESBaseTwoKeyNav",
        "$filter=NavPropertyETTwoKeyNavOne/olingo.odata.test1.ETBaseTwoKeyNav"
        + "/olingo.odata.test1.BFCETBaseTwoKeyNavRTETTwoKeyNav()/PropertyString eq 'test'");
  }

  /** OData 4.01 Part 1: Protocol, section 11.2.12: $schemaversion MAY be included in any request. */
  @Test
  void schemaVersion() throws Exception {
    testUri.run("$metadata", "$schemaversion=*").isKind(UriInfoKind.metadata);
    testUri.run("ESAllPrim", "$schemaversion=1.0.0").isKind(UriInfoKind.resource)
        .isSchemaVersionText("1.0.0");
    testUri.runEx("ESAllPrim", "$schemaversion=")
        .isExSyntax(UriParserSyntaxException.MessageKeys.WRONG_VALUE_FOR_SYSTEM_QUERY_OPTION);
    testUri.runEx("ESAllPrim", "$schemaversion=1&$schemaversion=2")
        .isExSyntax(UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION);
    // Since OData 4.01 ([OData-Protocol] 13.2.1 items 6 and 7) system query option names are
    // case-insensitive and the "$" prefix is optional, so these spell the same option.
    testUri.run("ESAllPrim", "$SchemaVersion=1.0.0").isKind(UriInfoKind.resource)
        .isSchemaVersionText("1.0.0");
    testUri.run("ESAllPrim", "schemaversion=1.0.0").isKind(UriInfoKind.resource)
        .isSchemaVersionText("1.0.0");
    testUri.runEx("ESAllPrim", "$schemaversion=1&SCHEMAVERSION=2")
        .isExSyntax(UriParserSyntaxException.MessageKeys.DOUBLE_SYSTEM_QUERY_OPTION);
  }
}
