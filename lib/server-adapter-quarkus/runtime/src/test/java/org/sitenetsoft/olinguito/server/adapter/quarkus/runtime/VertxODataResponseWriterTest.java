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
package org.sitenetsoft.olinguito.server.adapter.quarkus.runtime;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sitenetsoft.olinguito.server.api.ODataContent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link VertxODataResponseWriter}.
 */
class VertxODataResponseWriterTest {

    private HttpServerResponse response;

    @BeforeEach
    void setUp() {
        response = mock(HttpServerResponse.class);
    }

    @Test
    void testWriteInputStream() {
        String content = "Hello OData World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        VertxODataResponseWriter.writeInputStream(response, inputStream);

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).end(bufferCaptor.capture());

        Buffer capturedBuffer = bufferCaptor.getValue();
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), capturedBuffer.getBytes());
    }

    @Test
    void testWriteInputStreamEmpty() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        VertxODataResponseWriter.writeInputStream(response, inputStream);

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).end(bufferCaptor.capture());

        Buffer capturedBuffer = bufferCaptor.getValue();
        assertArrayEquals(new byte[0], capturedBuffer.getBytes());
    }

    @Test
    void testWriteInputStreamLargeContent() {
        // Create content larger than the buffer size (8192 bytes)
        byte[] largeContent = new byte[20000];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        InputStream inputStream = new ByteArrayInputStream(largeContent);

        VertxODataResponseWriter.writeInputStream(response, inputStream);

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).end(bufferCaptor.capture());

        Buffer capturedBuffer = bufferCaptor.getValue();
        assertArrayEquals(largeContent, capturedBuffer.getBytes());
    }

    @Test
    void testWriteODataContent() {
        String content = "OData Content Response";
        ODataContent odataContent = new TestODataContent(content.getBytes(StandardCharsets.UTF_8));

        VertxODataResponseWriter.writeODataContent(response, odataContent);

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).end(bufferCaptor.capture());

        Buffer capturedBuffer = bufferCaptor.getValue();
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), capturedBuffer.getBytes());
    }

    @Test
    void testWriteODataContentEmpty() {
        ODataContent odataContent = new TestODataContent(new byte[0]);

        VertxODataResponseWriter.writeODataContent(response, odataContent);

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).end(bufferCaptor.capture());

        Buffer capturedBuffer = bufferCaptor.getValue();
        assertArrayEquals(new byte[0], capturedBuffer.getBytes());
    }

    @Test
    void testWriteODataContentWithIOException() {
        ODataContent odataContent = new ODataContent() {
            @Override
            public void write(WritableByteChannel channel) {
                throw new RuntimeException(new IOException("Simulated IO error"));
            }

            @Override
            public void write(OutputStream stream) {
                throw new RuntimeException(new IOException("Simulated IO error"));
            }
        };

        assertThrows(RuntimeException.class, () ->
                VertxODataResponseWriter.writeODataContent(response, odataContent));
    }

    /**
     * Test implementation of ODataContent.
     */
    private static class TestODataContent implements ODataContent {
        private final byte[] data;

        TestODataContent(byte[] data) {
            this.data = data;
        }

        @Override
        public void write(WritableByteChannel channel) {
            try {
                channel.write(ByteBuffer.wrap(data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(OutputStream stream) {
            try {
                WritableByteChannel channel = Channels.newChannel(stream);
                channel.write(ByteBuffer.wrap(data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
