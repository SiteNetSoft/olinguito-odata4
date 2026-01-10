package org.sitenetsoft.olinguito.server.adapter.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.*;

import org.sitenetsoft.olinguito.server.api.*;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;

public final class ServletODataAdapter implements ODataServletHandler {

    private final ODataRequestHandler core;
    private int split = 0;

    public ServletODataAdapter(ODataRequestHandler core) {
        this.core = core;
    }

    @Override
    public void setSplit(int split) {
        this.split = split;
        core.setSplit(split);
    }

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp) {
        ODataRequest odReq = new ODataRequest();
        // TODO: fill odReq from req (copyHeaders, fillUriInformation, body, method...)
        ODataResponse odResp = core.process(odReq);
        // TODO: write odResp into resp
    }
}