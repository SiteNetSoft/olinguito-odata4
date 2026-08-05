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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.sitenetsoft.olinguito.commons.api.edm.Edm;
import org.sitenetsoft.olinguito.commons.api.edm.EdmAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.core.edm.EdmAnnotationsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdmAnnotationsImplTest {

  private Edm edm;

  @BeforeEach
  void setupEdm() {
    edm = mock(Edm.class);
  }

  @Test
  void initialAnnotationGroup() {
    CsdlAnnotations csdlAnnotationGroup = new CsdlAnnotations();
    EdmAnnotations annotationGroup = new EdmAnnotationsImpl(edm, csdlAnnotationGroup);

    assertNotNull(annotationGroup.getAnnotations());
    assertTrue(annotationGroup.getAnnotations().isEmpty());

    assertNull(annotationGroup.getQualifier());
    assertNull(annotationGroup.getTargetPath());
  }

  @Test
  void annotationGroupWithQualifierAndPathButNonValidTarget() {
    CsdlAnnotations csdlAnnotationGroup = new CsdlAnnotations();
    csdlAnnotationGroup.setQualifier("qualifier");
    csdlAnnotationGroup.setTarget("invalid.invalid");
    EdmAnnotations annotationGroup = new EdmAnnotationsImpl(edm, csdlAnnotationGroup);

    assertNotNull(annotationGroup.getAnnotations());
    assertTrue(annotationGroup.getAnnotations().isEmpty());

    assertEquals("qualifier", annotationGroup.getQualifier());
    assertEquals("invalid.invalid", annotationGroup.getTargetPath());
  }
}
