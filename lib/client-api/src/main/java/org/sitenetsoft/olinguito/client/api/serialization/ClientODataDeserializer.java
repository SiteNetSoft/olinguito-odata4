/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Read CSDL JSON metadata in the client deserializer
 */
package org.sitenetsoft.olinguito.client.api.serialization;

import java.io.InputStream;
import java.util.List;

import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.data.ServiceDocument;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.commons.api.data.Delta;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;

public interface ClientODataDeserializer extends ODataDeserializer {

  XMLMetadata toMetadata(InputStream input);

  /**
   * Gets the {@link XMLMetadata} object represented by the given CSDL JSON metadata document, as
   * defined by [OData-CSDLJSON] section 2.1 - the JSON representation of a metadata document, which a
   * service returns for {@code $metadata} when {@code application/json} is requested.
   * <p>
   * The resulting object graph is the one the CSDL XML representation produces, so the same
   * {@code ODataReader#readMetadata(java.util.Map)} builds the {@code Edm} from either representation.
   * <p>
   * The default implementation is unsupported, so deserializers outside this library keep compiling.
   *
   * @param input stream containing a CSDL JSON metadata document
   * @return the metadata of the document
   * @throws UnsupportedOperationException if this deserializer cannot read CSDL JSON metadata
   */
  default XMLMetadata toJSONMetadata(InputStream input) {
    throw new UnsupportedOperationException("This deserializer cannot read CSDL JSON metadata");
  }
  
  /**
   * Gets all the terms defined in the given input stream
   * @param input
   * @return
   */
  List<CsdlSchema> fetchTermDefinitionSchema(List<InputStream> input);

  /**
   * Gets the ServiceDocument object represented by the given InputStream.
   *
   * @param input stream to be de-serialized.
   * @return <tt>ServiceDocument</tt> object.
   * @throws ODataDeserializerException
   */
  ResWrap<ServiceDocument> toServiceDocument(InputStream input) throws ODataDeserializerException;
  
  /**
   * Gets a delta object from the given InputStream.
   *
   * @param input stream to be de-serialized.
   * @return {@link Delta} instance.
   * @throws ODataDeserializerException
   */
  ResWrap<Delta> toDelta(InputStream input) throws ODataDeserializerException;
}
