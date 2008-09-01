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
package org.hippoecm.frontend.editor.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

public class TemplateEngine implements ITemplateEngine, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private ITypeStore typeStore;
    private JcrTemplateStore templateStore;
    private JcrPrototypeStore prototypeStore;
    private String serviceId;

    public TemplateEngine(IPluginContext context, ITypeStore typeStore) {
        this.typeStore = typeStore;

        this.templateStore = new JcrTemplateStore();
        this.prototypeStore = new JcrPrototypeStore();
    }

    public void setId(String serviceId) {
        this.serviceId = serviceId;
        templateStore.setId(serviceId);
    }

    public ITypeDescriptor getType(String type) {
        return typeStore.getTypeDescriptor(type);
    }

    public ITypeDescriptor getType(IModel model) {
        if (model instanceof JcrNodeModel) {
            try {
                Node node = ((JcrNodeModel) model).getNode();
                // prototype has primary type "nt:unstructured"; look up real type
                // by finding the containing templatetype.
                if (node.getPath().startsWith("/hippo:namespaces")
                        && node.getName().equals(HippoNodeType.HIPPO_PROTOTYPE)) {
                    if (node.isNodeType("nt:unstructured")) {
                        Node parent = node.getParent();
                        while (parent != null) {
                            if (parent.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                                return getType(parent.getParent().getName() + ":"
                                        + ISO9075Helper.decodeLocalName(parent.getName()));
                            }
                            parent = parent.getParent();
                        }
                        return null;
                    }
                } else if (node.isNodeType("nt:frozenNode")) {
                    String type = node.getProperty("jcr:frozenPrimaryType").getString();
                    return getType(type);
                }
                return getType(node.getPrimaryNodeType().getName());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Unable to resolve type of {}", model);
        }
        return null;
    }

    public IClusterConfig getTemplate(ITypeDescriptor type, String mode) {
        IClusterConfig cluster = templateStore.getCluster(type.getName() + "/" + mode);
        if (cluster != null) {
            cluster.put(ITemplateEngine.ENGINE, serviceId);
        }
        return cluster;
    }

    public IModel getPrototype(ITypeDescriptor type) {
        return prototypeStore.getPrototype(type.getName(), false);
    }

    public void detach() {
        templateStore.detach();
        prototypeStore.detach();
    }

}
