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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages
 */
package org.sitenetsoft.olinguito.fit.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.sitenetsoft.olinguito.client.api.EdmEnabledODataClient;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.ext.proxy.api.EdmStreamValue;
import org.sitenetsoft.olinguito.fit.proxy.demo.Service;
import org.sitenetsoft.olinguito.fit.proxy.demo.odatademo.DemoService;
import org.sitenetsoft.olinguito.fit.proxy.demo.odatademo.types.Advertisement;
import org.junit.Test;

/**
 * This is the unit test class to check media entity retrieve operations.
 */
public class MediaEntityTestITCase extends AbstractTestITCase {

  private Service<EdmEnabledODataClient> ecf;

  private DemoService ime;

  protected Service<EdmEnabledODataClient> getService() {
    if (ecf == null) {
      ecf = Service.getV4(testDemoServiceRootURL);
      ecf.getClient().getConfiguration().setDefaultBatchAcceptFormat(ContentType.APPLICATION_OCTET_STREAM);
    }
    return ecf;
  }

  protected DemoService getContainer() {
    if (ime == null) {
      ime = getService().getEntityContainer(DemoService.class);
    }
    return ime;
  }

  @Test
  public void read() throws IOException {
    final UUID uuid = UUID.fromString("f89dee73-af9f-4cd4-b330-db93c25ff3c7");

    final Advertisement adv = getContainer().getAdvertisements().getByKey(uuid);
    assertNull(adv.getAirDate()); // No HTTP request --> property null

    final EdmStreamValue res = adv.loadStream();
    assertEquals("application/octet-stream", res.getContentType());
    assertNotNull(res.getStream());
    IOUtils.closeQuietly(res.getStream());

    getService().getContext().detachAll();
  }

  @Test
  public void update() throws IOException {
    final UUID uuid = UUID.fromString("f89dee73-af9f-4cd4-b330-db93c25ff3c7");
    final Advertisement adv = getContainer().getAdvertisements().getByKey(uuid);
    final String random = RandomStringUtils.secure().next(124, "abcdefghijklmnopqrstuvwxyz");
    adv.uploadStream(getContainer().newEdmStreamValue("application/octet-stream", IOUtils.toInputStream(random, StandardCharsets.UTF_8)));
    getContainer().flush();
    assertEquals(random,
        IOUtils.toString(getContainer().getAdvertisements().getByKey(uuid).loadStream().getStream(), StandardCharsets.UTF_8));
    getService().getContext().detachAll();
  }

  @Test
  public void create() throws IOException {
    final String random = RandomStringUtils.secure().next(124, "abcdefghijklmnopqrstuvwxyz");

    final Advertisement adv = getContainer().newEntityInstance(Advertisement.class);
    adv.uploadStream(getContainer().newEdmStreamValue("application/octet-stream", IOUtils.toInputStream(random, StandardCharsets.UTF_8)));
    adv.setAirDate(new Timestamp(Calendar.getInstance().getTimeInMillis()));

    getContainer().getAdvertisements().add(adv);
    getContainer().flush();

    final UUID uuid = adv.getID();
    getService().getContext().detachAll();

    assertEquals(random, IOUtils.toString(getContainer().getAdvertisements().getByKey(uuid).loadStream().getStream(), StandardCharsets.UTF_8));

    getService().getContext().detachAll();

    getContainer().getAdvertisements().delete(uuid);
    getContainer().flush();

    try {
      getContainer().getAdvertisements().getByKey(uuid).load();
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
    getService().getContext().detachAll();
  }
}
