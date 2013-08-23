/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.jackrabbit.core.query.lucene.MultiIndexReader;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.OpenBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiReaderQueryFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(MultiReaderQueryFilter.class);

    private static final class CachedBitSet extends OpenBitSet {

        private final int maxDoc;

        CachedBitSet(int maxDoc) {
            super(maxDoc);
            this.maxDoc = maxDoc;
        }

        boolean isValid(IndexReader reader) {
            return maxDoc == reader.maxDoc();
        }
    }

    private static boolean requiresMultiIndexReader(Query query) {
        if (query instanceof BooleanQuery) {
            for (BooleanClause clause : ((BooleanQuery) query).getClauses()) {
                final Query subQuery = clause.getQuery();
                final String strSubQuery = subQuery.toString();
                if (strSubQuery.startsWith("ParentAxisQuery")
                        || strSubQuery.startsWith("ChildAxisQuery")
                        || strSubQuery.startsWith("DescendantSelfAxisQuery")) {
                    return true;
                }
                if (requiresMultiIndexReader(subQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final Map<IndexReader, CachedBitSet> cache = Collections.synchronizedMap(
            new WeakHashMap<IndexReader, CachedBitSet>());
    private final Query query;
    private final boolean disectMultiIndex;


    public MultiReaderQueryFilter(final Query query) {
        this.query = query;
        disectMultiIndex = !requiresMultiIndexReader(query);
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
        if (disectMultiIndex && reader instanceof MultiIndexReader) {
            MultiIndexReader multiIndexReader = (MultiIndexReader) reader;

            IndexReader[] indexReaders = multiIndexReader.getIndexReaders();
            DocIdSet[] docIdSets = new DocIdSet[indexReaders.length];
            int[] maxDocs = new int[indexReaders.length];
            for (int i = 0; i < indexReaders.length; i++) {
                IndexReader subReader = indexReaders[i];
                docIdSets[i] = getIndexReaderDocIdSet(subReader);
                maxDocs[i] = subReader.maxDoc();
            }

            return new MultiDocIdSet(docIdSets, maxDocs);
        }
        return getIndexReaderDocIdSet(reader);
    }

    private DocIdSet getIndexReaderDocIdSet(final IndexReader reader) throws IOException {
        CachedBitSet docIdSet = cache.get(reader);
        if (docIdSet == null || !docIdSet.isValid(reader)) {
            docIdSet = createAndCacheDocIdSet(reader);
        }
        return docIdSet;
    }

    private synchronized CachedBitSet createAndCacheDocIdSet(IndexReader reader) throws IOException {
        CachedBitSet docIdSet;
        docIdSet = cache.get(reader);
        if (docIdSet != null && docIdSet.isValid(reader)) {
            return docIdSet;
        }
        docIdSet = createDocIdSet(reader);
        cache.put(reader, docIdSet);
        return docIdSet;
    }

    private CachedBitSet createDocIdSet(IndexReader reader) throws IOException {
        final CachedBitSet bits = new CachedBitSet(reader.maxDoc());

        long start = System.currentTimeMillis();

        new IndexSearcher(reader).search(query, new AbstractHitCollector() {

            @Override
            public final void collect(int doc, float score) {
                bits.set(doc);  // set bit for hit
            }
        });

        long docIdSetCreationTime = System.currentTimeMillis() - start;
        log.info("Creating authorization doc id set took {} ms.", String.valueOf(docIdSetCreationTime));

        return bits;
    }

}
