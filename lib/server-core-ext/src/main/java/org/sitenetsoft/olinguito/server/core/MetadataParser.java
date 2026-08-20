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
 * Copyright 2026 SiteNetSoft - Replaced Arrays.asList with List.of/Set.of
 * Copyright 2026 SiteNetSoft - Removed unnecessary boxing and modernized length checks
 * Copyright 2026 SiteNetSoft - Cached compiled regex pattern for split()
 * Copyright 2026 SiteNetSoft - OLINGO-1466/1520: Handle Scale="variable"
 * Copyright 2026 SiteNetSoft - Shared the reference loader, accepted 4.01 and fixed Nullable defaults
 * Copyright 2026 SiteNetSoft - Exposed the default reference resolver to the CSDL JSON parser
 * Copyright 2026 SiteNetSoft - Kept the Qualifier of an annotation, which was silently dropped
 * Copyright 2026 SiteNetSoft - Kept the cause message when wrapping a failure in an XMLStreamException
 */
package org.sitenetsoft.olinguito.server.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.sitenetsoft.olinguito.commons.api.edm.EdmException;
import org.sitenetsoft.olinguito.commons.api.edm.FullQualifiedName;
import org.sitenetsoft.olinguito.commons.api.edm.geo.SRID;
import org.sitenetsoft.olinguito.commons.api.edm.constants.ODataServiceVersion;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlActionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotatable;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlAnnotations;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlBindingTarget;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlComplexType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityContainer;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntitySet;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEntityType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumMember;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlEnumType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlFunctionImport;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDelete;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOnDeleteAction;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlOperation;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlParameter;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlProperty;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlPropertyRef;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReferentialConstraint;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlReturnType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSchema;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlSingleton;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTerm;
import org.sitenetsoft.olinguito.commons.api.edm.provider.CsdlTypeDefinition;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlAnnotationPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlApply;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCast;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlCollection;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlExpression;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlIsOf;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElement;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlLabeledElementReference;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNavigationPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlNull;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlRecord;
import org.sitenetsoft.olinguito.commons.api.edm.provider.annotation.CsdlUrlRef;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReference;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceInclude;
import org.sitenetsoft.olinguito.commons.api.edmx.EdmxReferenceIncludeAnnotation;
import org.sitenetsoft.olinguito.commons.api.ex.ODataException;
import org.sitenetsoft.olinguito.server.api.ServiceMetadata;

/**
 * This class can convert a CSDL document into EDMProvider object
 */
public class MetadataParser {
  private boolean parseAnnotations = false;
  private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");
  private static final String XML_LINK_NS = "http://www.w3.org/1999/xlink";
  private ReferenceResolver referenceResolver = new DefaultReferenceResolver();
  private boolean useLocalCoreVocabularies = true;
  private boolean implicitlyLoadCoreVocabularies = false;
  private boolean recursivelyLoadReferences = false;
  private final Map<String, SchemaBasedEdmProvider> globalReferenceMap;
  private final ReferenceLoader referenceLoader;

  public MetadataParser() {
    this(new HashMap<>());
  }

  MetadataParser(final Map<String, SchemaBasedEdmProvider> globalReferenceMap) {
    this.globalReferenceMap = globalReferenceMap;
    this.referenceLoader = new ReferenceLoader(globalReferenceMap, this::buildEdmProviderFromStream);
  }

  private SchemaBasedEdmProvider buildEdmProviderFromStream(final InputStream csdl, final ReferenceResolver resolver,
      final boolean loadCore, final boolean useLocal, final boolean loadReferenceSchemas, final String namespace)
      throws ODataException {
    try {
      return buildEdmProvider(csdl, resolver, loadCore, useLocal, loadReferenceSchemas, namespace);
    } catch (XMLStreamException e) {
      throw new ODataException(e.getMessage(), e);
    }
  }
  
  /**
   * Avoid reading the annotations in the $metadata 
   * @param parse
   * @return
   */
  public MetadataParser parseAnnotations(boolean parse) {
    this.parseAnnotations = parse;
    return this;
  }

  /**
   * Externalize the reference loading, such that they can be loaded from local caches
   * @param resolver
   * @return
   */
  public MetadataParser referenceResolver(ReferenceResolver resolver) {
    this.referenceResolver = resolver;
    return this;
  }
  
  /**
   * Load the core libraries from local classpath
   * @param load true for yes; false otherwise
   * @return
   */
  public MetadataParser useLocalCoreVocabularies(boolean load) {
    this.useLocalCoreVocabularies = load;
    return this;
  }
  
  /**
   * Load the core libraries from local classpath
   * @param load true for yes; false otherwise
   * @return
   */
  public MetadataParser recursivelyLoadReferences(boolean load) {
    this.recursivelyLoadReferences = load;
    return this;
  }  
  
  /**
   * Load the core vocabularies, irrespective of if they are defined in the $metadata
   * @param load
   * @return
   */
  public MetadataParser implicitlyLoadCoreVocabularies(boolean load) {
    this.implicitlyLoadCoreVocabularies = load;
    return this;
  }
  
  public ServiceMetadata buildServiceMetadata(Reader csdl) throws XMLStreamException {
    SchemaBasedEdmProvider provider = buildEdmProvider(csdl, this.referenceResolver,
            this.implicitlyLoadCoreVocabularies, this.useLocalCoreVocabularies, true, null);
    return new ServiceMetadataImpl(provider, provider.getReferences(), null);
  }

  public SchemaBasedEdmProvider buildEdmProvider(Reader csdl) throws XMLStreamException {
    XMLInputFactory xmlInputFactory = createXmlInputFactory();
    XMLEventReader reader = xmlInputFactory.createXMLEventReader(csdl);    
    return buildEdmProvider(reader, this.referenceResolver, this.implicitlyLoadCoreVocabularies,
            this.useLocalCoreVocabularies, true, null);
  }
  
