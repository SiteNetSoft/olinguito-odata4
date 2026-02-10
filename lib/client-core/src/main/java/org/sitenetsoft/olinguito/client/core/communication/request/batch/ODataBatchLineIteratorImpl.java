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
 * Copyright 2026 SiteNetSoft - Fixed deprecated API usages;
 * replaced commons-io LineIterator with BufferedReader
 */
package org.sitenetsoft.olinguito.client.core.communication.request.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.sitenetsoft.olinguito.client.api.communication.request.batch.ODataBatchLineIterator;

/**
 * Batch line iterator class.
 */
public class ODataBatchLineIteratorImpl implements ODataBatchLineIterator {

  private final BufferedReader reader;

  /**
   * Last cached line.
   */
  private String current;

  /**
   * Lookahead line for hasNext() support.
   */
  private String lookahead;
  private boolean lookaheadFetched;

  /**
   * Constructor.
   *
   * @param reader buffered reader to iterate lines from.
   */
  public ODataBatchLineIteratorImpl(final BufferedReader reader) {
    this.reader = reader;
    this.current = null;
    this.lookahead = null;
    this.lookaheadFetched = false;
  }

  @Override
  public boolean hasNext() {
    if (!lookaheadFetched) {
      try {
        lookahead = reader.readLine();
      } catch (IOException e) {
        lookahead = null;
      }
      lookaheadFetched = true;
    }
    return lookahead != null;
  }

  @Override
  public String next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return nextLine();
  }

  @Override
  public String nextLine() {
    if (!lookaheadFetched) {
      hasNext();
    }
    current = lookahead;
    lookaheadFetched = false;
    lookahead = null;
    return current;
  }

  /**
   * Unsupported operation.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Unsupported operation");
  }

  @Override
  public String getCurrent() {
    return current;
  }
}
