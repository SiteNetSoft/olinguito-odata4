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
 * Copyright 2026 SiteNetSoft - OData 4.01: dedicated exception for ambiguous function overloads
 */
package org.sitenetsoft.olinguito.commons.api.edm;

import java.io.Serial;

/**
 * Thrown when the parameter names specified for a function invocation cover the non-optional
 * parameters of more than one function overload and no overload matches exactly.
 * <p>
 * OData 4.01, Protocol section 11.5.4.2 allows a service to reject such an invocation; this library
 * does so and maps this exception to a client error (400) instead of a server error (500). It is a
 * distinct subtype of {@link EdmException} so that genuine model errors keep their original
 * (server-error) treatment.
 */
public class EdmAmbiguousOverloadException extends EdmException {

  @Serial

  private static final long serialVersionUID = 1L;

  public EdmAmbiguousOverloadException(final String msg) {
    super(msg);
  }
}
