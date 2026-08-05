/*
 * Copyright 2026 SiteNetSoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sitenetsoft.olinguito.server.core.responses;

import java.net.URI;
import java.net.URISyntaxException;

import org.sitenetsoft.olinguito.commons.api.data.EntityCollection;
import org.sitenetsoft.olinguito.server.api.uri.queryoption.SystemQueryOptionKind;

/**
 * Helper class for server-driven paging.
 * <p>
 * Server-driven paging allows the server to limit the number of entities returned
 * and provide a next link for clients to retrieve additional results.
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * EntityCollection entities = dataProvider.getEntities();
 * int pageSize = 10;
 * PagingHelper.applyPaging(entities, pageSize, rawRequestUri, skipToken);
 * // entities now contains at most pageSize items and has next link set if more exist
 * </pre>
 * </p>
 */
public final class PagingHelper {

  private PagingHelper() {
    // Utility class
  }

  /**
   * Applies server-side paging to an entity collection.
   * <p>
   * This method modifies the collection in place:
   * <ul>
   *   <li>Removes entities before the current page (based on skipToken)</li>
   *   <li>Limits the collection to pageSize entities</li>
   *   <li>Sets the next link if more entities exist</li>
   * </ul>
   * </p>
   *
   * @param entityCollection the collection to apply paging to (modified in place)
   * @param pageSize         maximum number of entities per page
   * @param rawRequestUri    the raw request URI (used to construct next link)
   * @param skipToken        the current skip token value, or null for first page
   * @return the page size actually used
   */
  public static int applyPaging(EntityCollection entityCollection, int pageSize,
      String rawRequestUri, String skipToken) {

    int page = getPageFromSkipToken(skipToken);
    int itemsToSkip = pageSize * page;

    // Skip items for previous pages
    if (itemsToSkip > 0 && itemsToSkip < entityCollection.getEntities().size()) {
      entityCollection.getEntities().subList(0, itemsToSkip).clear();
    }

    int remainingItems = entityCollection.getEntities().size();

    // Limit to page size
    if (remainingItems > pageSize) {
      entityCollection.getEntities().subList(pageSize, remainingItems).clear();
      // Set next link since more items exist
      entityCollection.setNext(createNextLink(rawRequestUri, page + 1, pageSize));
    }

    return pageSize;
  }

  /**
   * Creates a next link URI for server-driven paging.
   *
   * @param rawRequestUri the current request URI
   * @param nextPage      the next page number (0-based)
   * @param pageSize      the page size
   * @return the next link URI, or null if URI creation fails
   */
  public static URI createNextLink(String rawRequestUri, int nextPage, int pageSize) {
    // Remove existing skiptoken if present
    String nextLink = rawRequestUri.contains("?")
        ? rawRequestUri.replaceAll("(\\$|%24)skiptoken=[^&]*&?", "").replaceAll("[?&]$", "")
        : rawRequestUri;

    // Add separator
    nextLink += nextLink.contains("?") ? "&" : "?";

    // Add skiptoken in format: page*pageSize
    nextLink += SystemQueryOptionKind.SKIPTOKEN.toString().replace("$", "%24")
        + "=" + nextPage + "%2A" + pageSize;

    try {
      return new URI(nextLink);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Extracts the page number from a skip token.
   * <p>
   * Skip tokens are expected in the format "page*pageSize" (e.g., "2*10" for page 2, size 10).
   * </p>
   *
   * @param skipToken the skip token value
   * @return the page number, or 0 if skipToken is null or invalid
   */
  public static int getPageFromSkipToken(String skipToken) {
    if (skipToken != null && skipToken.contains("*")) {
      try {
        return Integer.parseInt(skipToken.substring(0, skipToken.indexOf('*')));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  /**
   * Extracts the page size from a skip token.
   *
   * @param skipToken the skip token value
   * @return the page size from the token, or 0 if not present or invalid
   */
  public static int getPageSizeFromSkipToken(String skipToken) {
    if (skipToken != null && skipToken.contains("*")) {
      try {
        return Integer.parseInt(skipToken.substring(skipToken.indexOf('*') + 1));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }
}
