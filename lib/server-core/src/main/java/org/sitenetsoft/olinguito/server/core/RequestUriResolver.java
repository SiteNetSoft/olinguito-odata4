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

import org.sitenetsoft.olinguito.server.api.ODataRequest;

public final class RequestUriResolver {

    private static final String REQUESTMAPPING = "requestMapping";

    private RequestUriResolver() {}

    public static void fillUriInformation(
            final ODataRequest odRequest,
            final String requestUrl,
            final String requestUri,
            final String queryString,
            final String contextPath,
            final String servletPath,
            final Object requestMappingAttr,
            final int split) {

        String rawRequestUri = requestUrl;

        String rawServiceResolutionUri = null;
        String rawODataPath;

        // Spring controller case: requestMapping attribute provided by app
        if (requestMappingAttr != null) {
            String requestMapping = requestMappingAttr.toString();
            rawServiceResolutionUri = requestMapping;
            int beginIndex = rawRequestUri.indexOf(requestMapping) + requestMapping.length();
            rawODataPath = rawRequestUri.substring(beginIndex);

        } else if (servletPath != null && !"".equals(servletPath)) {
            int beginIndex = rawRequestUri.indexOf(servletPath) + servletPath.length();
            rawODataPath = rawRequestUri.substring(beginIndex);

        } else if (contextPath != null && !"".equals(contextPath)) {
            int beginIndex = rawRequestUri.indexOf(contextPath) + contextPath.length();
            rawODataPath = rawRequestUri.substring(beginIndex);

        } else {
            rawODataPath = requestUri;
        }

        if (split > 0) {
            rawServiceResolutionUri = rawODataPath;
            for (int i = 0; i < split; i++) {
                int index = rawODataPath.indexOf('/', 1);
                if (-1 == index) {
                    rawODataPath = "";
                    break;
                } else {
                    rawODataPath = rawODataPath.substring(index);
                }
            }
            int end = rawServiceResolutionUri.length() - rawODataPath.length();
            rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
        }

        String rawBaseUri = rawRequestUri.substring(0, rawRequestUri.length() - rawODataPath.length());

        odRequest.setRawQueryPath(queryString);
        odRequest.setRawRequestUri(rawRequestUri + (queryString == null ? "" : "?" + queryString));
        odRequest.setRawODataPath(rawODataPath);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
    }
}
