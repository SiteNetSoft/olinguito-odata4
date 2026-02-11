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
 * Copyright 2026 SiteNetSoft - Replacement for commons-lang3 Triple/ImmutableTriple
 */
package org.sitenetsoft.olinguito.ext.proxy.commons;

/**
 * An immutable triple of three elements, replacing
 * {@code org.apache.commons.lang3.tuple.Triple} and {@code ImmutableTriple}.
 *
 * @param left   the left element
 * @param middle the middle element
 * @param right  the right element
 * @param <L> the type of the left element
 * @param <M> the type of the middle element
 * @param <R> the type of the right element
 */
public record Triple<L, M, R>(L left, M middle, R right) {

  public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
    return new Triple<>(left, middle, right);
  }

  public L getLeft() {
    return left;
  }

  public M getMiddle() {
    return middle;
  }

  public R getRight() {
    return right;
  }
}
