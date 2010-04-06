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
package org.hippoecm.repository.jackrabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.OrderBy;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetResultSetProvider extends HippoVirtualProvider
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Logger log = LoggerFactory.getLogger(HippoVirtualProvider.class);

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    public class FacetResultSetNodeId extends HippoNodeId {
        private static final long serialVersionUID = 1L;
        String queryname;
        String docbase;
        String[] search;
        List<KeyValue<String, String>> preparedSearch;
        List<FacetRange> currentRanges;
        String facetedFiltersString;
        
        // the list of properties to order the resultset on
        List<OrderBy> orderByList;
        
        long count;
        // default limit = 1000
        int limit = 1000;
        
        FacetResultSetNodeId(NodeId parent, StateProviderContext context, Name name) {
            super(FacetResultSetProvider.this, parent, context, name);
        }
        public FacetResultSetNodeId(NodeId parent, StateProviderContext context, Name name, String queryname, String docbase, String[] search, long count) {
            super(FacetResultSetProvider.this, parent, context, name);
            this.queryname = queryname;
            this.docbase = docbase;
            this.search = search;
            this.count = count;
        }
        
        public FacetResultSetNodeId(NodeId parent, StateProviderContext context, Name name, String queryname, String docbase,
                List<KeyValue<String, String>> currentSearch, List<FacetRange> currentRanges, int count, String facetedFiltersString) {
            super(FacetResultSetProvider.this, parent, context, name);
            this.queryname = queryname;
            this.docbase = docbase;
            this.preparedSearch = currentSearch;
            this.currentRanges = currentRanges;
            this.count = count;
            this.facetedFiltersString = facetedFiltersString;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public void setOrderByList(List<OrderBy> orderByList) {
            this.orderByList = orderByList;
        }
    }

    ViewVirtualProvider subNodesProvider;
    FacetedNavigationEngine<FacetedNavigationEngine.Query, FacetedNavigationEngine.Context> facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name countName;
    PropDef countPropDef;
    PropDef primaryTypePropDef;

    public FacetResultSetProvider()
        throws IllegalNameException, NamespaceException, RepositoryException {
        super();
    }

    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
    }

    @Override
    protected void initialize() throws RepositoryException {
        countName = resolveName(HippoNodeType.HIPPO_COUNT);
        countPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETRESULT), countName);
        primaryTypePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETRESULT), countName);
        this.subNodesProvider = (ViewVirtualProvider) lookup(ViewVirtualProvider.class.getName());
        register(null, resolveName(HippoNodeType.NT_FACETRESULT));
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws IllegalNameException, NamespaceException {
        FacetResultSetNodeId nodeId = (FacetResultSetNodeId) state.getNodeId();
        String queryname = nodeId.queryname;
        String docbase = nodeId.docbase;
        String facetedFiltersString = nodeId.facetedFiltersString;
        String[] search = nodeId.search;
        List<FacetRange> currentRanges = nodeId.currentRanges;
        long count = nodeId.count;
        
        Map<Name,String> inheritedFilter = null;
        boolean singledView = false;
        LinkedHashMap<Name,String> view = null;
        LinkedHashMap<Name,String> order = null;
        
        if (state.getParentId()!=null && state.getParentId() instanceof IFilterNodeId) {
            IFilterNodeId filterNodeId = (IFilterNodeId)state.getParentId();
            if(filterNodeId.getView() != null) {
                inheritedFilter = new LinkedHashMap<Name,String>(filterNodeId.getView());
                view =  new LinkedHashMap<Name,String>(filterNodeId.getView());
            }
            if(filterNodeId.getOrder() != null) {
                order = new LinkedHashMap<Name,String>(filterNodeId.getOrder());
            }
            singledView = filterNodeId.isSingledView();
        }
        
        /*
         * if we have a preparedSearch, we do not need to get it from the search[] 
         */
        List<KeyValue<String, String>> currentFacetQuery = nodeId.preparedSearch;
        
        if(currentFacetQuery == null) {
            currentFacetQuery = new ArrayList<KeyValue<String,String>>();
            for(int i=0; search != null && i < search.length; i++) {
                Matcher matcher = facetPropertyPattern.matcher(search[i]);
                if(matcher.matches() && matcher.groupCount() == 2) {
                    try {
                        currentFacetQuery.add(new FacetKeyValue(resolvePath(matcher.group(1)).toString(), matcher.group(2)));
                    } catch(IllegalNameException ex) {
                        log.error("Could not resolve path for: '{}'. Return unpopulated state", matcher.group(1));
                        return state;
                    } catch(NamespaceException ex) {
                        log.error("Could not resolve path for: '{}'. Return unpopulated state", matcher.group(1));
                        return state;
                    } catch(MalformedPathException ex) {
                        log.error("Could not resolve path for: '{}'. Return unpopulated state", matcher.group(1));
                        return state;
                    }
                }
            }
        }
        StringBuilder initialQueryString = new StringBuilder();
        if(docbase != null) {
            initialQueryString.append(docbase);
        }
        if(facetedFiltersString != null) {
            initialQueryString.append(FacetedNavigationEngine.Query.DOCBASE_FILTER_DELIMITER).append(facetedFiltersString);
        }
        FacetedNavigationEngine.Query initialQuery = facetedEngine.parse(initialQueryString.toString());

        HitsRequested hitsRequested = new HitsRequested();
        hitsRequested.setResultRequested(true);
        hitsRequested.setLimit(nodeId.limit);
        hitsRequested.setOffset(0);
        hitsRequested.addOrderBy(nodeId.orderByList);
          
        FacetedNavigationEngine.Result facetedResult;
        long t1 = 0;
        if(log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }
        Map<String, String> filters = null;
        if(inheritedFilter != null) {
            filters = new HashMap<String,String>();
            for(Entry<Name, String> entry : inheritedFilter.entrySet()) {
                filters.put(entry.getKey().toString(), entry.getValue());
            }
        }
        facetedResult = facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery, currentRanges, (context != null && context.getArgument() != null ? facetedEngine.parse(context.getArgument()) : null), null, filters,
                                           hitsRequested);
        if(log.isDebugEnabled()) {
            FacetedNavigationModulesTimer.log.debug("Creating facetedResultSet took '{}' ms for '{}' number of results.", (System.currentTimeMillis() - t1),  facetedResult.length());
        }
        count = facetedResult.length();

        PropertyState propState = createNew(NameConstants.JCR_PRIMARYTYPE, state.getNodeId());
        propState.setType(PropertyType.NAME);
        propState.setDefinitionId(primaryTypePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(resolveName(HippoNodeType.NT_FACETRESULT))} );
        propState.setMultiValued(false);
        state.addPropertyName(NameConstants.JCR_PRIMARYTYPE);

        propState = createNew(countName, state.getNodeId());
        propState.setType(PropertyType.LONG);
        propState.setDefinitionId(countPropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(count) });
        propState.setMultiValued(false);
        state.addPropertyName(countName);

        for(NodeId upstream : facetedResult) {
            if(upstream == null)
                continue;
            /* The next statements are painfull performance wise.
             * Only to obtain the child node name, we have to retrieve the parent state.
             */
            NodeState upstreamState = getNodeState(upstream);
            if(upstreamState == null)
                continue;
            NodeId parentId = upstreamState.getParentId();
            if(parentId == null)
                continue;
	    NodeState parentNodeState = getNodeState(parentId);
            if(parentNodeState == null || !parentNodeState.hasChildNodeEntry(upstream))
                continue;
            Name name = parentNodeState.getChildNodeEntry(upstream).getName();
            /*
             *  TODO : inherit all the 
             *  this.view = view;
             *  this.order = order;
             *  this.singledView = singledView;
             *  
             *  from parent NodeId, which is NOT a ViewNodeId, nor a MirrorNodeId
             */
            state.addChildNodeEntry(name, subNodesProvider . new ViewNodeId(state.getNodeId(), upstream, context, name, view, order , singledView));
        }

        return state;
    }
}
