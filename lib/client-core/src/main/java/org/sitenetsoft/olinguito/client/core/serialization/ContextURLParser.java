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
 * Copyright 2026 SiteNetSoft - Code quality improvements
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.client.core.StringHelper;
import org.sitenetsoft.olinguito.commons.api.Constants;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.ContextURL.Suffix;

public class ContextURLParser {

  public static ContextURL parse(final URI contextURL) {
    if (contextURL == null) {
      return null;
    }

    final ContextURL.Builder contextUrl = ContextURL.with();

    String contextURLasString = contextURL.toASCIIString();

    boolean isEntity = false;
    if (contextURLasString.endsWith("/$entity") || contextURLasString.endsWith("/@Element")) {
      isEntity = true;
      contextUrl.suffix(Suffix.ENTITY);
      contextURLasString = contextURLasString.replace("/$entity", "").
          replace("/@Element", "");
    } else if (contextURLasString.endsWith("/$ref")) {
      contextUrl.suffix(Suffix.REFERENCE);
      contextURLasString = contextURLasString.replace("/$ref", "");
    } else if (contextURLasString.endsWith("/$delta")) {
      contextUrl.suffix(Suffix.DELTA);
      contextURLasString = contextURLasString.replace("/$delta", "");
    } else if (contextURLasString.endsWith("/$deletedEntity")) {
      contextUrl.suffix(Suffix.DELTA_DELETED_ENTITY);
      contextURLasString = contextURLasString.replace("/$deletedEntity", "");
    } else if (contextURLasString.endsWith("/$link")) {
      contextUrl.suffix(Suffix.DELTA_LINK);
      contextURLasString = contextURLasString.replace("/$link", "");
    } else if (contextURLasString.endsWith("/$deletedLink")) {
      contextUrl.suffix(Suffix.DELTA_DELETED_LINK);
      contextURLasString = contextURLasString.replace("/$deletedLink", "");
    }

    contextUrl.serviceRoot(URI.create(StringHelper.substringBefore(contextURLasString, Constants.METADATA)));

    final String rest = StringHelper.substringAfter(contextURLasString, Constants.METADATA + "#");

    String firstToken;
    String entitySetOrSingletonOrType;
    if (rest.startsWith("Collection(")) {
      firstToken = rest.substring(0, rest.indexOf(')') + 1);
      entitySetOrSingletonOrType = firstToken;
    } else {
      final int openParIdx = rest.indexOf('(');
      if (openParIdx == -1) {
        firstToken = StringHelper.substringBeforeLast(rest, "/");

        entitySetOrSingletonOrType = firstToken;
      } else {
        firstToken = isEntity ? rest : StringHelper.substringBeforeLast(rest, ")") + ")";

        final List<String> parts = new ArrayList<>();
        for (String split : firstToken.split("\\)/")) {
          parts.add(split.replaceAll("\\(.*", ""));
        }
        entitySetOrSingletonOrType = String.join("/", parts);
        final int commaIdx = firstToken.indexOf(',');
        if (commaIdx != -1) {
          contextUrl.selectList(firstToken.substring(openParIdx + 1, firstToken.length() - 1));
        }
      }
    }
    contextUrl.entitySetOrSingletonOrType(entitySetOrSingletonOrType);

    final int slashIdx = entitySetOrSingletonOrType.lastIndexOf('/');
    if (slashIdx != -1 && entitySetOrSingletonOrType.substring(slashIdx + 1).indexOf('.') != -1) {
      contextUrl.entitySetOrSingletonOrType(entitySetOrSingletonOrType.substring(0, slashIdx));
      contextUrl.derivedEntity(entitySetOrSingletonOrType.substring(slashIdx + 1));
    }

    if (!firstToken.equals(rest)) {
      final String[] pathElems = StringHelper.substringAfter(rest, "/").split("/");
      if (pathElems.length > 0 && !pathElems[0].isEmpty()) {
        if (pathElems[0].indexOf('.') == -1) {
          contextUrl.navOrPropertyPath(pathElems[0]);
        } else {
          contextUrl.derivedEntity(pathElems[0]);
        }

        if (pathElems.length > 1) {
          contextUrl.navOrPropertyPath(pathElems[1]);
        }
      }
    }

    return contextUrl.build();
  }
}
