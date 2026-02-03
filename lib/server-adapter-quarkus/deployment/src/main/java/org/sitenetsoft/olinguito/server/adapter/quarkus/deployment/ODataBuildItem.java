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
package org.sitenetsoft.olinguito.server.adapter.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that carries OData configuration information
 * from build steps to runtime initialization.
 */
public final class ODataBuildItem extends SimpleBuildItem {

    private final String path;
    private final boolean enabled;

    public ODataBuildItem(String path, boolean enabled) {
        this.path = path;
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
