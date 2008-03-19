/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.query.lucene.DoubleField;
import org.apache.jackrabbit.core.query.lucene.LongField;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFacetSearchProvider extends HippoVirtualProvider {
    
    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    class FacetSearchNodeId extends HippoNodeId {
        String queryname;
        String docbase;
        String[] facets;
        String[] search;
        long count;

        FacetSearchNodeId(HippoVirtualProvider provider, NodeId parent, Name name) {
            super(provider, parent, name);
        }
    }

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    protected FacetSubSearchProvider subSearchProvider = null;
    protected FacetResultSetProvider subNodesProvider = null;

    FacetedNavigationEngine facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name querynameName;
    Name docbaseName;
    Name facetsName;
    Name searchName;
    Name countName;

    PropDef querynamePropDef;
    PropDef docbasePropDef;
    PropDef facetsPropDef;
    PropDef searchPropDef;
    PropDef countPropDef;

    Name virtualNodeName;

    protected AbstractFacetSearchProvider() {
        super();
    }

    @Override
    void initialize(HippoLocalItemStateManager stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.facetedEngine;
        this.facetedContext = stateMgr.facetedContext;
        stateMgr.registerProviderProperty(countName);
    }

    @Override
    protected void initialize() throws RepositoryException {
        querynameName = resolveName(HippoNodeType.HIPPO_QUERYNAME);
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(HippoNodeType.HIPPO_FACETS);
        searchName = resolveName(HippoNodeType.HIPPO_SEARCH);
        countName = resolveName(HippoNodeType.HIPPO_COUNT);

        querynamePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), querynameName);
        docbasePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), docbaseName);
        facetsPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), facetsName);
        searchPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), searchName);
        countPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), countName);
    }

    @Override
    public NodeState populate(NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String queryname;
        String docbase;
        String[] facets;
        String[] search;
        long count = 0;
        if (nodeId instanceof FacetSearchNodeId) {
            FacetSearchNodeId facetSearchNodeId = (FacetSearchNodeId) nodeId;
            queryname = facetSearchNodeId.queryname;
            docbase = facetSearchNodeId.docbase;
            facets = facetSearchNodeId.facets;
            search = facetSearchNodeId.search;
            count = facetSearchNodeId.count;
        } else {
            queryname = getProperty(nodeId, querynameName)[0];
            docbase = getProperty(nodeId, docbaseName)[0];
            facets = getProperty(nodeId, facetsName);
            search = getProperty(nodeId, searchName);
        }

        if (facets != null && facets.length > 0) {
            Map<String, Map<String, org.hippoecm.repository.FacetedNavigationEngine.Count>> facetSearchResultMap;
            facetSearchResultMap = new TreeMap<String, Map<String, FacetedNavigationEngine.Count>>();
            Map<String, FacetedNavigationEngine.Count> facetSearchResult;
            facetSearchResult = new TreeMap<String, FacetedNavigationEngine.Count>();
            facetSearchResultMap.put(resolveName(facets[0]).toString(), facetSearchResult);

            Map<String, String> currentFacetQuery = new TreeMap<String, String>();
            for (int i = 0; search != null && i < search.length; i++) {
                Matcher matcher = facetPropertyPattern.matcher(search[i]);
                if (matcher.matches() && matcher.groupCount() == 2) {
                    try {
                        currentFacetQuery.put(resolveName(matcher.group(1)).toString(), matcher.group(2));
                    } catch (IllegalNameException ex) {
                        // FIXME: log a very serious error
                        return state;
                    } catch (NamespaceException ex) {
                        // FIXME: log a very serious error
                        return state;
                    }
                }
            }
            FacetedNavigationEngine.Query initialQuery;
            initialQuery = facetedEngine.parse(docbase);

            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(false);

            FacetedNavigationEngine.Result facetedResult;
            long t1 = 0, t2;
            if (log.isDebugEnabled())
                t1 = System.currentTimeMillis();
            facetedResult = facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery, null,
                    facetSearchResultMap, null, hitsRequested);
            if (log.isDebugEnabled()) {
                t2 = System.currentTimeMillis();
                log.debug("facetsearch turnaround=" + (t2 - t1));
            }
            count = facetedResult.length();

            PropertyState propState = createNew(countName, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setDefinitionId(countPropDef.getId());
            propState.setValues(new InternalValue[] { InternalValue.create(count) });
            propState.setMultiValued(false);
            state.addPropertyName(countName);

            for (Map.Entry<String, FacetedNavigationEngine.Count> facetValue : facetSearchResult.entrySet()) {
                if (facetValue.getKey().length() > 1) {
                    String[] newFacets = new String[Math.max(0, facets.length - 1)];
                    if (facets.length > 1) {
                        System.arraycopy(facets, 1, newFacets, 0, facets.length - 1);
                    }
                    String[] newSearch = new String[search != null ? search.length + 1 : 1];
                    if (search != null && search.length > 0) {
                        System.arraycopy(search, 0, newSearch, 0, search.length);
                    }
                    // nextTerm is the next facet value to search for (skip last char because this is the facet type constant)
                    String nextFacet = facetValue.getKey().substring(0, facetValue.getKey().length() - 1);
                    Character facetTypeConstant = facetValue.getKey().charAt(facetValue.getKey().length() - 1);
                    ;
                    if (facets[0].indexOf("#") == -1) {
                        newSearch[newSearch.length - 1] = "@" + facets[0] + "='" + nextFacet + "'";
                    } else {
                        newSearch[newSearch.length - 1] = "@" + facets[0].substring(0, facets[0].indexOf("#")) + "='"
                                + nextFacet + "'" + facets[0].substring(facets[0].indexOf("#"));
                    }
                    try {
                        String name = getDisplayName(nextFacet, facetTypeConstant);
                        Name childName = resolveName(ISO9075Helper.encodeLocalName(name));
                        FacetSearchNodeId childNodeId = new FacetSearchNodeId(subSearchProvider, state.getNodeId(),
                                childName);
                        state.addChildNodeEntry(childName, childNodeId);
                        childNodeId.queryname = queryname;
                        childNodeId.docbase = docbase;
                        childNodeId.facets = newFacets;
                        childNodeId.search = newSearch;
                        childNodeId.count = facetValue.getValue().count;
                    } catch (RepositoryException ex) {
                        log.warn("cannot add virtual child in facet search: " + ex.getMessage());
                    }
                } else {
                    log.error("facet value with only facet type constant found. Skip result");
                }
            }
        }

        FacetResultSetProvider.FacetResultSetNodeId childNodeId;
        Name resultSetChildName = resolveName(HippoNodeType.HIPPO_RESULTSET);
        childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), resultSetChildName, queryname,
                docbase, search, count);
        state.addChildNodeEntry(resultSetChildName, childNodeId);

        return state;
    }

    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        FacetSearchNodeId searchNodeId = (FacetSearchNodeId) nodeId;
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setDefinitionId(lookupNodeDef(getNodeState(parentId), resolveName(HippoNodeType.NT_FACETSUBSEARCH),
                nodeId.name).getId());
        state.setNodeTypeName(resolveName(HippoNodeType.NT_FACETSUBSEARCH));

        PropertyState propState = createNew(querynameName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(querynamePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.queryname) });
        propState.setMultiValued(false);
        state.addPropertyName(querynameName);

        propState = createNew(docbaseName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(docbasePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.docbase) });
        propState.setMultiValued(false);
        state.addPropertyName(docbaseName);

        propState = createNew(facetsName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(facetsPropDef.getId());
        propState.setValues(InternalValue.create(searchNodeId.facets));
        propState.setMultiValued(true);
        state.addPropertyName(facetsName);

        propState = createNew(searchName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(searchPropDef.getId());
        propState.setValues(InternalValue.create(searchNodeId.search));
        propState.setMultiValued(true);
        state.addPropertyName(searchName);

        propState = createNew(countName, nodeId);
        propState.setType(PropertyType.LONG);
        propState.setDefinitionId(countPropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.count) });
        propState.setMultiValued(false);
        state.addPropertyName(countName);

        return populate(state);
    }

    private String getDisplayName(String nextFacet, Character facetTypeConstant) {
        int type = 0;
        while (type < FacetTypeConstants.POSTFIXES.length) {
            if (facetTypeConstant.equals(FacetTypeConstants.POSTFIXES[type])) {
                break;
            }
            type++;
        }
        switch (type) {
        case FacetTypeConstants.STRING:
            return nextFacet;
        case FacetTypeConstants.BOOLEAN:
            return nextFacet;
        case FacetTypeConstants.LONG:
            return String.valueOf(LongField.stringToLong(nextFacet));
        case FacetTypeConstants.DOUBLE:
            return String.valueOf(DoubleField.stringToDouble(nextFacet));
        case FacetTypeConstants.DATE:
            return nextFacet;
            // TODO : configurable representation of date nodes + resolution
            //try {
                //return String.valueOf(DateTools.stringToDate(nextFacet).toString());
            //} catch (ParseException e) {
            //    log.error("error parsing date " + e.getMessage());
            //    return nextFacet;
            //}
            //return String.valueOf(DateField.stringToDate(nextFacet).toString());
        default:
            return nextFacet;
        }

    }
}
