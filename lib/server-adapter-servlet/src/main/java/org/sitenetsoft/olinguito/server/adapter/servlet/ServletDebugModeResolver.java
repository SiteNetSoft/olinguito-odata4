package org.sitenetsoft.olinguito.server.adapter.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.sitenetsoft.olinguito.server.api.debug.DebugSupport;
import org.sitenetsoft.olinguito.server.core.debug.ServerCoreDebugger;

public final class ServletDebugModeResolver {
    private ServletDebugModeResolver() {}

    public static void resolve(ServerCoreDebugger debugger, HttpServletRequest request) {
        debugger.resolveDebugMode(request.getParameter(DebugSupport.ODATA_DEBUG_QUERY_PARAMETER));
    }
}