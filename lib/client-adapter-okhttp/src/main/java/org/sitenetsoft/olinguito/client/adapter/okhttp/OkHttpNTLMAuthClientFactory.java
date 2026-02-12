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
 * Copyright 2026 SiteNetSoft - OkHttp NTLM Auth client factory
 */
package org.sitenetsoft.olinguito.client.adapter.okhttp;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * {@link OkHttpClientFactory} that adds NTLM Authentication via OkHttp's {@link Authenticator}.
 * <p>
 * Uses the JDK's built-in NTLM support through {@link java.net.Authenticator}.
 * The JDK authenticator is set for the scope of the client builder; it handles the
 * NTLM challenge-response handshake transparently.
 */
public class OkHttpNTLMAuthClientFactory extends OkHttpClientFactory {

  private final String username;
  private final String password;
  private final String workstation;
  private final String domain;

  public OkHttpNTLMAuthClientFactory(final String username, final String password,
      final String workstation, final String domain) {
    this.username = username;
    this.password = password;
    this.workstation = workstation;
    this.domain = domain;
  }

  @Override
  protected void configureBuilder(final OkHttpClient.Builder builder) {
    super.configureBuilder(builder);
    builder.authenticator(new NTLMAuthenticator());
  }

  /**
   * OkHttp {@link Authenticator} that responds to NTLM (and Negotiate) challenges
   * using the configured credentials via the JDK's built-in NTLM engine.
   */
  private class NTLMAuthenticator implements Authenticator {

    @Override
    public Request authenticate(final Route route, final Response response) {
      if (response.request().header("Authorization") != null) {
        // Already attempted auth -- give up to avoid infinite loop
        return null;
      }

      for (String challenge : response.challenges().stream()
          .map(c -> c.scheme().toUpperCase())
          .toList()) {
        if ("NTLM".equals(challenge) || "NEGOTIATE".equals(challenge)) {
          java.net.Authenticator.setDefault(new java.net.Authenticator() {
            @Override
            protected java.net.PasswordAuthentication getPasswordAuthentication() {
              final String user = domain != null && !domain.isEmpty()
                  ? domain + "\\" + username : username;
              return new java.net.PasswordAuthentication(
                  user, password.toCharArray());
            }
          });
          return response.request();
        }
      }

      return null;
    }
  }
}
