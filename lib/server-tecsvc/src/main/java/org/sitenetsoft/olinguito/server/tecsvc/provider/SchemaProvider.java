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
 * Copyright 2026 SiteNetSoft - Modernized Collections usage
 *
 * Copyright 2026 SiteNetSoft - Add OpenType support (tecsvc ETOpen model)
 * Copyright 2026 SiteNetSoft - OpenType: register open complex type CTOpen
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 3: publish the schema version through the
 * Core.SchemaVersion annotation (OData 4.01, Part 1: Protocol, section 11.2.12)
 */
package org.sitenetsoft.olinguito.server.tecsvc.provider;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;

public class SchemaProvider {

  public static final String NAMESPACE = "olingo.odata.test1";
  public static final String NAMESPACE_ALIAS = "Namespace1_Alias";

  /**
   * Version of this service's schema, published in the metadata document through the
   * <code>Core.SchemaVersion</code> annotation and accepted as the value of the
   * <code>$schemaversion</code> system query option (OData 4.01, Part 1: Protocol, section 11.2.12).
   * The same constant is handed to the {@code ServiceMetadata} the tecsvc runtime adapters build,
   * so the published and the enforced version can never drift apart.
   */
  public static final String SCHEMA_VERSION = "1.0.0";

  /** Alias-qualified name of the vocabulary term the schema version is published with. */
  private static final String SCHEMA_VERSION_TERM = "Core.SchemaVersion";

  private final CsdlEdmProvider prov;

  public SchemaProvider(final CsdlEdmProvider prov) {
    this.prov = prov;
  }

