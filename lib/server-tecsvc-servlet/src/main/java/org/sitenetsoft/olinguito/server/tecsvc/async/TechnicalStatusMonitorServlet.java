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
 * Copyright 2026 SiteNetSoft - Moved from server-tecsvc to server-tecsvc-servlet
 * Copyright 2026 SiteNetSoft - Tier 6 Wave 3 Task 10: reduced to a bridge onto the request handler,
 * which serves the status monitor resource itself ([OData-Protocol] section 11.6)
 */
package org.sitenetsoft.olinguito.server.tecsvc.async;

import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sitenetsoft.olinguito.server.tecsvc.TechnicalServlet;

/**
 * Serves the status monitor resources of the technical service.
 *
 * <p>The monitor URL sits outside the OData servlet's path (see
 * {@link TechnicalAsyncService}, which mints <code>&lt;service root's parent&gt;/status/&lt;n&gt;</code>),
 * so it needs its own servlet mapping — but not its own logic: this servlet builds exactly the same
 * request handler as {@link TechnicalServlet}, with {@link TechnicalAsyncService} registered, and
 * lets the handler recognize the monitor request and render both result shapes of
 * [OData-Protocol] section 11.6.</p>
 *
 * <p>The one thing that is not OData is <code>/status/list</code>, a development aid listing the
 * queued invocations.</p>
 */
public class TechnicalStatusMonitorServlet extends TechnicalServlet {

  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    if ("/list".equals(request.getPathInfo())) {
      listQueue(response);
      return;
    }
    super.service(request, response);
  }

  /** Writes a plain listing of the queued invocations and their state. */
  private static void listQueue(final HttpServletResponse response) throws IOException {
    final StringBuilder sb = new StringBuilder("<html><header/><body><h1>Queued requests</h1><ul>");
    for (final Map.Entry<String, TechnicalAsyncService.AsyncRunner> entry
        : TechnicalAsyncService.getInstance().getRunners().entrySet()) {
      sb.append("<li><b>Location: </b><a href=\"").append(entry.getKey()).append("\">")
          .append(entry.getKey()).append("</a><br/>")
          .append("<b>Finished: </b>").append(entry.getValue().isFinished()).append("<br/>")
          .append("</li>");
    }
    sb.append("</ul></body></html>");

    response.setContentType("text/html;charset=UTF-8");
    response.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
  }
}
