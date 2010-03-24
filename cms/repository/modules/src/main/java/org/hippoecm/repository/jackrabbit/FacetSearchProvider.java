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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetSearchProvider extends AbstractFacetSearchProvider
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public FacetSearchProvider()
        throws RepositoryException
    {
        super();
    }

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        subSearchProvider = (FacetSubSearchProvider) lookup(FacetSubSearchProvider.class.getName());
        subNodesProvider  = (FacetResultSetProvider) lookup(FacetResultSetProvider.class.getName());
        virtualNodeName = resolveName(HippoNodeType.NT_FACETSUBSEARCH);
        register(resolveName(HippoNodeType.NT_FACETSEARCH), virtualNodeName);
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Cannot populate top facetsearch node");
    }
}