  public List<CsdlSchema> getSchemas() throws ODataException {
    CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(NAMESPACE);
    schema.setAlias(NAMESPACE_ALIAS);

    // EnumTypes
    schema.setEnumTypes(List.of(
        prov.getEnumType(EnumTypeProvider.nameENString)));

    // TypeDefinitions
    schema.setTypeDefinitions(List.of(
        prov.getTypeDefinition(TypeDefinitionProvider.nameTDString)));

    // EntityTypes
    List<CsdlEntityType> entityTypes = new ArrayList<>();
    schema.setEntityTypes(entityTypes);
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETDeriveCollComp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETAllPrim));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETAllPrimDefaultValues));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCollAllPrim));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoPrim));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETMixPrimCollComp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoKeyTwoPrim));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETMixEnumDefCollComp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETBase));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoBase));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETAllKey));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCompAllPrim));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCompCollAllPrim));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCompComp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCompCollComp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETMedia));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETFourKeyAlias));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETServerSidePaging));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETStreamServerSidePaging));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETAllNullable));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETKeyNav));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoKeyNav));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETBaseTwoKeyNav));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoBaseTwoKeyNav));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETKeyNavCont));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoKeyNavCont));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCompMixPrimCollComp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETKeyPrimNav));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETStream));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETDelta)); 
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETCont)); 
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETBaseCont)); 
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETTwoCont)); 
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETStreamOnComplexProp));
    entityTypes.add(prov.getEntityType(EntityTypeProvider.nameETOpen));

    // ComplexTypes
    List<CsdlComplexType> complexTypes = new ArrayList<>();
    schema.setComplexTypes(complexTypes);
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTPrim));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTAllPrim));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTCollAllPrim));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTTwoPrim));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTMixPrimCollComp));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTMixEnumDef));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTBase));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTTwoBase));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTCompComp));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTCompCollComp));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTPrimComp));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTNavFiveProp));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTBasePrimCompNav));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTTwoBasePrimCompNav));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTCompNav));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTNavCont));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTCompCollCompAno));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTTwoPrimAno));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTBaseAno));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTWithStreamProp));
    complexTypes.add(prov.getComplexType(ComplexTypeProvider.nameCTOpen));
    
    // Actions
    List<CsdlAction> actions = new ArrayList<>();
    schema.setActions(actions);
    actions.addAll(prov.getActions(ActionProvider.nameBA_RTETTwoKeyNav));
    actions.addAll(prov.getActions(ActionProvider.nameBAESAllPrimRTETAllPrim));
    actions.addAll(prov.getActions(ActionProvider.nameBAESTwoKeyNavRTESTwoKeyNav));
    actions.addAll(prov.getActions(ActionProvider.nameBAESTwoKeyNavRTESKeyNav));
    actions.addAll(prov.getActions(ActionProvider.nameBAETBaseTwoKeyNavRTETBaseTwoKeyNav));
    actions.addAll(prov.getActions(ActionProvider.nameBAETTwoBaseTwoKeyNavRTETBaseTwoKeyNav));
    actions.addAll(prov.getActions(ActionProvider.nameBAETAllPrimRT));
    actions.addAll(prov.getActions(ActionProvider.nameBAESAllPrimRT));
    actions.addAll(prov.getActions(ActionProvider.nameBAETTwoPrimRTString));
    actions.addAll(prov.getActions(ActionProvider.nameBAETTwoPrimRTCollString));
    actions.addAll(prov.getActions(ActionProvider.nameBAETTwoPrimRTCTAllPrim));
    actions.addAll(prov.getActions(ActionProvider.nameBAETTwoPrimRTCollCTAllPrim));
    actions.addAll(prov.getActions(ActionProvider.nameBAETCompAllPrimRTETCompAllPrim));
    actions.addAll(prov.getActions(ActionProvider.nameBAETTwoKeyNavRTETTwoKeyNavParam));
    actions.addAll(prov.getActions(ActionProvider.nameBAETMixPrimCollCompRTCTTwoPrim));
    actions.addAll(prov.getActions(ActionProvider.nameBAETBaseETTwoBaseRTETTwoBase));
    actions.addAll(prov.getActions(ActionProvider.nameBAETMixPrimCollCompCTTWOPrimCompRTCollCTTwoPrim));
    actions.addAll(prov.getActions(ActionProvider.nameBAETMixPrimCollCompCTTWOPrimCompRTCTTwoPrim));
    actions.addAll(prov.getActions(ActionProvider.
        nameBAETTwoKeyNavCTBasePrimCompNavCTTwoBasePrimCompNavRTCTTwoBasePrimCompNav));
    actions.addAll(prov.getActions(ActionProvider.nameUARTString));
    actions.addAll(prov.getActions(ActionProvider.nameUARTCollStringTwoParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTCTTwoPrimParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTCollCTTwoPrimParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTETTwoKeyTwoPrimParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTCollETKeyNavParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTETAllPrimParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTCollETAllPrimParam));
    actions.addAll(prov.getActions(ActionProvider.nameUART));
    actions.addAll(prov.getActions(ActionProvider.nameUARTParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTTwoParam));
    actions.addAll(prov.getActions(ActionProvider.nameUARTByteNineParam));
    actions.addAll(prov.getActions(ActionProvider.name_A_RTTimeOfDay_));
    
    // Functions
    List<CsdlFunction> functions = new ArrayList<>();
    schema.setFunctions(functions);
    
    functions.addAll(prov.getFunctions(FunctionProvider.name_FC_RTTimeOfDay_));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFNRTInt16));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTETKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTETTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTETTwoKeyNavParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTETTwoKeyNavParamCTTwoPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTStringTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollETTwoKeyNavParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTString));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollStringTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollString));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCTAllPrimTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCTTwoPrimTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollCTTwoPrimTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCTTwoPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollCTTwoPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTETMedia));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollETMedia));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFNRTCollETMixPrimCollCompTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTETAllPrimTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollETMixPrimCollCompTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFNRTCollCTNavFiveProp));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollETKeyNavContParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFNRTByteNineParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTCollDecimal));
    functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTDecimal));

    functions.addAll(prov.getFunctions(FunctionProvider.nameBFC_RTESTwoKeyNav_));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCStringRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETBaseTwoKeyNavRTETTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESBaseTwoKeyNavRTESBaseTwoKey));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFNESAllPrimRTCTAllPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTCTTwoPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTCollCTTwoPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTString));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTCollString));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETTwoKeyNavRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETBaseTwoKeyNavRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCSINavRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETBaseTwoKeyNavRTESBaseTwoKey));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCCollStringRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCCTPrimCompRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCCTPrimCompRTESBaseTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCCollCTPrimCompRTESAllPrim));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESKeyNavRTETKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETKeyNavRTETKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFESTwoKeyNavRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETTwoKeyNavRTETTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCETTwoKeyNavRTCTTwoPrim));
    // functions.addAll(prov.getFunctions(FunctionProvider.nameUFCRTESMixPrimCollCompTwoParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTCTNavFiveProp));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTCollCTNavFiveProp));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTStringParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESKeyNavRTETKeyNavParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCCTPrimCompRTETTwoKeyNavParam));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESKeyNavRTESTwoKeyNav));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFNESTwoKeyNavRTString)); 
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFCESTwoKeyNavRTCollDecimal));
    functions.addAll(prov.getFunctions(FunctionProvider.nameBFESBaseRTESTwoBase));

    // functions.addAll(prov.getFunctions(FunctionProvider.nameBFCCTPrimCompRTESTwoKeyNavParam));

    // EntityContainer
    schema.setEntityContainer(prov.getEntityContainer());

    // Schema version (OData 4.01, Part 1: Protocol, section 11.2.12)
    schema.setAnnotations(List.of(new CsdlAnnotation().setTerm(SCHEMA_VERSION_TERM)
        .setExpression(new CsdlConstantExpression(ConstantExpressionType.String, SCHEMA_VERSION))));

    return List.of(schema);
  }

}
