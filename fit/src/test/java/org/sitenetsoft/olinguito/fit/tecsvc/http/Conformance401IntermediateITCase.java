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
 * Copyright 2026 SiteNetSoft - Tier 8: end-to-end evidence for the OData 4.01 Intermediate clauses
 */
package org.sitenetsoft.olinguito.fit.tecsvc.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.sitenetsoft.olinguito.client.api.ODataClient;
import org.sitenetsoft.olinguito.commons.api.http.HttpHeader;
import org.sitenetsoft.olinguito.commons.api.http.HttpMethod;
import org.sitenetsoft.olinguito.commons.api.http.HttpStatusCode;
import org.sitenetsoft.olinguito.fit.AbstractBaseTestITCase;
import org.sitenetsoft.olinguito.fit.tecsvc.TecSvcConst;
import org.junit.Test;

/**
 * End-to-end evidence for the [OData-Protocol] section 13.2.2 clauses Tier 8 closes, and for the
 * section 13.1.2 item 4 gap that level inherits. The parser and evaluator carry their own unit
 * tests; these exercise the same clauses over real HTTP, because this codebase's recurring failure
 * shape is a construct that parses cleanly and is then dropped before it reaches a response.
 */
public class Conformance401IntermediateITCase extends AbstractBaseTestITCase {

  private static final String SERVICE_URI = TecSvcConst.BASE_URI + "/";
  private static final String APPLICATION_JSON = "application/json";

  /**
   * 13.1.2 item 4. ESTwoPrimDerived is the entity set that genuinely holds mixed types: four
   * ETTwoPrim entities plus one ETBase (DataCreator:1238-1245). A cast restricts the collection to
   * the instances of the derived type, and the rest of the filter is evaluated against those.
   */
  @Test
  public void castInFilterSelectsMatchingInstances() throws Exception {
    // The one derived entity carries AdditionalPropertyString_5; the four base ones do not.
    assertEquals(1, entityCount("ESTwoPrimDerived?$filter=olingo.odata.test1.ETBase"
        + "/AdditionalPropertyString_5 eq 'Additional String1'"));
    assertEquals(0, entityCount("ESTwoPrimDerived?$filter=olingo.odata.test1.ETBase"
        + "/AdditionalPropertyString_5 eq 'nope'"));
  }

  /**
   * The same cast combined with another clause used to answer 500: the collection-narrowing special
   * case recognised only a top-level Binary whose left operand carried the type filter, so with an
   * "and" it did not fire and the cast member was evaluated against entities of the base type.
   */
  @Test
  public void castInFilterCombinedWithAnotherClause() throws Exception {
    // The derived entity is PropertyInt16 32766, so both clauses hold.
    assertEquals(1, entityCount("ESTwoPrimDerived?$filter=olingo.odata.test1.ETBase"
        + "/AdditionalPropertyString_5 eq 'Additional String1' and PropertyInt16 eq 32766"));

    // A second clause that excludes the only derived entity leaves nothing.
    assertEquals(0, entityCount("ESTwoPrimDerived?$filter=olingo.odata.test1.ETBase"
        + "/AdditionalPropertyString_5 eq 'Additional String1' and PropertyInt16 eq -365"));
  }

  /**
   * An entity that is not of the cast type makes the comparison false rather than raising an error:
   * [OData-URL] 5.1.1.10, "If the type cast is part of a Boolean expression, the type cast will
   * evaluate to null". ESTwoPrim holds no derived instances at all, so every cast filter over it is
   * empty -- it does not address the separate ESBase entity set.
   */
  @Test
  public void castOverACollectionWithNoInstancesOfThatTypeIsEmpty() throws Exception {
    assertEquals(0, entityCount("ESTwoPrim?$filter=olingo.odata.test1.ETBase"
        + "/AdditionalPropertyString_5 eq 'TEST A 0815'"));
  }

  /**
   * 13.2.2 item 3. A bare single-valued navigation property compared to null used to answer 500: the
   * property loop in visitMember starts at index 1 and never runs when the navigation property is the
   * whole path, leaving the EDM property null before getType(). ESAllPrim has four entities, two of
   * which link NavPropertyETTwoPrimOne (DataCreator:2102,2108).
   */
  @Test
  public void singleValuedNavigationComparedToNull() throws Exception {
    assertEquals(4, entityCount("ESAllPrim"));
    assertEquals(2, entityCount("ESAllPrim?$filter=NavPropertyETTwoPrimOne ne null"));
    assertEquals(2, entityCount("ESAllPrim?$filter=NavPropertyETTwoPrimOne eq null"));
  }

