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
package org.sitenetsoft.olinguito.fit.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.UUID;

import org.sitenetsoft.olinguito.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.sitenetsoft.olinguito.client.api.communication.request.cud.UpdateType;
import org.sitenetsoft.olinguito.client.api.communication.response.ODataEntityUpdateResponse;
import org.sitenetsoft.olinguito.client.api.domain.ClientEntity;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.junit.Test;

public class EntityUpdateTestITCase extends AbstractTestITCase {

  private void onContained(final ContentType contentType) {
    final String newName = UUID.randomUUID().toString();
    final ClientEntity changes = getClient().getObjectFactory().newEntity(
        new FullQualifiedName("Microsoft.Test.OData.Services.ODataWCFService.PaymentInstrument"));
    changes.getProperties().add(getClient().getObjectFactory().newPrimitiveProperty("FriendlyName",
        getClient().getObjectFactory().newPrimitiveValueBuilder().buildString(newName)));

    final URI uri = getClient().newURIBuilder(testStaticServiceRootURL).
        appendEntitySetSegment("Accounts").appendKeySegment(101).
        appendNavigationSegment("MyPaymentInstruments").appendKeySegment(101901).build();
    final ODataEntityUpdateRequest<ClientEntity> req = getClient().getCUDRequestFactory().
        getEntityUpdateRequest(uri, UpdateType.PATCH, changes);
    req.setFormat(contentType);

    final ODataEntityUpdateResponse<ClientEntity> res = req.execute();
    assertEquals(204, res.getStatusCode());

    final ClientEntity actual = getClient().getRetrieveRequestFactory().getEntityRequest(uri).execute().getBody();
    assertNotNull(actual);
    assertEquals(newName, actual.getProperty("FriendlyName").getPrimitiveValue().toString());
  }

  @Test
  public void atomOnContained() {
    onContained(ContentType.APPLICATION_ATOM_XML);
  }

  @Test
  public void jsonOnContained() {
    onContained(ContentType.JSON);
  }
}
