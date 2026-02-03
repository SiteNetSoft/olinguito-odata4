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
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEdmProvider;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.Processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CDI bean that holds the OData handler instance.
 * This is initialized from CDI-discovered beans (CsdlEdmProvider, Processor).
 */
@ApplicationScoped
public class ODataHandlerHolder {

    @Inject
    Instance<CsdlEdmProvider> edmProviderInstance;

    @Inject
    Instance<Processor> processorInstances;

    @Inject
    Instance<EdmxReference> edmxReferenceInstances;

    private volatile ODataRequestHandler handler;

    @PostConstruct
    void init() {
        // Only initialize if we have an EDM provider
        if (!edmProviderInstance.isUnsatisfied()) {
            CsdlEdmProvider edmProvider = edmProviderInstance.get();
            initHandler(edmProvider);
        }
    }

    private void initHandler(CsdlEdmProvider edmProvider) {
        OData odata = OData.newInstance();

        // Collect EDMX references
        List<EdmxReference> references = new ArrayList<>();
        if (!edmxReferenceInstances.isUnsatisfied()) {
            edmxReferenceInstances.forEach(references::add);
        }

        ServiceMetadata serviceMetadata = odata.createServiceMetadata(
                edmProvider,
                references.isEmpty() ? Collections.emptyList() : references);

        ODataRequestHandler requestHandler = odata.createHandler(serviceMetadata);

        // Register all discovered processors
        if (!processorInstances.isUnsatisfied()) {
            processorInstances.forEach(requestHandler::register);
        }

        this.handler = requestHandler;
    }

    /**
     * Gets the OData request handler.
     *
     * @return the handler, or null if not configured
     */
    public ODataRequestHandler getHandler() {
        return handler;
    }

    /**
     * Checks if the handler is available.
     *
     * @return true if handler is configured
     */
    public boolean isAvailable() {
        return handler != null;
    }
}