  /**
   * 13.1.2 item 4 covers casting a function result too: FICRTCollESTwoKeyNavParam returns a
   * collection of ETTwoKeyNav, and ETBaseTwoKeyNav derives from it. The service used to reject any
   * type filter on an operation segment with 501.
   */
  @Test
  public void castOnFunctionResult() throws Exception {
    assertStatus(HttpStatusCode.OK, "FICRTCollESTwoKeyNavParam(ParameterInt16=1)");
    assertStatus(HttpStatusCode.OK,
        "FICRTCollESTwoKeyNavParam(ParameterInt16=1)/olingo.odata.test1.ETBaseTwoKeyNav");
  }

  /**
   * 13.2.2 item 5, the level's remaining MUST: $select nested within $select. CTTwoPrim carries two
   * structural members, so a nested selection of one of them is visible in the payload -- a complex
   * type with a single structural member would project identically either way and prove nothing.
   *
   * [OData-Protocol] 11.2.5.1 also requires that "the context URL MUST reflect the set of selected
   * properties", so both spellings must produce byte-identical responses.
   */
  @Test
  public void nestedSelectProjectsLikeThePathForm() throws Exception {
    final String nested = body("ESKeyNav(1)?$select=PropertyCompTwoPrim($select=PropertyInt16)");
    final String path = body("ESKeyNav(1)?$select=PropertyCompTwoPrim/PropertyInt16");

    assertTrue("the nested form must project PropertyInt16: " + nested,
        nested.contains("\"PropertyInt16\""));
    assertFalse("the nested form must not project the sibling member: " + nested,
        nested.contains("\"PropertyString\""));
    assertEquals("both spellings must project the same members and context URL", path, nested);
  }

  /**
   * Item 9's collection options parse but are not evaluated yet. [OData-Protocol] 13.1.2 item 2
   * requires 501 for parsed-but-unsupported functionality, which is honest; accepting the option
   * and silently ignoring it is the failure mode this tier exists to remove.
   */
  @Test
  public void unevaluatedSelectCollectionOptionsAreNotImplemented() throws Exception {
    assertStatus(HttpStatusCode.NOT_IMPLEMENTED, "ESKeyNav(1)?$select=CollPropertyString($top=1)");
    assertStatus(HttpStatusCode.NOT_IMPLEMENTED,
        "ESKeyNav(1)?$select=CollPropertyString($orderby=$it desc)");

    // A nested $select is evaluated, so it must not be caught by the same guard.
    assertStatus(HttpStatusCode.OK, "ESKeyNav(1)?$select=PropertyCompTwoPrim($select=PropertyInt16)");
  }

  private int entityCount(final String pathAndQuery) throws IOException {
    final String content = body(pathAndQuery);
    final int start = content.indexOf("\"value\":[");
    assertTrue("expected a collection response: " + content, start >= 0);
    // Every tecsvc entity in these sets carries PropertyInt16, so counting it counts entities.
    return content.substring(start).split("\"PropertyInt16\"", -1).length - 1;
  }

  private void assertStatus(final HttpStatusCode expected, final String pathAndQuery) throws IOException {
    final HttpURLConnection connection = connect(pathAndQuery);
    assertEquals(pathAndQuery + " -> " + read(connection), expected.getStatusCode(),
        connection.getResponseCode());
  }

  private String body(final String pathAndQuery) throws IOException {
    final HttpURLConnection connection = connect(pathAndQuery);
    final String content = read(connection);
    assertEquals(pathAndQuery + " -> " + content, HttpStatusCode.OK.getStatusCode(),
        connection.getResponseCode());
    return content;
  }

  private HttpURLConnection connect(final String pathAndQuery) throws IOException {
    final URL url = new URL(SERVICE_URI + pathAndQuery.replace(" ", "%20").replace("'", "%27"));
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(HttpMethod.GET.toString());
    connection.setRequestProperty(HttpHeader.ACCEPT, APPLICATION_JSON);
    connection.connect();
    return connection;
  }

  private static String read(final HttpURLConnection connection) throws IOException {
    final InputStream stream = connection.getResponseCode() >= 400
        ? connection.getErrorStream() : connection.getInputStream();
    return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Override
  protected ODataClient getClient() {
    return null;
  }
}
