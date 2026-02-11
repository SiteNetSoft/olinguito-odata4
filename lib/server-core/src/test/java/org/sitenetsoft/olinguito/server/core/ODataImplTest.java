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
package org.sitenetsoft.olinguito.server.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.deserializer.DeserializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ODataImplTest {

  private final OData odata = OData.newInstance();

  @Test
  public void serializerSupportedFormats() throws SerializerException {
    assertNotNull(odata.createSerializer(ContentType.JSON_NO_METADATA));
    assertNotNull(odata.createSerializer(ContentType.JSON));
    assertNotNull(odata.createSerializer(ContentType.APPLICATION_JSON));
    assertNotNull(odata.createSerializer(ContentType.JSON_FULL_METADATA));
    List<String> versions = new ArrayList<String>();
    versions.add("4.01");
    assertNotNull(odata.createSerializer(ContentType.JSON_FULL_METADATA, versions));
  }

  @Test
  public void deserializerSupportedFormats() throws DeserializerException {
    assertNotNull(odata.createDeserializer(ContentType.JSON_NO_METADATA));
    assertNotNull(odata.createDeserializer(ContentType.JSON));
    assertNotNull(odata.createDeserializer(ContentType.JSON_FULL_METADATA));
    assertNotNull(odata.createDeserializer(ContentType.APPLICATION_JSON));
    List<String> versions = new ArrayList<String>();
    versions.add("4.01");
    assertNotNull(odata.createDeserializer(ContentType.APPLICATION_JSON, versions));
  }

  public void xmlDeserializer() throws DeserializerException {
    assertNotNull(odata.createDeserializer(ContentType.APPLICATION_XML));
  }
  
  @Test
  public void deserializerWithoutContentType() throws DeserializerException {
      assertThrows(DeserializerException.class, () -> odata.createDeserializer(null));
  }
  
  @Test
  public void deserializerWithoutContentTypeAndWithVersions() throws DeserializerException {
      assertThrows(DeserializerException.class, () -> {
          List<String> versions = new ArrayList<String>();
          versions.add("4.01");
          odata.createDeserializer(null, versions);
      });
  }
  
  @Test
  public void deltaSerializer() throws SerializerException {
      assertThrows(SerializerException.class, () -> {
          List<String> versions = new ArrayList<String>();
          versions.add("4.01");
          odata.createEdmDeltaSerializer(null, versions);
      });
  }
  
  @Test
  public void edmAssitedSerializer() throws SerializerException {
      assertThrows(SerializerException.class, () -> odata.createEdmAssistedSerializer(null));
  }
  
  @Test
  public void deserializer1() throws DeserializerException {
      assertThrows(DeserializerException.class, () -> {
          List<String> versions = new ArrayList<String>();
          versions.add("4.01");
          odata.createDeserializer(null, null, versions);
      });
  }
  
  @Test
  public void deserializer2() throws DeserializerException {
      assertThrows(DeserializerException.class,
          () -> odata.createDeserializer(null, Mockito.mock(ServiceMetadata.class)));
  }
  
  @Test
  public void serializerWithVersions() throws SerializerException {
      assertThrows(SerializerException.class, () -> {
          List<String> versions = new ArrayList<String>();
          versions.add("4.01");
          odata.createSerializer(null, versions);
      });
  }
  
  @Test
  public void serializer() throws SerializerException {
      assertThrows(SerializerException.class, () -> odata.createSerializer(null));
  }
  
  @Test
  public void edmAssistedSerializerWithVersion() throws SerializerException {
	  List<String> versions = new ArrayList<String>();
	  versions.add("4.01");
	  assertNotNull(odata.createEdmAssistedSerializer(ContentType.APPLICATION_JSON, versions));
	  
	  versions.add("5");
	  assertNotNull(odata.createEdmAssistedSerializer(ContentType.APPLICATION_JSON, versions));
  }
}
