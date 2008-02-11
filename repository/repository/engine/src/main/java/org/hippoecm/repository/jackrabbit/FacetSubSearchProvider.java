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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetSubSearchProvider extends AbstractFacetSearchProvider
{
    final static private String SVN_ID = "$Id$";

    PropDef primaryTypePropDef;

    FacetSubSearchProvider()
        throws RepositoryException
    {
    }

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        subSearchProvider = this;
        subNodesProvider  = (FacetResultSetProvider) lookup("org.hippoecm.repository.jackrabbit.FacetResultSetProvider");
        primaryTypePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), countName);
        virtualNodeName = resolveName(HippoNodeType.NT_FACETSUBSEARCH);
        register(null, virtualNodeName);
    }

    public NodeState populate(NodeState state) throws RepositoryException {
        super.populate(state);

        PropertyState propState = createNew(NameConstants.JCR_PRIMARYTYPE, state.getNodeId());
        propState.setType(PropertyType.STRING);
        propState.setDefinitionId(primaryTypePropDef.getId());
        propState.setValues(new InternalValue[] { InternalValue.create(HippoNodeType.NT_FACETSUBSEARCH) });
        propState.setMultiValued(false);
        state.addPropertyName(NameConstants.JCR_PRIMARYTYPE);

        return state;
    }
}
