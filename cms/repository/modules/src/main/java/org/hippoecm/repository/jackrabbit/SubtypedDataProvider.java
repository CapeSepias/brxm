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

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import org.hippoecm.repository.api.HippoNodeType;

public class SubtypedDataProvider implements DataProviderModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    protected final Logger log = LoggerFactory.getLogger(SubtypedDataProvider.class);

    public SubtypedDataProvider() {
    }

    public void initialize(DataProviderContext context) {
        NodeTypeRegistry ntReg = context.getNodeTypeRegistry();
        NameFactory nameFactory = NameFactoryImpl.getInstance();

        Map<Name, String> subtyping = new TreeMap<Name, String>();
        try {

            subtyping.put(context.getQName(HippoNodeType.NT_FACETSELECT), "org.hippoecm.repository.jackrabbit.FacetSelectProvider");
        } catch (IllegalNameException ex) {
            log.warn("Error registering subnodes: " + ex.getClass().getName() + ": " + ex.getMessage());
        } catch (NamespaceException ex) {
            log.warn("Error registering subnodes: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        try {
            subtyping.put(context.getQName(HippoNodeType.NT_FACETSEARCH), "org.hippoecm.repository.jackrabbit.FacetSearchProvider");
        } catch (IllegalNameException ex) {
            log.warn("Error registering subnodes: " + ex.getClass().getName() + ": " + ex.getMessage());
        } catch (NamespaceException ex) {
            log.warn("Error registering subnodes: " + ex.getClass().getName() + ": " + ex.getMessage());
        }

        Name[] nodeTypes = ntReg.getRegisteredNodeTypes();
        for (int i = 0; i < nodeTypes.length; i++) {
            try {
                NodeTypeDef nodeType = ntReg.getNodeTypeDef(nodeTypes[i]);
                Name[] superTypes = nodeType.getSupertypes();
                int superTypeMatch = -1;
                for (int j = 0; j < superTypes.length; j++) {
                    if (subtyping.containsKey(superTypes[j])) {
                        superTypeMatch = j;
                        break;
                    }
                }
                if (superTypeMatch >= 0) {
                    if(context.lookupProvider(nodeTypes[i]) == null) {
                        context.registerProvider(nodeTypes[i], context.lookupProvider(subtyping.get(superTypes[superTypeMatch])));
                    }
                }
            } catch (NoSuchNodeTypeException ex) {
                log.warn("Error registering subnodes: " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }
}
