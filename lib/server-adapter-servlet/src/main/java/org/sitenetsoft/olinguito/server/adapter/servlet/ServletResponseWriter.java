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
package org.sitenetsoft.olinguito.server.adapter.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map.Entry;

import jakarta.servlet.http.HttpServletResponse;
import org.sitenetsoft.olinguito.commons.api.ex.ODataRuntimeException;
import org.sitenetsoft.olinguito.server.api.ODataContent;
import org.sitenetsoft.olinguito.server.api.ODataResponse;

public final class ServletResponseWriter {

    private static final int COPY_BUFFER_SIZE = 8192;

    private ServletResponseWriter() {}

    public static void write(HttpServletResponse response, ODataResponse odResponse) {
        response.setStatus(odResponse.getStatusCode());

        for (Entry<String, List<String>> entry : odResponse.getAllHeaders().entrySet()) {
            for (String headerValue : entry.getValue()) {
                response.addHeader(entry.getKey(), headerValue);
            }
        }

        if (odResponse.getContent() != null) {
            copyContent(odResponse.getContent(), response);
        } else if (odResponse.getODataContent() != null) {
            writeContent(odResponse, response);
        }
    }

    private static void writeContent(final ODataResponse odataResponse, final HttpServletResponse servletResponse) {
        try {
            ODataContent res = odataResponse.getODataContent();
            res.write(Channels.newChannel(servletResponse.getOutputStream()));
        } catch (IOException e) {
            throw new ODataRuntimeException("Error on reading request content", e);
        }
    }

    private static void copyContent(final InputStream inputStream, final HttpServletResponse servletResponse) {
        copyContent(Channels.newChannel(inputStream), servletResponse);
    }

    private static void copyContent(final ReadableByteChannel input, final HttpServletResponse servletResponse) {
        try (WritableByteChannel output = Channels.newChannel(servletResponse.getOutputStream())) {
            ByteBuffer inBuffer = ByteBuffer.allocate(COPY_BUFFER_SIZE);
            while (input.read(inBuffer) > 0) {
                inBuffer.flip();
                output.write(inBuffer);
                inBuffer.clear();
            }
        } catch (IOException e) {
            throw new ODataRuntimeException("Error on reading request content", e);
        } finally {
            closeStream(input);
        }
    }

    private static void closeStream(final Channel closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
