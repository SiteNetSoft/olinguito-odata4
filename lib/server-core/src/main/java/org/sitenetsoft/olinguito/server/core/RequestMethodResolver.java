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
package org.sitenetsoft.olinguito.server.core;

import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.server.api.ODataLibraryException;

public final class RequestMethodResolver {

    private RequestMethodResolver() {}

    public static HttpMethod resolve(
            final String method,
            final String xHttpMethod,
            final String xHttpMethodOverride) throws ODataLibraryException {

        final HttpMethod httpRequestMethod;
        try {
            httpRequestMethod = HttpMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new ODataHandlerException(
                    "HTTP method not allowed " + method, e,
                    ODataHandlerException.MessageKeys.HTTP_METHOD_NOT_ALLOWED, method);
        }

        // Only POST can be overridden via headers
        if (httpRequestMethod != HttpMethod.POST) {
            return httpRequestMethod;
        }

        try {
            if (xHttpMethod == null && xHttpMethodOverride == null) {
                return httpRequestMethod;
            } else if (xHttpMethod == null) {
                return HttpMethod.valueOf(xHttpMethodOverride);
            } else if (xHttpMethodOverride == null) {
                return HttpMethod.valueOf(xHttpMethod);
            } else {
                if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
                    throw new ODataHandlerException(
                            "Ambiguous X-HTTP-Methods",
                            ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD,
                            xHttpMethod, xHttpMethodOverride);
                }
                return HttpMethod.valueOf(xHttpMethod);
            }
        } catch (IllegalArgumentException e) {
            throw new ODataHandlerException(
                    "Invalid HTTP method " + method, e,
                    ODataHandlerException.MessageKeys.INVALID_HTTP_METHOD, method);
        }
    }

    /** Convenience for adapters that have a header map abstraction. */
    public static HttpMethod resolveFromHeaders(
            final String method,
            final java.util.function.Function<String, String> headerGetter) throws ODataLibraryException {

        return resolve(
                method,
                headerGetter.apply(HttpHeader.X_HTTP_METHOD),
                headerGetter.apply(HttpHeader.X_HTTP_METHOD_OVERRIDE));
    }
}
