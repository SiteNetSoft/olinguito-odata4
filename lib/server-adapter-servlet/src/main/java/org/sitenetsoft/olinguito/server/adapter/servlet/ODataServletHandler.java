package org.sitenetsoft.olinguito.server.adapter.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ODataServletHandler {
    void process(HttpServletRequest request, HttpServletResponse response);
    void setSplit(int split);
}