package org.hippoecm.repository.jackrabbit.facetnavigation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.ParsedFacet;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.jackrabbit.FacetKeyValue;
import org.hippoecm.repository.jackrabbit.FacetResultSetProvider;
import org.hippoecm.repository.jackrabbit.HippoNodeId;
import org.hippoecm.repository.jackrabbit.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetSubNavigationProvider extends AbstractFacetNavigationProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Logger log = LoggerFactory.getLogger(FacetSubNavigationProvider.class);
    
    protected FacetsAvailableNavigationProvider facetsAvailableNavigationProvider = null;
    protected FacetResultSetProvider subNodesProvider = null;

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        facetsAvailableNavigationProvider = (FacetsAvailableNavigationProvider) lookup(FacetsAvailableNavigationProvider.class.getName());
        subNodesProvider  = (FacetResultSetProvider) lookup(FacetResultSetProvider.class.getName());
        virtualNodeName = resolveName(FacNavNodeType.NT_FACETSUBNAVIGATION);
        register(null, virtualNodeName);
    }
 
    @Override
    public NodeState populate(NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        if (nodeId instanceof FacetNavigationNodeId) {
            FacetNavigationNodeId facetNavigationNodeId = (FacetNavigationNodeId)nodeId;
            List<KeyValue<String, String>> currentSearch = facetNavigationNodeId.currentSearch;
            List<FacetRange> currentRanges = facetNavigationNodeId.currentRanges;
            String[] availableFacets = facetNavigationNodeId.availableFacets;
            String[] facetNodeNames = facetNavigationNodeId.facetNodeNames;
            String docbase = facetNavigationNodeId.docbase;
            List<KeyValue<String, String>> usedFacetValueCombis = facetNavigationNodeId.usedFacetValueCombis;

            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(false);
            hitsRequested.setFixedDrillPath(false);
          
            PropertyState propState = createNew(countName, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setDefinitionId(subCountPropDef.getId());
            propState.setValues(new InternalValue[] { InternalValue.create(facetNavigationNodeId.count) });
            propState.setMultiValued(false);
            state.addPropertyName(countName);
            
            if(facetNavigationNodeId.stopSubNavigation) {
                // we are done with this facet - value combination
                return state;
            }
            int i = 0;
            for(String facet : availableFacets){ 
                String configuredNodeName = null;
                if(facetNodeNames != null && facetNodeNames[i] != null && !"".equals(facetNodeNames[i])) {
                    configuredNodeName = facetNodeNames[i];
                }
                ParsedFacet parsedFacet;
                try {
                    parsedFacet = new ParsedFacet(facet, configuredNodeName, this);
                } catch (Exception e) {
                    log.warn("Malformed facet range configuration '{}'. Valid format is "+VALID_RANGE_EXAMPLE,
                                    facet);
                    return state;
                }

                i++;

                Name childName = resolveName(NodeNameCodec.encode(parsedFacet.getDisplayFacetName()));
                FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsAvailableNavigationProvider,state.getNodeId(), childName);
                for(String value : facetNavigationNodeId.ancestorAndSelfUsedCombinations) {
                    KeyValue<String,String> facetValueCombi = new FacetKeyValue(facet, value);
                    if(usedFacetValueCombis.indexOf(facetValueCombi) > -1) {
                           /*
                            * the exact facet value combination is already populated before in the tree. We populate the key-value
                            * combi one more time, to have a consistent faceted navigation view, but, after that, we do not populate the 
                            * childs anymore, also to avoid recursion
                            */
                           childNodeId.stopSubNavigation = true;
                           break;  
                        }
                }
                childNodeId.availableFacets = availableFacets;
                childNodeId.facetNodeNames = facetNodeNames;
                childNodeId.currentFacet = facet;
                childNodeId.ancestorAndSelfUsedCombinations = facetNavigationNodeId.ancestorAndSelfUsedCombinations;
                childNodeId.docbase = docbase;
                childNodeId.currentSearch = currentSearch;
                childNodeId.currentRanges = currentRanges;
                childNodeId.view = facetNavigationNodeId.view;
                childNodeId.order = facetNavigationNodeId.order;
                childNodeId.singledView = facetNavigationNodeId.singledView;
                childNodeId.limit = facetNavigationNodeId.limit;
                childNodeId.orderByList = facetNavigationNodeId.orderByList;
                
                childNodeId.usedFacetValueCombis = new ArrayList<KeyValue<String,String>>(usedFacetValueCombis);
                state.addChildNodeEntry(childName, childNodeId);
            }
          
            // add child node resultset:
            Name resultSetChildName = resolveName(HippoNodeType.HIPPO_RESULTSET);
            FacetResultSetProvider.FacetResultSetNodeId childNodeId;
            childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), resultSetChildName, null,
                    docbase, currentSearch, currentRanges, facetNavigationNodeId.count);
            childNodeId.setLimit(facetNavigationNodeId.limit);
            childNodeId.setOrderByList(facetNavigationNodeId.orderByList);
            
            state.addChildNodeEntry(resultSetChildName, childNodeId);
        }
        return state;
    }
    
    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setDefinitionId(lookupNodeDef(getNodeState(parentId), resolveName(FacNavNodeType.NT_FACETSUBNAVIGATION),
                nodeId.name).getId());
        state.setNodeTypeName(resolveName(FacNavNodeType.NT_FACETSUBNAVIGATION));

        return populate(state);
    }
}