  public SchemaBasedEdmProvider addToEdmProvider(SchemaBasedEdmProvider existing, Reader csdl)
      throws XMLStreamException {
    XMLInputFactory xmlInputFactory = createXmlInputFactory();
    XMLEventReader reader = xmlInputFactory.createXMLEventReader(csdl);
    return addToEdmProvider(existing, reader, this.referenceResolver, this.implicitlyLoadCoreVocabularies,
        this.useLocalCoreVocabularies, true, null);
  }

  protected SchemaBasedEdmProvider buildEdmProvider(Reader csdl, ReferenceResolver resolver,
                                                    boolean loadCore, boolean useLocal,
                                                    boolean loadReferenceSchemas, String namespace)
          throws XMLStreamException {
    XMLInputFactory xmlInputFactory = createXmlInputFactory();
    XMLEventReader reader = xmlInputFactory.createXMLEventReader(csdl);
    return buildEdmProvider(reader, resolver, loadCore, useLocal, loadReferenceSchemas, namespace);
  }

  protected SchemaBasedEdmProvider buildEdmProvider(InputStream csdl, ReferenceResolver resolver,
                                                    boolean loadCore, boolean useLocal,
                                                    boolean loadReferenceSchemas, String namespace)
          throws XMLStreamException {
    XMLInputFactory xmlInputFactory = createXmlInputFactory();
    XMLEventReader reader = xmlInputFactory.createXMLEventReader(csdl);
    return buildEdmProvider(reader, resolver, loadCore, useLocal, loadReferenceSchemas, namespace);
  } 

  protected SchemaBasedEdmProvider buildEdmProvider(XMLEventReader reader, ReferenceResolver resolver, boolean loadCore,
      boolean useLocal, boolean loadReferenceSchemas, String namespace) throws XMLStreamException {
    SchemaBasedEdmProvider provider = new SchemaBasedEdmProvider();
    return addToEdmProvider(provider, reader, resolver, loadCore, useLocal, loadReferenceSchemas, namespace);
  }
  
  protected SchemaBasedEdmProvider addToEdmProvider(SchemaBasedEdmProvider provider, XMLEventReader reader,
      ReferenceResolver resolver, boolean loadCore, boolean useLocal, boolean loadReferenceSchemas, String namespace)
      throws XMLStreamException {
    
    final StringBuilder xmlBase = new StringBuilder();
    
    new ElementReader<SchemaBasedEdmProvider>() {
      @Override
      void build(XMLEventReader reader, StartElement element, SchemaBasedEdmProvider provider,
          String name) throws XMLStreamException {
        if (attrNS(element, XML_LINK_NS, "base") != null) {
          xmlBase.append(attrNS(element, XML_LINK_NS, "base"));
        }
        String version = attr(element, "Version");
        // OData 4.01 is a valid metadata document version for a 4.01 library; CSDL JSON section 4
        // allows exactly "4.0" and "4.01" and the XML gate now matches it.
        if (ODataServiceVersion.V40.toString().equals(version)
            || ODataServiceVersion.V401.toString().equals(version)) {
          readDataServicesAndReference(reader, element, provider);
        } else {
          throw new XMLStreamException("Only OData 4.0 and 4.01 metadata documents are supported.");
        }
      }
    }.read(reader, null, provider, "Edmx");
    
    // make sure there is nothing left to read, due to parser error
    if(reader.hasNext()) {
      XMLEvent event = reader.peek();
      throw new XMLStreamException(
          "Failed to read complete metadata file. Failed at "
              + (event.isStartElement() ? 
                  event.asStartElement().getName().getLocalPart() : 
                  event.asEndElement().getName().getLocalPart()));
    }
    
    try {
      //load core vocabularies even though they are not defined in the references
      if (loadCore) {
        this.referenceLoader.loadCoreVocabularies(provider);
      }

      this.referenceLoader.rememberProvider(namespace, provider);

      // load all the reference schemas
      if (resolver != null && loadReferenceSchemas) {
        this.referenceLoader.loadReferenceSchemas(provider, xmlBase.isEmpty() ? null
            : ReferenceLoader.fixBase(xmlBase.toString()), resolver, loadCore, useLocal,
            this.recursivelyLoadReferences);
      }
    } catch (ODataException e) {
      throw new XMLStreamException(e.getMessage(), e);
    }
    return provider;
  }

