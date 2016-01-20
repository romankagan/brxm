/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.restapi.content.search;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.restapi.content.ResourceContext;
import org.hippoecm.hst.restapi.content.linking.RestApiLinkCreator;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;

public class SearchResult {

    @JsonProperty("offset")
    public long offset;

    @JsonProperty("max")
    public long max;

    @JsonProperty("count")
    public long count;

    @JsonProperty("total")
    public long total;

    @JsonProperty("more")
    public boolean more;

    @JsonProperty("items")
    public SearchResultItem[] items;

    public void populate(final int offset, final int max,
                         final QueryResult queryResult,
                         final ResourceContext context,
                         final String expectedNodeType) throws RepositoryException {
        final List<SearchResultItem> itemArrayList = new ArrayList<>();
        final HitIterator iterator = queryResult.getHits();
        final Session session = context.getRequestContext().getSession();
        final RestApiLinkCreator restApiLinkCreator = context.getRestApiLinkCreator();
        while (iterator.hasNext()) {
            final Hit hit = iterator.nextHit();
            final String uuid = hit.getSearchDocument().getContentId().toIdentifier();
            final Node node = session.getNodeByIdentifier(uuid);
            if (!node.isNodeType(expectedNodeType)) {
                throw new IllegalStateException(String.format("Expected node of type '%s' but was '%s'.",
                        expectedNodeType, node.getPrimaryNodeType().getName()));
            }

            final HstLink hstLink = context.getRequestContext().getHstLinkCreator().create(node, context.getRequestContext());
            final SearchResultItem item = new SearchResultItem(node.getName(), uuid, restApiLinkCreator.convert(context, node.getIdentifier(), hstLink));
            itemArrayList.add(item);

            this.offset = offset;
            this.max = max;
            count = itemArrayList.size();
            total = queryResult.getTotalHitCount();
            more = (offset + count) < total;
            items = new SearchResultItem[(int)count];
            itemArrayList.toArray(items);
        }
    }
}