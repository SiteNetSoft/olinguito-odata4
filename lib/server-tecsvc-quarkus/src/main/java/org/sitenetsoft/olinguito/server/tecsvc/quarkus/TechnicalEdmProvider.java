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
package org.sitenetsoft.olinguito.server.tecsvc.quarkus;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAliasInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.server.tecsvc.provider.EdmTechProvider;

import java.util.List;

/**
 * CDI bean wrapper around the EdmTechProvider for Quarkus.
 */
@ApplicationScoped
public class TechnicalEdmProvider extends CsdlAbstractEdmProvider {

    private final EdmTechProvider delegate = new EdmTechProvider();

    @Override
    public CsdlEnumType getEnumType(FullQualifiedName enumTypeName) throws ODataException {
        return delegate.getEnumType(enumTypeName);
    }

    @Override
    public CsdlTypeDefinition getTypeDefinition(FullQualifiedName typeDefinitionName) throws ODataException {
        return delegate.getTypeDefinition(typeDefinitionName);
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        return delegate.getEntityType(entityTypeName);
    }

    @Override
    public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) throws ODataException {
        return delegate.getComplexType(complexTypeName);
    }

    @Override
    public List<CsdlAction> getActions(FullQualifiedName actionName) throws ODataException {
        return delegate.getActions(actionName);
    }

    @Override
    public List<CsdlFunction> getFunctions(FullQualifiedName functionName) throws ODataException {
        return delegate.getFunctions(functionName);
    }

    @Override
    public CsdlTerm getTerm(FullQualifiedName termName) throws ODataException {
        return delegate.getTerm(termName);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        return delegate.getEntitySet(entityContainer, entitySetName);
    }

    @Override
    public CsdlSingleton getSingleton(FullQualifiedName entityContainer, String singletonName) throws ODataException {
        return delegate.getSingleton(entityContainer, singletonName);
    }

    @Override
    public CsdlActionImport getActionImport(FullQualifiedName entityContainer, String actionImportName)
            throws ODataException {
        return delegate.getActionImport(entityContainer, actionImportName);
    }

    @Override
    public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, String functionImportName)
            throws ODataException {
        return delegate.getFunctionImport(entityContainer, functionImportName);
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        return delegate.getEntityContainerInfo(entityContainerName);
    }

    @Override
    public List<CsdlAliasInfo> getAliasInfos() throws ODataException {
        return delegate.getAliasInfos();
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        return delegate.getSchemas();
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        return delegate.getEntityContainer();
    }

    @Override
    public CsdlAnnotations getAnnotationsGroup(FullQualifiedName targetName, String qualifier) throws ODataException {
        return delegate.getAnnotationsGroup(targetName, qualifier);
    }
}
