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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;

import org.hippoecm.repository.api.HippoNodeType;

public class ViewVirtualProvider extends MirrorVirtualProvider
{
    
    protected class ViewNodeId extends MirrorNodeId {
        private static final long serialVersionUID = 1L;
        
        boolean singledView;
        Map<Name,String> view; // must be immutable
        
        ViewNodeId(NodeId parent, NodeId upstream, Name name, Map<Name,String> view, boolean singledView) {
            super(ViewVirtualProvider.this, parent, name, upstream);
            this.view = view;
            this.singledView = singledView;
        }
    }

    ViewVirtualProvider() throws RepositoryException {
        super();
    }

    private Name handleName;    
    private Name requestName;

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        requestName = resolveName(HippoNodeType.NT_REQUEST);        
    }
    
    @Override
    public NodeState populate(NodeState state) throws RepositoryException {
        String docbase = getProperty(state.getNodeId(), docbaseName)[0];
        NodeState dereference = getNodeState(new NodeId(new UUID(docbase)));
        Map<Name,String> view = new HashMap<Name,String>();
        if(dereference != null) {
            for(Iterator iter = dereference.getChildNodeEntries().iterator(); iter.hasNext(); ) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                if(this.match(view, entry.getId())) {
                    NodeId childNodeId = this . new ViewNodeId(state.getNodeId(),entry.getId(),entry.getName(),view, false);
                    state.addChildNodeEntry(entry.getName(), childNodeId);
                }
            }
        }
        return state;
    }

    protected boolean match(Map<Name,String> view, NodeId candidate) {
        for(Map.Entry<Name,String> entry : view.entrySet()) {
            Name facet = entry.getKey();
            String value = entry.getValue();
            String[] matching = getProperty(candidate, facet);
            if(matching != null && matching.length > 0) {
                if(value != null && !value.equals("") && !value.equals("*")) {
                    int i;
                    for(i=0; i<matching.length; i++)
                        if(matching[i].equals(value))
                            break;
                    if(i == matching.length)
                        return false;
                }
            }
        }
        return true;
    }

    protected void populateChildren(NodeId nodeId, NodeState state, NodeState upstream) {
        ViewNodeId viewId = (ViewNodeId) nodeId;
        boolean isHandle = state.getNodeTypeName().equals(handleName);
        for(Iterator iter = upstream.getChildNodeEntries().iterator(); iter.hasNext(); ) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            if (!isHandle || match(viewId.view, entry.getId())) {
                /*
                 * below we check on the entry's nodestate wether the node type is hippo:request, 
                 * because we do not show these nodes in the facetselects in mode single.
                 * Since match() already populates the nodestates of the child entries, this won't impose
                 * extra performance hit
                 */ 
                if (isHandle && viewId.singledView && getNodeState(entry.getId()).getNodeTypeName().equals(requestName)) {
                    continue;
                } else {
                    ViewNodeId childNodeId = new ViewNodeId(nodeId, entry.getId(), entry.getName(), viewId.view,
                            viewId.singledView);
                    state.addChildNodeEntry(entry.getName(), childNodeId);
                    if (isHandle && viewId.singledView) {
                        // stop after first match because single hippo document view
                        break;
                    }
                }
            }
        }
    }
}
