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
package org.sitenetsoft.olinguito.server.core;

import static org.junit.Assert.assertNull;

import org.sitenetsoft.olinguito.server.api.uri.UriInfoEntityId;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoKind;
import org.sitenetsoft.olinguito.server.api.uri.UriInfoMetadata;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.FormatOption;
import org.sitenetsoft.olinguito.server.core.uri.UriInfoImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceComplexPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceCountImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceEntitySetImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceFunctionImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceItImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceLambdaAllImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceLambdaAnyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceNavigationPropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourcePrimitivePropertyImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceRefImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceRootImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceSingletonImpl;
import org.sitenetsoft.olinguito.server.core.uri.UriResourceValueImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.CountOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.DeltaTokenOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.ExpandOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.FilterOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.FormatOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SearchOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SkipOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.SkipTokenOptionImpl;
import org.sitenetsoft.olinguito.server.core.uri.queryoption.TopOptionImpl;
import org.junit.Test;

public class RequestUrlHierarchyVisitorTest {
  
  @Test
  public void visitorTest(){
    RequestURLHierarchyVisitor visitor = new RequestURLHierarchyVisitor();
    assertNull(visitor.getUriInfo());
    UriInfoImpl info = new UriInfoImpl();
    visitor.visit(info.setKind(UriInfoKind.all));
    visitor.visit(info.setKind(UriInfoKind.batch));
    visitor.visit(info.setKind(UriInfoKind.crossjoin));
    visitor.visit(info.setKind(UriInfoKind.entityId));
    visitor.visit(info.setKind(UriInfoKind.service));
    UriInfoEntityId entityId = info;
    visitor.visit(entityId);
    visitor.visit(new UriInfoMetadata() {
      
      @Override
      public String getFragment() {
        return null;
      }
      
      @Override
      public FormatOption getFormatOption() {
        return null;
      }
    });
    visitor.visit(new ExpandOptionImpl());
    visitor.visit(new FilterOptionImpl());
    visitor.visit(new FormatOptionImpl());
    visitor.visit(new CountOptionImpl());
    visitor.visit(new SearchOptionImpl());
    visitor.visit(new SkipOptionImpl());
    visitor.visit(new SkipTokenOptionImpl());
    visitor.visit(new TopOptionImpl());
    visitor.visit(new UriResourceCountImpl());
    visitor.visit(new DeltaTokenOptionImpl());
    visitor.visit(new UriResourceRefImpl());
    visitor.visit(new UriResourceRootImpl(null, false));
    visitor.visit(new UriResourceValueImpl());
    visitor.visit(new UriResourceEntitySetImpl(null));
    visitor.visit(new UriResourceFunctionImpl(null, null, null));
    visitor.visit(new UriResourceItImpl(null, false));
    visitor.visit(new UriResourceLambdaAllImpl(null, null));
    visitor.visit(new UriResourceLambdaAnyImpl(null, null));
    visitor.visit(new UriResourceNavigationPropertyImpl(null));
    visitor.visit(new UriResourceSingletonImpl(null));
    visitor.visit(new UriResourceComplexPropertyImpl(null));
    visitor.visit(new UriResourcePrimitivePropertyImpl(null));
  }
}
