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
package myservice.mynamespace.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import myservice.mynamespace.data.Storage;

import org.sitenetsoft.olinguito.commons.api.data.ContextURL;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.EntityIterator;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.EdmEntityType;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.server.api.OData;
import org.sitenetsoft.olinguito.server.api.ODataApplicationException;
import org.sitenetsoft.olinguito.server.api.ODataContentWriteErrorCallback;
import org.sitenetsoft.olinguito.server.api.ODataContentWriteErrorContext;
import org.sitenetsoft.olinguito.server.api.ODataRequest;
import org.sitenetsoft.olinguito.server.api.ODataResponse;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;
import org.sitenetsoft.olinguito.server.api.processor.EntityCollectionProcessor;
import org.sitenetsoft.olinguito.server.api.serializer.EntityCollectionSerializerOptions;
import org.sitenetsoft.olinguito.server.api.serializer.ODataSerializer;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerException;
import org.sitenetsoft.olinguito.server.api.serializer.SerializerStreamResult;
import org.sitenetsoft.olinguito.server.api.uri.UriInfo;
import org.sitenetsoft.olinguito.server.api.uri.UriResource;
import org.sitenetsoft.olinguito.server.api.uri.UriResourceEntitySet;

public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

	private OData odata;
	private ServiceMetadata serviceMetadata;
	private Storage storage;

	public DemoEntityCollectionProcessor(Storage storage) {
		this.storage = storage;
	}

	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, SerializerException {

		// 1st retrieve the requested EntitySet from the uriInfo (representation of the parsed URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName and deliver as EntitySet
		EntityIterator iterator = storage.readEntitySetDataStreamed(edmEntitySet);

		// 3rd: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// and serialize the content: transform from the EntitySet object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts =
				EntityCollectionSerializerOptions.with().id(id)
						.writeContentErrorCallback(errorCallback)
						.contextURL(contextUrl).build();

		SerializerStreamResult serializerResult = serializer.entityCollectionStreamed(serviceMetadata,
				edmEntityType, iterator, opts);

		// 4th: configure the response object: set the body, headers and status code
		response.setODataContent(serializerResult.getODataContent());
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}


	private ODataContentWriteErrorCallback errorCallback = new ODataContentWriteErrorCallback() {
		public void handleError(ODataContentWriteErrorContext context, WritableByteChannel channel) {
			String message = "An error occurred with message: ";
			if(context.getException() != null) {
				message += context.getException().getMessage();
			}
			try {
				channel.write(ByteBuffer.wrap(message.getBytes()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
}
