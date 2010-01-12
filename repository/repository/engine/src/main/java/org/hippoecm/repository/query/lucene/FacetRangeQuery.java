/*
 *  Copyright 2008 Hippo.
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

import java.util.List;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.FacetRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetRangeQuery {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FacetRangeQuery.class);

    /**
     * The lucene query
     */
    private BooleanQuery query;

    public FacetRangeQuery(List<FacetRange> rangeQuery, NamespaceMappings nsMappings, ServicingSearchIndex searchIndex) {
        this.query = new BooleanQuery(true);

        if (rangeQuery != null) {
            for(FacetRange facetRange : rangeQuery) {
                try {
                    String internalName = ServicingNameFormat.getInteralPropertyPathName(nsMappings, facetRange.getNamespacedProperty());
                    
                    RangeFields rangeFields = new RangeFields(internalName, facetRange);
                    Query constraint = null;
                    if(rangeFields.begin == null && rangeFields.end == null) {
                        // no range constraints: short is to just make sure the property exists, this is much faster
                        constraint = new FacetPropExistsQuery(rangeFields.compoundInternalName).getQuery();
                    } else {
                        // rangeFields.begin and rangeFields.end are allowed to be null in ConstantScoreRangeQuery
                        constraint = new ConstantScoreRangeQuery(rangeFields.internalFacetName, rangeFields.begin, rangeFields.end, true, false);
                    }
                    query.add(constraint, Occur.MUST);
                   
                } catch (IllegalNameException e) {
                    log.error(e.toString());
                } catch (IllegalArgumentException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }
}
