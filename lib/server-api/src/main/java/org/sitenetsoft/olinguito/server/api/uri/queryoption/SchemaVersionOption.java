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
 * Copyright 2026 SiteNetSoft - Tier 5 Wave 2 Task 1: $schemaversion system query option
 */
package org.sitenetsoft.olinguito.server.api.uri.queryoption;

/**
 * Represents the system query option $schemaversion (OData 4.01, Part 1: Protocol, section 11.2.12).
 * Per the specification this option MAY be included in any request; the service returns metadata
 * consistent with the requested schema version, or an appropriate error if the version is unknown.
 * For example: http://.../ESAllPrim?$schemaversion=1.0
 */
public interface SchemaVersionOption extends SystemQueryOption {

  /**
   * @return Value of $schemaversion
   */
  String getSchemaVersion();

}
