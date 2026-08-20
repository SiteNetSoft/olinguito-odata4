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
 * Copyright 2026 SiteNetSoft - Narrowed catch(Exception) to specific exception types
 * Copyright 2026 SiteNetSoft - Read CSDL JSON metadata in the client deserializer
 */
package org.sitenetsoft.olinguito.client.core.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.sitenetsoft.olinguito.client.api.data.ResWrap;
import org.sitenetsoft.olinguito.client.api.data.ServiceDocument;
import org.sitenetsoft.olinguito.client.api.edm.xml.Edmx;
import org.sitenetsoft.olinguito.client.api.edm.xml.XMLMetadata;
import org.sitenetsoft.olinguito.client.api.serialization.ClientODataDeserializer;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializer;
import org.sitenetsoft.olinguito.client.api.serialization.ODataDeserializerException;
import org.sitenetsoft.olinguito.client.core.data.JSONServiceDocumentDeserializer;
import org.sitenetsoft.olinguito.client.core.data.XMLServiceDocumentDeserializer;
import org.sitenetsoft.olinguito.client.core.edm.ClientCsdlXMLMetadata;
import org.sitenetsoft.olinguito.client.core.edm.xml.ClientCsdlEdmx;
import org.sitenetsoft.olinguito.client.core.edm.xml.ClientCsdlJsonMetadataParser;
import org.sitenetsoft.olinguito.commons.api.data.Delta;
import org.sitenetsoft.olinguito.commons.api.data.Entity;
import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.commons.api.data.Property;
import org.sitenetsoft.olinguito.commons.api.edm.EdmPrimitiveTypeException;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.ex.ODataError;
import org.sitenetsoft.olinguito.commons.api.format.ContentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;

public class ClientODataDeserializerImpl implements ClientODataDeserializer {

  private final ODataDeserializer deserializer;
  private final ContentType contentType;
  private static final String SCHEMA = "Schema";
  private static final String XMLNS = "xmlns";

