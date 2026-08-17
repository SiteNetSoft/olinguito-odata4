/*
 * Copyright 2026 SiteNetSoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2026 SiteNetSoft - OData 4.01: forward the key-as-segment flag to the wrapped handler
 */
package org.sitenetsoft.olinguito.server.core;

import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataRequestHandler;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.OlingoExtension;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.Processor;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;

public class ODataRequestHandlerImpl implements ODataRequestHandler {

  private final ODataHandlerImpl handler;

  public ODataRequestHandlerImpl(final OData odata, final ServiceMetadata serviceMetadata) {
    ServerCoreDebugger debugger = new ServerCoreDebugger(odata);
    handler = new ODataHandlerImpl(odata, serviceMetadata, debugger);
  }

  @Override
  public ODataResponse process(ODataRequest request) {
    return handler.process(request);
  }

  @Override
  public void setSplit(final int split) {
  }

  @Override
  public void register(final Processor processor) {
    handler.register(processor);
  }

  @Override
  public void register(OlingoExtension extension) {
    handler.register(extension);
  }

  @Override
  public void setKeyAsSegment(final boolean enabled) {
    handler.setKeyAsSegment(enabled);
  }
}
