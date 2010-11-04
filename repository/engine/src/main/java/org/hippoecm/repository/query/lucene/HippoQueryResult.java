/*
 *  Copyright 2010 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

import java.io.IOException;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.AbstractQueryImpl;
import org.apache.jackrabbit.core.query.lucene.ExcerptProvider;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.QueryResultImpl;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.apache.lucene.search.Query;

public class HippoQueryResult extends QueryResultImpl {
    private int totalSize;

    private final Query query;
    /**
     * The relative paths of properties to use for ordering the result set.
     */
    protected final Path[] orderProps;
    /**
     * The order specifier for each of the order properties.
     */
    protected final boolean[] orderSpecs;

    public HippoQueryResult(SearchIndex index,
                            ItemManager itemMgr,
                            SessionImpl session,
                            AccessManager accessMgr,
                            AbstractQueryImpl queryImpl,
                            Query query,
                            ColumnImpl[] columns,
                            Path[] orderProps,
                            boolean[] orderSpecs,
                            boolean documentOrder,
                            long offset,
                            long limit) throws RepositoryException {
        super(index, itemMgr, session, accessMgr, queryImpl, null, columns, documentOrder, offset, limit);
        this.query = query;
        this.orderProps = orderProps;
        this.orderSpecs = orderSpecs;
        // if document order is requested get all results right away
        getResults(docOrder ? Integer.MAX_VALUE : index.getResultFetchSize());
    }

    /**
     * {@inheritDoc}
     */
    protected MultiColumnQueryHits executeQuery(long resultFetchHint)
            throws IOException {
        MultiColumnQueryHits hits = index.executeQuery(session, queryImpl, query,
                orderProps, orderSpecs, resultFetchHint);
        totalSize = hits.getSize();
        return hits;
    }

    /**
     * {@inheritDoc}
     */
    protected ExcerptProvider createExcerptProvider() throws IOException {
        return index.createExcerptProvider(query);
    }

    public int getSizeTotal() {
        return totalSize;
    }
}
