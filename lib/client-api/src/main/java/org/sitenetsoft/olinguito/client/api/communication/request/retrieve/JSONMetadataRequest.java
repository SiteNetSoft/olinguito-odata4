/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 1: CSDL JSON metadata request
 */
package org.sitenetsoft.olinguito.client.api.communication.request.retrieve;

import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;

/**
 * This class implements a CSDL JSON metadata request, the [OData-CSDLJSON] representation of the
 * metadata document defined by OData 4.01, Part 1: Protocol section 11.1.2.
 * <br/>
 * The body type is the same {@link XMLMetadata} the CSDL XML request produces: it is the client's
 * format-neutral holder for a parsed CSDL document, not an XML-specific one.
 */
public interface JSONMetadataRequest extends ODataRetrieveRequest<XMLMetadata> {
//No additional methods needed for now.
}