  private XMLInputFactory createXmlInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }

  public void loadCoreVocabulary(SchemaBasedEdmProvider provider,
      String namespace) throws XMLStreamException {
    try {
      this.referenceLoader.loadCoreVocabulary(provider, namespace);
    } catch (ODataException e) {
      throw new XMLStreamException(e.getMessage(), e);
    }
  }

  private void readDataServicesAndReference(XMLEventReader reader,
      StartElement element, SchemaBasedEdmProvider provider)
      throws XMLStreamException {
    new ElementReader<SchemaBasedEdmProvider>() {
      @Override
      void build(XMLEventReader reader, StartElement element, SchemaBasedEdmProvider provider,
          String name) throws XMLStreamException {
        if ("DataServices".equals(name)) {
          readSchema(reader, element, provider);
        } else if ("Reference".equals(name)) {
          readReference(reader, element, provider, "Reference");
        }
      }
    }.read(reader, element, provider, "DataServices", "Reference");
  }

  private void readReference(XMLEventReader reader, StartElement element,
      final SchemaBasedEdmProvider provider, String name) throws XMLStreamException {
    EdmxReference reference;
    try {
      String uri = attr(element, "Uri");
      reference = new EdmxReference(new URI(uri));
    } catch (URISyntaxException e) {
      throw new XMLStreamException(e.getMessage(), e);
    }
    new ElementReader<EdmxReference>() {
      @Override
      void build(XMLEventReader reader, StartElement element,
          EdmxReference reference, String name) throws XMLStreamException {
        if ("Include".equals(name)) {
          EdmxReferenceInclude include = new EdmxReferenceInclude(attr(element, "Namespace"),
              attr(element, "Alias"));
          reference.addInclude(include);
        } else if ("IncludeAnnotations".equals(name)) {
          EdmxReferenceIncludeAnnotation annotation = new EdmxReferenceIncludeAnnotation(
              attr(element, "TermNamespace"));
          annotation.setTargetNamespace(attr(element, "TargetNamespace"));
          annotation.setQualifier(attr(element, "Qualifier"));
          reference.addIncludeAnnotation(annotation);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, reference);
        }
      }
    }.read(reader, element, reference, "Include", "IncludeAnnotations", "Annotation");
    provider.addReference(reference);
  }
  
  private void readSchema(XMLEventReader reader, StartElement element,
      SchemaBasedEdmProvider provider) throws XMLStreamException {

    new ElementReader<SchemaBasedEdmProvider>() {
      @Override
      void build(XMLEventReader reader, StartElement element, SchemaBasedEdmProvider provider, String name)
          throws XMLStreamException {
        CsdlSchema schema = new CsdlSchema();
        schema.setComplexTypes(new ArrayList<>());
        schema.setActions(new ArrayList<>());
        schema.setEntityTypes(new ArrayList<>());
        schema.setEnumTypes(new ArrayList<>());
        schema.setFunctions(new ArrayList<>());
        schema.setTerms(new ArrayList<>());
        schema.setTypeDefinitions(new ArrayList<>());        
        schema.setNamespace(attr(element, "Namespace"));
        schema.setAlias(attr(element, "Alias"));
        readSchemaContents(reader, schema);
        provider.addSchema(schema);
      }
    }.read(reader, element, provider, "Schema");
  }

  private void readSchemaContents(XMLEventReader reader, CsdlSchema schema) throws XMLStreamException {
    new ElementReader<CsdlSchema>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlSchema schema, String name)
          throws XMLStreamException {
        if ("Action".equals(name)) {
          readAction(reader, element, schema);
        } else if ("Annotations".equals(name)) {
          readAnnotationGroup(reader, element, schema);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, schema);
        } else if ("ComplexType".equals(name)) {
          readComplexType(reader, element, schema);
        } else if ("EntityContainer".equals(name)) {
          readEntityContainer(reader, element, schema);
        } else if ("EntityType".equals(name)) {
          readEntityType(reader, element, schema);
        } else if ("EnumType".equals(name)) {
          readEnumType(reader, element, schema);
        } else if ("Function".equals(name)) {
          readFunction(reader, element, schema);
        } else if ("Term".equals(name)) {
          schema.getTerms().add(readTerm(reader, element));
        } else if ("TypeDefinition".equals(name)) {
          schema.getTypeDefinitions().add(readTypeDefinition(reader, element));
        }
      }
    }.read(reader, null, schema, "Action", "Annotations", "Annotation", "ComplexType",
        "EntityContainer", "EntityType", "EnumType", "Function", "Term", "TypeDefinition");
  }

  private void readAction(XMLEventReader reader, StartElement element, CsdlSchema schema)
      throws XMLStreamException {

    CsdlAction action = new CsdlAction();
    action.setParameters(new ArrayList<>());
    action.setName(attr(element, "Name"));
    action.setBound(Boolean.parseBoolean(attr(element, "IsBound")));
    String entitySetPath = attr(element, "EntitySetPath");
    if (entitySetPath != null) {
      // EntitySetPath is stored as-is; parsing into binding parameter and path
      // segments happens at resolution time in EdmActionImpl/EdmFunctionImpl
      action.setEntitySetPath(entitySetPath);
    }
    readOperationParameters(reader, action);
    schema.getActions().add(action);
  }

  private FullQualifiedName readType(StartElement element) {
    String type = attr(element, "Type");
    if (type != null && type.startsWith("Collection(") && type.endsWith(")")) {
      return new FullQualifiedName(type.substring(11, type.length() - 1));
    }
    return new FullQualifiedName(type);
  }

  private boolean isCollectionType(StartElement element) {
    String type = attr(element, "Type");
    if (type != null && type.startsWith("Collection(") && type.endsWith(")")) {
      return true;
    }
    return false;
  }

  private void readReturnType(XMLEventReader reader, StartElement element,
      CsdlOperation operation) throws XMLStreamException {
    CsdlReturnType returnType = new CsdlReturnType();
    returnType.setType(readType(element));
    returnType.setCollection(isCollectionType(element));
    returnType.setNullable(attr(element, "Nullable") == null
        || Boolean.parseBoolean(attr(element, "Nullable")));

    String maxLength = attr(element, "MaxLength");
    if (maxLength != null) {
      returnType.setMaxLength(Integer.parseInt(maxLength));
    }
    String precision = attr(element, "Precision");
    if (precision != null) {
      returnType.setPrecision(Integer.parseInt(precision));
    }
    String scale = attr(element, "Scale");
    if (scale != null
        && !"variable".equalsIgnoreCase(scale) && !"floating".equalsIgnoreCase(scale)) {
      returnType.setScale(Integer.parseInt(scale));
    }
    String srid = attr(element, "SRID");
    if (srid != null) {
      returnType.setSrid(SRID.valueOf(srid));
    }
    peekAnnotations(reader, element.getName().getLocalPart(), returnType);
    operation.setReturnType(returnType);
  }

  private void readParameter(XMLEventReader reader, StartElement element,
      CsdlOperation operation) throws XMLStreamException {
    CsdlParameter parameter = new CsdlParameter();
    parameter.setName(attr(element, "Name"));
    parameter.setType(readType(element));
    parameter.setCollection(isCollectionType(element));
    parameter.setNullable(attr(element, "Nullable") == null
        || Boolean.parseBoolean(attr(element, "Nullable")));

    String maxLength = attr(element, "MaxLength");
    if (maxLength != null) {
      parameter.setMaxLength(Integer.parseInt(maxLength));
    }
    String precision = attr(element, "Precision");
    if (precision != null) {
      parameter.setPrecision(Integer.parseInt(precision));
    }
    String scale = attr(element, "Scale");
    if (scale != null
        && !"variable".equalsIgnoreCase(scale) && !"floating".equalsIgnoreCase(scale)) {
      parameter.setScale(Integer.parseInt(scale));
    }
    String srid = attr(element, "SRID");
    if (srid != null) {
      parameter.setSrid(SRID.valueOf(srid));
    }
    peekAnnotations(reader, element.getName().getLocalPart(), parameter);
    operation.getParameters().add(parameter);
  }

  private CsdlTypeDefinition readTypeDefinition(XMLEventReader reader,
      StartElement element) throws XMLStreamException {
    CsdlTypeDefinition td = new CsdlTypeDefinition();
    td.setName(attr(element, "Name"));
    td.setUnderlyingType(new FullQualifiedName(attr(element, "UnderlyingType")));
    if (attr(element, "Unicode") != null) {
      td.setUnicode(Boolean.parseBoolean(attr(element, "Unicode")));
    }

    String maxLength = attr(element, "MaxLength");
    if (maxLength != null) {
      td.setMaxLength(Integer.parseInt(maxLength));
    }
    String precision = attr(element, "Precision");
    if (precision != null) {
      td.setPrecision(Integer.parseInt(precision));
    }
    String scale = attr(element, "Scale");
    if (scale != null
        && !"variable".equalsIgnoreCase(scale) && !"floating".equalsIgnoreCase(scale)) {
      td.setScale(Integer.parseInt(scale));
    }
    String srid = attr(element, "SRID");
    if (srid != null) {
      td.setSrid(SRID.valueOf(srid));
    }
    peekAnnotations(reader, element.getName().getLocalPart(), td);
    return td;
  }

  private CsdlTerm readTerm(XMLEventReader reader, StartElement element) throws XMLStreamException {
    CsdlTerm term = new CsdlTerm();
    term.setName(attr(element, "Name"));
    term.setType(attr(element, "Type"));
    
    if (attr(element, "BaseTerm") != null) {
      term.setBaseTerm(attr(element, "BaseTerm"));
    }
    if (attr(element, "DefaultValue") != null) {
      term.setDefaultValue(attr(element, "DefaultValue"));
    }
    if (attr(element, "AppliesTo") != null) {
      String[] appliesTo = WHITESPACE.split(attr(element, "AppliesTo"));
      term.setAppliesTo(List.of(appliesTo));
    }
    term.setNullable(attr(element, "Nullable") == null
        || Boolean.parseBoolean(attr(element, "Nullable")));
    String maxLength = attr(element, "MaxLength");
    if (maxLength != null) {
      term.setMaxLength(Integer.parseInt(maxLength));
    }
    String precision = attr(element, "Precision");
    if (precision != null) {
      term.setPrecision(Integer.parseInt(precision));
    }
    String scale = attr(element, "Scale");
    if (scale != null
        && !"variable".equalsIgnoreCase(scale) && !"floating".equalsIgnoreCase(scale)) {
      term.setScale(Integer.parseInt(scale));
    }
    String srid = attr(element, "SRID");
    if (srid != null) {
      term.setSrid(SRID.valueOf(srid));
    }
    peekAnnotations(reader, "Term", term);
    return term;
  }

  private void readAnnotationGroup(XMLEventReader reader, StartElement element,
      CsdlSchema schema) throws XMLStreamException {
    final CsdlAnnotations annotations = new CsdlAnnotations();
    annotations.setTarget(attr(element, "Target"));
    annotations.setQualifier(attr(element, "Qualifier"));
    peekAnnotations(reader, element.getName().getLocalPart(), annotations);
    schema.getAnnotationGroups().add(annotations);
  }

  private void peekAnnotations(XMLEventReader reader, String endName,
      CsdlAnnotatable edmObject) throws XMLStreamException {
    if(!parseAnnotations) {
      return;
    }
    while (reader.hasNext()) {
      XMLEvent event = reader.peek();

      if (!event.isStartElement() && !event.isEndElement()) {
        reader.nextEvent();
        continue;
      }
      
      if (event.isStartElement()) {
        StartElement element = event.asStartElement();
        if ("Annotation".equals(element.getName().getLocalPart())) {
          reader.nextEvent();
          readAnnotations(reader, element, edmObject);
        }
      }
      
      if (event.isEndElement()) {
        EndElement element = event.asEndElement();
        if ("Annotation".equals(element.getName().getLocalPart())) {
          reader.nextEvent();
        }
        
        if (element.getName().getLocalPart().equals(endName)) {
          return;
        }
      }
    }
  }
  
  private void readAnnotations(XMLEventReader reader, StartElement element,
      CsdlAnnotatable edmObject) throws XMLStreamException {
    if (!parseAnnotations) {
      return;
    }
    final CsdlAnnotation annotation = new CsdlAnnotation();
    annotation.setTerm(attr(element, "Term"));
    // CSDL XML section 14.2.1: "A term can be applied multiple times to the same model element by
    // providing a qualifier to distinguish the annotations." Without it two applications of one term
    // are indistinguishable, and the CSDL JSON writer emits the same member name twice.
    annotation.setQualifier(attr(element, "Qualifier"));
    for (ConstantExpressionType type:ConstantExpressionType.values()) {
      if (attr(element, type.name()) != null) {
        annotation.setExpression(new CsdlConstantExpression(
            type, attr(element, type.name())));
      }        
    }
    readExpressions(reader, element, annotation);
    edmObject.getAnnotations().add(annotation);
  } 

  private <T> void write(T t, CsdlExpression expr) throws XMLStreamException {
    if(t instanceof CsdlAnnotation csdlAnnotation) {
      csdlAnnotation.setExpression(expr);
    } else if (t instanceof CsdlUrlRef csdlUrlRef) {
      csdlUrlRef.setValue(expr);
    } else if (t instanceof CsdlCast csdlCast) {
      csdlCast.setValue(expr);
    } else if (t instanceof CsdlLabeledElement csdlLabeledElement) {
      csdlLabeledElement.setValue(expr);
    } else if (t instanceof CsdlIsOf csdlIsOf) {
      csdlIsOf.setValue(expr);
    } else if (t instanceof CsdlCollection csdlCollection) {
      csdlCollection.getItems().add(csdlCollection.getItems().size(), expr);
    } else if (t instanceof CsdlApply csdlApply) {
      csdlApply.getParameters().add(expr);
    } else if (t instanceof CsdlIf csdlIf) {
      if (csdlIf.getGuard() == null) {
        csdlIf.setGuard(expr);
      } else if (csdlIf.getThen() == null) {
        csdlIf.setThen(expr);
      } else {
        csdlIf.setElse(expr);
      }
    } else if (t instanceof CsdlPropertyValue csdlPropertyValue) {
      csdlPropertyValue.setValue(expr);
    } else {
      throw new XMLStreamException("Unknown expression parent in Annoatation");
    }
  }
  
  private <T> void readExpressions(XMLEventReader reader,
      StartElement element, T target)
      throws XMLStreamException {
    new ElementReader<T>() {
      @Override
      void build(XMLEventReader reader, StartElement element, T target, String name)
          throws XMLStreamException {
        
        // element based expressions
        if (!"Annotation".equals(name)) {
          // attribute based expressions.
          readAttributeExpressions(element, target);        
          
          for (ConstantExpressionType type:ConstantExpressionType.values()) {
            if (name.equals(type.name()) && reader.peek().isCharacters()) {
              CsdlExpression expr = new CsdlConstantExpression(type, elementValue(reader, element));
              write(target, expr);
            }        
          }
        }
        
        if ("Collection".equals(name)) {
          CsdlCollection expr = new CsdlCollection();
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("AnnotationPath".equals(name)) {
          write(target, new CsdlAnnotationPath().setValue(elementValue(reader, element)));
        } else if ("NavigationPropertyPath".equals(name)) {
          write(target, new CsdlNavigationPropertyPath()
              .setValue(elementValue(reader, element)));
        } else if ("Path".equals(name)) {
          write(target, new CsdlPath().setValue(elementValue(reader, element)));
        } else if ("PropertyPath".equals(name)) {
          write(target, new CsdlPropertyPath().setValue(elementValue(reader, element)));
        } else if ("UrlRef".equals(name)) {
          CsdlUrlRef expr = new CsdlUrlRef();
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("Apply".equals(name)) {
          CsdlApply expr = new CsdlApply();
          expr.setFunction(attr(element, "Function"));
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("Cast".equals(name)) {
          CsdlCast expr = new CsdlCast();
          expr.setType(attr(element, "Type"));
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("If".equals(name)) {
          CsdlIf expr = new CsdlIf();
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("IsOf".equals(name)) {
          CsdlIsOf expr = new CsdlIsOf();
          expr.setType(attr(element, "Type"));
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("LabeledElement".equals(name)) {
          CsdlLabeledElement expr = new CsdlLabeledElement();
          expr.setName(attr(element, "Name"));
          readExpressions(reader, element, expr);
          write(target, expr);
        } else if ("LabeledElementReference".equals(name)) {
          CsdlLabeledElementReference expr = new CsdlLabeledElementReference();
          expr.setValue(elementValue(reader, element));
          write(target, expr);
        } else if ("Null".equals(name)) {
          write(target, new CsdlNull());
        } else if ("Record".equals(name)) {
          CsdlRecord expr = new CsdlRecord();
          expr.setType(attr(element, "Type"));          
          readPropertyValues(reader, element, expr);
          write(target, expr);          
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, (CsdlAnnotatable)target);
        }
      }
    }.read(reader, element, target, "Collection", "AnnotationPath",
        "NavigationPropertyPath", "Path", "PropertyPath", "UrlRef",
        "Apply", "Function", "Cast", "If", "IsOf", "LabeledElement",
        "LabeledElementReference", "Null", "Record","Binary", "Bool", "Date",
        "DateTimeOffset", "Decimal", "Duration", "EnumMember", "Float", "Guid",
        "Int", "String", "TimeOfDay", "Annotation");
  }
  
  private <T> void readAttributeExpressions(StartElement element, T target)
      throws XMLStreamException {
    // attribute based expressions
    for (ConstantExpressionType type:ConstantExpressionType.values()) {
      if (attr(element, type.name()) != null) {
        write(target, new CsdlConstantExpression(
            type, attr(element, type.name())));
      }        
    }
    
    if (attr(element,  "AnnotationPath") != null) {
     write(target, new CsdlAnnotationPath().setValue(attr(element,  "AnnotationPath"))); 
    }
    if (attr(element,  "NavigationPropertyPath") != null) {
      write(target, new CsdlNavigationPropertyPath()
          .setValue(attr(element, "NavigationPropertyPath"))); 
    }
    if (attr(element,  "Path") != null) {
      write(target, new CsdlPath().setValue(attr(element, "Path"))); 
    }
    if (attr(element,  "PropertyPath") != null) {
      write(target, new CsdlPropertyPath().setValue(attr(element, "PropertyPath"))); 
    }
    if (attr(element,  "UrlRef") != null) {
      write(target, new CsdlUrlRef().setValue(new CsdlConstantExpression(
          ConstantExpressionType.String, attr(element, "UrlRef"))));
    }
  }  
  
  private String elementValue(XMLEventReader reader, StartElement element) throws XMLStreamException {
    while (reader.hasNext()) {
      XMLEvent event = reader.peek();
      if (event.isStartElement() || event.isEndElement()) {
        return null;
      } else if (event.isCharacters()){
        reader.nextEvent();
        String data = event.asCharacters().getData();
        if (!data.isBlank()) {
          return data.trim();
        }
      }
    }    
    return null;
  }
  
  private void readPropertyValues(XMLEventReader reader,
      StartElement element, CsdlRecord record) throws XMLStreamException {
    
    new ElementReader<CsdlRecord>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlRecord record, String name)
          throws XMLStreamException {
        if ("PropertyValue".equals(name)) {
          CsdlPropertyValue value = new CsdlPropertyValue();
          value.setProperty(attr(element, "Property"));
          readAttributeExpressions(element, value);
          readExpressions(reader, element, value);
          record.getPropertyValues().add(value);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, record);
        }
      }
    }.read(reader, element, record, "PropertyValue", "Annotation");    
  }  
  
  private void readFunction(XMLEventReader reader, StartElement element, CsdlSchema schema)
      throws XMLStreamException {
    CsdlFunction function = new CsdlFunction();
    function.setParameters(new ArrayList<>());
    function.setName(attr(element, "Name"));
    function.setBound(Boolean.parseBoolean(attr(element, "IsBound")));
    function.setComposable(Boolean.parseBoolean(attr(element, "IsComposable")));
    String entitySetPath = attr(element, "EntitySetPath");
    if (entitySetPath != null) {
      // EntitySetPath is stored as-is; parsing into binding parameter and path
      // segments happens at resolution time in EdmActionImpl/EdmFunctionImpl
      function.setEntitySetPath(entitySetPath);
    }
    readOperationParameters(reader, function);
    schema.getFunctions().add(function);
  }

  private void readOperationParameters(XMLEventReader reader, final CsdlOperation operation)
      throws XMLStreamException {
    new ElementReader<CsdlOperation>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlOperation operation, String name)
          throws XMLStreamException {
        if ("Parameter".equals(name)) {
          readParameter(reader, element, operation);
        } else if ("ReturnType".equals(name)) {
          readReturnType(reader, element, operation);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, operation);
        }
      }
    }.read(reader, null, operation, "Parameter", "ReturnType", "Annotation");
  }

  private void readEnumType(XMLEventReader reader, StartElement element, CsdlSchema schema)
      throws XMLStreamException {
    CsdlEnumType type = new CsdlEnumType();
    type.setMembers(new ArrayList<>());
    type.setName(attr(element, "Name"));
    if (attr(element, "UnderlyingType") != null) {
      type.setUnderlyingType(new FullQualifiedName(attr(element, "UnderlyingType")));
    }
    type.setFlags(Boolean.parseBoolean(attr(element, "IsFlags")));

    readEnumMembers(reader, element, type);
    schema.getEnumTypes().add(type);
  }

  private void readEnumMembers(XMLEventReader reader, StartElement element, CsdlEnumType type)
      throws XMLStreamException {
    
    new ElementReader<CsdlEnumType>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlEnumType type, String name)
          throws XMLStreamException {
        if ("Member".equals(name)) {
          CsdlEnumMember member = new CsdlEnumMember();
          member.setName(attr(element, "Name"));
          member.setValue(attr(element, "Value"));
          peekAnnotations(reader, name, member);
          type.getMembers().add(member);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, type);
        }
      }
    }.read(reader, element, type, "Member", "Annotation");
  }

  private void readEntityType(XMLEventReader reader, StartElement element, CsdlSchema schema)
      throws XMLStreamException {
    CsdlEntityType entityType = new CsdlEntityType();
    entityType.setProperties(new ArrayList<>());
    entityType.setNavigationProperties(new ArrayList<>());
    entityType.setKey(new ArrayList<>());
    entityType.setName(attr(element, "Name"));
    if (attr(element, "BaseType") != null) {
      entityType.setBaseType(new FullQualifiedName(attr(element, "BaseType")));
    }
    entityType.setAbstract(Boolean.parseBoolean(attr(element, "Abstract")));
    entityType.setOpenType(Boolean.parseBoolean(attr(element, "OpenType")));
    entityType.setHasStream(Boolean.parseBoolean(attr(element, "HasStream")));
    readEntityProperties(reader, entityType);
    schema.getEntityTypes().add(entityType);
  }

  private void readEntityProperties(XMLEventReader reader, CsdlEntityType entityType)
      throws XMLStreamException {
    new ElementReader<CsdlEntityType>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlEntityType entityType, String name)
          throws XMLStreamException {
        if ("Property".equals(name)) {
          entityType.getProperties().add(readProperty(reader, element));
        } else if ("NavigationProperty".equals(name)) {
          entityType.getNavigationProperties().add(readNavigationProperty(reader, element));
        } else if ("Key".equals(name)) {
          readKey(reader, element, entityType);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, entityType);
        }
      }
    }.read(reader, null, entityType, "Property", "NavigationProperty", "Key", "Annotation");
  }

  private void readKey(XMLEventReader reader, StartElement element, CsdlEntityType entityType)
      throws XMLStreamException {
    new ElementReader<CsdlEntityType>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlEntityType entityType, String name)
          throws XMLStreamException {
        CsdlPropertyRef ref = new CsdlPropertyRef();
        ref.setName(attr(element, "Name"));
        ref.setAlias(attr(element, "Alias"));
        entityType.getKey().add(ref);
      }
    }.read(reader, element, entityType, "PropertyRef");
  }

  private CsdlNavigationProperty readNavigationProperty(XMLEventReader reader, StartElement element)
      throws XMLStreamException {
    CsdlNavigationProperty property = new CsdlNavigationProperty();
    property.setReferentialConstraints(new ArrayList<>());

    property.setName(attr(element, "Name"));
    property.setType(readType(element));
    property.setCollection(isCollectionType(element));
    property.setNullable(Boolean.parseBoolean(attr(element, "Nullable") == null ? "true" : attr(element, "Nullable")));
    property.setPartner(attr(element, "Partner"));
    property.setContainsTarget(Boolean.parseBoolean(attr(element, "ContainsTarget")));

    new ElementReader<CsdlNavigationProperty>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlNavigationProperty property,
          String name) throws XMLStreamException {
        if ("ReferentialConstraint".equals(name)) {
          CsdlReferentialConstraint constraint = new CsdlReferentialConstraint();
          constraint.setProperty(attr(element, "Property"));
          constraint.setReferencedProperty(attr(element, "ReferencedProperty"));
          peekAnnotations(reader, name, constraint);
          property.getReferentialConstraints().add(constraint);
        } else if ("OnDelete".equals(name)) {
          CsdlOnDelete delete = new CsdlOnDelete();
          delete.setAction(CsdlOnDeleteAction.valueOf(attr(element, "Action")));
          property.setOnDelete(delete);
          peekAnnotations(reader, name, delete);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, property);
        }
      }
    }.read(reader, element, property, "ReferentialConstraint", "OnDelete", "Annotation");
    return property;
  }

  private static String attr(StartElement element, String name) {
    Attribute attr = element.getAttributeByName(new QName(name));
    if (attr != null) {
      return attr.getValue();
    }
    return null;
  }

  private static String attrNS(StartElement element, String ns, String name) {
    Attribute attr = element.getAttributeByName(new QName(ns, name));
    if (attr != null) {
      return attr.getValue();
    }
    return null;
  }  
  
  private CsdlProperty readProperty(XMLEventReader reader, StartElement element)
      throws XMLStreamException {
    CsdlProperty property = new CsdlProperty();
    property.setName(attr(element, "Name"));
    property.setType(readType(element));
    property.setCollection(isCollectionType(element));
    property.setNullable(Boolean.parseBoolean(attr(element, "Nullable") == null ? "true" : attr(
        element, "Nullable")));
    if (attr(element, "Unicode") != null) {
      property.setUnicode(Boolean.parseBoolean(attr(element, "Unicode")));
    }

    String maxLength = attr(element, "MaxLength");
    if (maxLength != null) {
      property.setMaxLength(Integer.parseInt(maxLength));
    }
    String precision = attr(element, "Precision");
    if (precision != null) {
      property.setPrecision(Integer.parseInt(precision));
    }
    String scale = attr(element, "Scale");
    if (scale != null) {
      if ("variable".equalsIgnoreCase(scale) || "floating".equalsIgnoreCase(scale)) {
        property.setScaleAsString(scale);
      } else {
        property.setScale(Integer.parseInt(scale));
      }
    }
    String srid = attr(element, "SRID");
    if (srid != null) {
      property.setSrid(SRID.valueOf(srid));
    }
    String defaultValue = attr(element, "DefaultValue");
    if (defaultValue != null) {
      property.setDefaultValue(defaultValue);
    }
    peekAnnotations(reader, element.getName().getLocalPart(), property);
    return property;
  }

  private void readEntityContainer(XMLEventReader reader, StartElement element, CsdlSchema schema)
      throws XMLStreamException {
    final CsdlEntityContainer container = new CsdlEntityContainer();
    container.setName(attr(element, "Name"));
    if (attr(element, "Extends") != null) {
      container.setExtendsContainer(attr(element, "Extends"));
    }
    container.setActionImports(new ArrayList<>());
    container.setFunctionImports(new ArrayList<>());
    container.setEntitySets(new ArrayList<>());
    container.setSingletons(new ArrayList<>());

    new ElementReader<CsdlSchema>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlSchema schema, String name)
          throws XMLStreamException {
        if ("EntitySet".equals(name)) {
          readEntitySet(reader, element, container);
        } else if ("Singleton".equals(name)) {
          readSingleton(reader, element, container);
        } else if ("ActionImport".equals(name)) {
          readActionImport(reader, element, container);
        } else if ("FunctionImport".equals(name)) {
          readFunctionImport(reader, element, container);
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, container);
        }
      }

      private void readFunctionImport(XMLEventReader reader,
          StartElement element, CsdlEntityContainer container)
          throws XMLStreamException {
        CsdlFunctionImport functionImport = new CsdlFunctionImport();
        functionImport.setName(attr(element, "Name"));
        functionImport.setFunction(new FullQualifiedName(attr(element, "Function")));
        functionImport.setIncludeInServiceDocument(Boolean.parseBoolean(attr(element,
            "IncludeInServiceDocument")));

        String entitySet = attr(element, "EntitySet");
        if (entitySet != null) {
          functionImport.setEntitySet(entitySet);
        }
        peekAnnotations(reader, "FunctionImport", functionImport);
        container.getFunctionImports().add(functionImport);
      }

      private void readActionImport(XMLEventReader reader,
          StartElement element, CsdlEntityContainer container)
          throws XMLStreamException {
        CsdlActionImport actionImport = new CsdlActionImport();
        actionImport.setName(attr(element, "Name"));
        actionImport.setAction(new FullQualifiedName(attr(element, "Action")));

        String entitySet = attr(element, "EntitySet");
        if (entitySet != null) {
          actionImport.setEntitySet(entitySet);
        }
        peekAnnotations(reader, "ActionImport", actionImport);
        container.getActionImports().add(actionImport);
      }

      private void readSingleton(XMLEventReader reader, StartElement element,
          CsdlEntityContainer container) throws XMLStreamException {
        CsdlSingleton singleton = new CsdlSingleton();
        singleton.setNavigationPropertyBindings(new ArrayList<>());
        singleton.setName(attr(element, "Name"));
        singleton.setType(new FullQualifiedName(attr(element, "Type")));
        singleton.setNavigationPropertyBindings(new ArrayList<>());
        readNavigationPropertyBindings(reader, element, singleton);
        container.getSingletons().add(singleton);
      }

      private void readEntitySet(XMLEventReader reader, StartElement element,
          CsdlEntityContainer container) throws XMLStreamException {
        CsdlEntitySet entitySet = new CsdlEntitySet();
        entitySet.setName(attr(element, "Name"));
        entitySet.setType(new FullQualifiedName(attr(element, "EntityType")));
        entitySet.setIncludeInServiceDocument(Boolean.parseBoolean(attr(element,
            "IncludeInServiceDocument")));
        entitySet.setNavigationPropertyBindings(new ArrayList<>());
        readNavigationPropertyBindings(reader, element, entitySet);
        container.getEntitySets().add(entitySet);
      }

      private void readNavigationPropertyBindings(XMLEventReader reader, StartElement element,
          CsdlBindingTarget entitySet) throws XMLStreamException {
        new ElementReader<CsdlBindingTarget>() {
          @Override
          void build(XMLEventReader reader, StartElement element,
              CsdlBindingTarget entitySet, String name) throws XMLStreamException {
            if ("NavigationPropertyBinding".equals(name)) {
              CsdlNavigationPropertyBinding binding = new CsdlNavigationPropertyBinding();
              binding.setPath(attr(element, "Path"));
              binding.setTarget(attr(element, "Target"));
              entitySet.getNavigationPropertyBindings().add(binding);
            } else if ("Annotation".equals(name)) {
              readAnnotations(reader, element, entitySet);
            }
          }

        }.read(reader, element, entitySet, "NavigationPropertyBinding", "Annotation");
      }
    }.read(reader, element, schema, "EntitySet", "Singleton", "ActionImport", "FunctionImport", "Annotation");
    schema.setEntityContainer(container);
  }

  private void readComplexType(XMLEventReader reader, StartElement element, CsdlSchema schema)
      throws XMLStreamException {
    CsdlComplexType complexType = new CsdlComplexType();
    complexType.setProperties(new ArrayList<>());
    complexType.setNavigationProperties(new ArrayList<>());
    complexType.setName(attr(element, "Name"));
    if (attr(element, "BaseType") != null) {
      complexType.setBaseType(new FullQualifiedName(attr(element, "BaseType")));
    }
    complexType.setAbstract(Boolean.parseBoolean(attr(element, "Abstract")));
    complexType.setOpenType(Boolean.parseBoolean(attr(element, "OpenType")));
    readProperties(reader, complexType);

    schema.getComplexTypes().add(complexType);
  }

  private void readProperties(XMLEventReader reader, CsdlComplexType complexType)
      throws XMLStreamException {
    new ElementReader<CsdlComplexType>() {
      @Override
      void build(XMLEventReader reader, StartElement element, CsdlComplexType complexType, String name)
          throws XMLStreamException {
        if ("Property".equals(name)) {
          complexType.getProperties().add(readProperty(reader, element));
        } else if ("NavigationProperty".equals(name)) {
          complexType.getNavigationProperties().add(readNavigationProperty(reader, element));
        } else if ("Annotation".equals(name)) {
          readAnnotations(reader, element, complexType);
        }
      }
    }.read(reader, null, complexType, "Property", "NavigationProperty", "Annotation");
  }

  abstract class ElementReader<T> {
    void read(XMLEventReader reader, StartElement parentElement, T t, String... names)
        throws XMLStreamException {
      while (reader.hasNext()) {
        XMLEvent event = reader.peek();

        if (!parseAnnotations) {
          XMLEvent eventBefore = event;
          event = skipAnnotations(reader, event);
          // if annotation is stripped start again
          if (eventBefore != event) {            
            continue;
          }
        }

        if (!event.isStartElement() && !event.isEndElement()) {
          reader.nextEvent();
          continue;
        }

        if (parentElement != null && event.isEndElement()
            && ((EndElement) event).getName().equals(parentElement.getName())) {
          // end reached
          break;
        }

        boolean hit = false;

        for (String name : names) {
          if (event.isStartElement()) {
            StartElement element = event.asStartElement();
            if (element.getName().getLocalPart().equals(name)) {              
              reader.nextEvent(); // advance cursor start which is current
              build(reader, element, t, name);
              hit = true;
              break;
            }
          }
          if (event.isEndElement()) {
            EndElement e = event.asEndElement();
            if (e.getName().getLocalPart().equals(name)) {
              reader.nextEvent(); // advance cursor to end which is current
              hit = true;
              break;
            }
          }
        }
        if (!hit) {
          break;
        }
      }
    }

    private XMLEvent skipAnnotations(XMLEventReader reader, XMLEvent event)
        throws XMLStreamException {
      boolean skip = false;

      while (reader.hasNext()) {
        if (event.isStartElement()) {
          StartElement element = event.asStartElement();
          if ("Annotation".equals(element.getName().getLocalPart())) {
            skip = true;
          }
        }
        if (event.isEndElement()) {
          EndElement element = event.asEndElement();
          if ("Annotation".equals(element.getName().getLocalPart())) {
            return reader.peek();
          }
        }
        if (skip) {
          event = reader.nextEvent();
        } else {
          return event;
        }
      }
      return event;
    }

    abstract void build(XMLEventReader reader, StartElement element, T t, String name)
        throws XMLStreamException;
  }
  
  /**
   * The resolver used when no other one is configured. Shared with the CSDL JSON parser so that both
   * parsers resolve references the same way.
   */
  static ReferenceResolver defaultReferenceResolver() {
    return new DefaultReferenceResolver();
  }

  private static class DefaultReferenceResolver implements ReferenceResolver {
    @Override
    public InputStream resolveReference(URI referenceUri, String xmlBase) {
      InputStream in = null;
      try {
        if (referenceUri.isAbsolute()) {
          URL schemaURL = referenceUri.toURL();
          in = schemaURL.openStream();
        } else {
          if (xmlBase != null) {
            URL schemaURL = new URL(xmlBase+referenceUri.toString());
            in = schemaURL.openStream();
          } else {
            in = this.getClass().getClassLoader().getResourceAsStream(referenceUri.getPath());
            if (in == null) {
              throw new EdmException("No xml:base set to read the references from the metadata");
            }
          }        
        }
        return in;
      } catch (MalformedURLException e) {
        throw new EdmException(e);
      } catch (IOException e) {
        throw new EdmException(e);
      }
    }
  }   
}
