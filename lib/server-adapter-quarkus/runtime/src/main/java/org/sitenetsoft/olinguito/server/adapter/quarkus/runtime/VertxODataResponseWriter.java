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
 */
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

import org.sitenetsoft.olinguito.server.api.ODataContent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Utility class for writing OData responses to Vert.x HTTP responses.
 */
public final class VertxODataResponseWriter {

    private static final int COPY_BUFFER_SIZE = 8192;

    private VertxODataResponseWriter() {
    }

    /**
     * Write an InputStream to the Vert.x response.
     */
    public static void writeInputStream(HttpServerResponse response, InputStream inputStream) {
        try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
            ByteBuffer buffer = ByteBuffer.allocate(COPY_BUFFER_SIZE);
            Buffer vertxBuffer = Buffer.buffer();

            while (channel.read(buffer) > 0) {
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                vertxBuffer.appendBytes(bytes);
                buffer.clear();
            }

            response.end(vertxBuffer);
        } catch (IOException e) {
            throw new RuntimeException("Error writing response content", e);
        }
    }

    /**
     * Write ODataContent to the Vert.x response.
     * ODataContent supports streaming via WritableByteChannel.
     */
    public static void writeODataContent(HttpServerResponse response, ODataContent content) {
        try {
            // Create a ByteArrayOutputStream to capture the ODataContent output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (WritableByteChannel channel = Channels.newChannel(baos)) {
                // Write ODataContent to the channel
                content.write(channel);
            }

            // Send the captured content to Vert.x response
            byte[] bytes = baos.toByteArray();
            response.end(Buffer.buffer(bytes));
        } catch (IOException e) {
            throw new RuntimeException("Error writing OData content", e);
        }
    }
}