  public ClientODataDeserializerImpl(final boolean serverMode, final ContentType contentType) {
    this.contentType = contentType;
    if (contentType.isCompatible(ContentType.APPLICATION_ATOM_SVC)
        || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)
        || contentType.isCompatible(ContentType.APPLICATION_XML)) {
      deserializer = new AtomDeserializer();
    } else {
      deserializer = new JsonDeserializer(serverMode);
    }
  }

  @Override
  public ResWrap<EntityCollection> toEntitySet(final InputStream input) throws ODataDeserializerException {
    return deserializer.toEntitySet(input);
  }

  @Override
  public ResWrap<Entity> toEntity(final InputStream input) throws ODataDeserializerException {
    return deserializer.toEntity(input);
  }

  @Override
  public ResWrap<Property> toProperty(final InputStream input) throws ODataDeserializerException {
    return deserializer.toProperty(input);
  }

  @Override
  public ODataError toError(final InputStream input) throws ODataDeserializerException {
    return deserializer.toError(input);
  }

  protected XmlMapper getXmlMapper() {
    final XmlMapper xmlMapper = new XmlMapper(
        new XmlFactory(new InputFactoryImpl(), new OutputFactoryImpl()), new JacksonXmlModule());

    xmlMapper.setInjectableValues(new InjectableValues.Std().addValue(Boolean.class, Boolean.FALSE));
    xmlMapper.enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL);
    
    xmlMapper.addHandler(new DeserializationProblemHandler() {
      @Override
      public boolean handleUnknownProperty(final DeserializationContext ctxt, final JsonParser jp,
          final com.fasterxml.jackson.databind.JsonDeserializer<?> deserializer,
          final Object beanOrClass, final String propertyName)
          throws IOException {

        // skip any unknown property
        ctxt.getParser().skipChildren();
        return true;
      }
    });
    return xmlMapper;
  }

  @Override
  public XMLMetadata toMetadata(final InputStream input) {
    try {
    	
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    	input.transferTo(byteArrayOutputStream);
    	// copy the content of input stream to reuse it
    	byte[] inputContent = byteArrayOutputStream.toByteArray();
    	
    	InputStream inputStream1 = new ByteArrayInputStream(inputContent);
    	Edmx edmx = getXmlMapper().readValue(inputStream1, ClientCsdlEdmx.class);
    	
    	InputStream inputStream2 = new ByteArrayInputStream(inputContent);
    	List<List<String>> schemaNameSpaces = getAllSchemaNameSpace(inputStream2);
 
      return new ClientCsdlXMLMetadata(edmx,schemaNameSpaces);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new IllegalArgumentException("Could not parse as Edmx document", e);
    }
  }

  /**
   * Reads the CSDL JSON representation of a metadata document, [OData-CSDLJSON] section 2.1, into the
   * object graph {@link #toMetadata(InputStream)} builds from the CSDL XML representation of the same
   * model.
   */
  @Override
  public XMLMetadata toJSONMetadata(final InputStream input) {
    return ClientCsdlJsonMetadataParser.parse(input);
  }

	private List<List<String>> getAllSchemaNameSpace(InputStream inputStream)
			throws ParserConfigurationException, SAXException, IOException{
		List<List<String>> schemaNameSpaces = new ArrayList <>();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setFeature(
	                "http://xml.org/sax/features/namespaces", true);
		dbFactory.setFeature(
	                "http://apache.org/xml/features/validation/schema",
	                false);
		dbFactory.setFeature(
	                "http://apache.org/xml/features/validation/schema-full-checking",
	                true);
		dbFactory.setFeature(
	                "http://xml.org/sax/features/external-general-entities",
	                false);
		dbFactory.setFeature(
	                "http://xml.org/sax/features/external-parameter-entities",
	                false);
		dbFactory.setFeature(
	                "http://apache.org/xml/features/disallow-doctype-decl",
	                true);
		dbFactory.setFeature(
	                "http://javax.xml.XMLConstants/feature/secure-processing",
	                true);
		
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputStream);
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName(SCHEMA);
		
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
            List<String> nameSpaces = getNameSpaces(nNode);
            schemaNameSpaces.add(nameSpaces);
		}
	return schemaNameSpaces;
	}

    private static List<String> getNameSpaces(Node nNode) {
        List<String> nameSpaces = new ArrayList <>();
        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            Element eElement = (Element) nNode;
            NamedNodeMap attributes = eElement.getAttributes();
            int len = attributes.getLength();
            for(int i =0;i<len;i++){
                // check for all atributes begining with name xmlns or xmlns:
                String attrName = attributes.item(i).getNodeName();
                if( XMLNS.equals(attrName) || attrName.startsWith(XMLNS+":")){
                    nameSpaces.add(attributes.item(i).getNodeValue());
                }
            }
        }
        return nameSpaces;
    }

    @Override
  public ResWrap<ServiceDocument> toServiceDocument(final InputStream input) throws ODataDeserializerException {
    return contentType.isCompatible(ContentType.APPLICATION_XML) ?
        new XMLServiceDocumentDeserializer(false).toServiceDocument(input) :
        new JSONServiceDocumentDeserializer(false).toServiceDocument(input);
  }

  @Override
  public ResWrap<Delta> toDelta(final InputStream input) throws ODataDeserializerException {
    try {
      return contentType.isCompatible(ContentType.APPLICATION_ATOM_SVC)
          || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML) ?
          new AtomDeserializer().delta(input) :
          new JsonDeltaDeserializer(false).toDelta(input);
    } catch (final XMLStreamException | EdmPrimitiveTypeException e) {
      throw new ODataDeserializerException(e);
    }
  }

    @Override
    public List<CsdlSchema> fetchTermDefinitionSchema(List<InputStream> input) {
        List<CsdlSchema> schemas = new ArrayList<>();
        try {
            int i = 0;
            for (InputStream stream : input) {
                if (stream == null) {
                    throw new IllegalArgumentException("Term definition InputStream is null at index " + i);
                }
                ClientCsdlEdmx edmx = getXmlMapper().readValue(stream, ClientCsdlEdmx.class);
                schemas.addAll(edmx.getDataServices().getSchemas());
                i++;
            }
            return schemas;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse Term definition", e);
        }
    }
}
