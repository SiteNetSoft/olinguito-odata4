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
 * Copyright 2026 SiteNetSoft - Reduced test method visibility
 */
package org.sitenetsoft.olinguito.server.core.edm.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumMember;
import org.sitenetsoft.olinguito.commons.core.edm.EdmMemberImpl;
import org.sitenetsoft.olinguito.commons.core.edm.EdmProviderImpl;
import org.junit.jupiter.api.Test;

class EdmMemberImplTest {

  @Test
  void enumMember() {
    final CsdlEnumMember member = new CsdlEnumMember().setName("name").setValue("value");
    final EdmMemberImpl memberImpl = new EdmMemberImpl(mock(EdmProviderImpl.class), member);

    assertEquals("name", memberImpl.getName());
    assertEquals("value", memberImpl.getValue());
  }

}
