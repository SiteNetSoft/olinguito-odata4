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
 * Copyright 2026 SiteNetSoft - Port OLINGO-1399: expose raw term name for unresolvable terms
 */
package org.sitenetsoft.olinguito.commons.api.edm;

import org.sitenetsoft.olinguito.commons.api.edm.annotation.EdmExpression;

/**
 * This class models an OData Annotation which can be applied to a target.
 */
public interface EdmAnnotation extends EdmAnnotatable {

  /**
   * @return the term of this annotation, or <code>NULL</code> if the term cannot be resolved
   * against the served metadata (for example because its vocabulary is not included)
   */
  EdmTerm getTerm();

  /**
   * <p>Returns the raw term name exactly as written in the underlying CSDL annotation,
   * regardless of whether the term itself can be resolved.</p>
   * <p>This is a fallback for serializers: {@link #getTerm()} returns <code>NULL</code> for a
   * term whose vocabulary is not part of the served metadata, but the (mandatory) term name is
   * still needed to emit valid CSDL.</p>
   * @return the raw term name, or <code>NULL</code> if none was specified
   */
  String getTermName();

  /**
   * @return the qualifier for this annotation. Might be <code>NULL</code>
   */
  String getQualifier();

  EdmExpression getExpression();
}
