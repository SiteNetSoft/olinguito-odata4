package org.sitenetsoft.olinguito.server.adapter.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.sitenetsoft.olinguito.server.api.ODataRequest;

public final class ServletHeaderCopier {
    private ServletHeaderCopier() {}

    public static void copyHeaders(ODataRequest odRequest, HttpServletRequest req) {
        for (final Enumeration<?> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
            final String headerName = (String) headerNames.nextElement();
            @SuppressWarnings("unchecked")
            final List<String> headerValues = Collections.list(req.getHeaders(headerName));
            odRequest.addHeader(headerName, headerValues);
        }
    }
}
