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

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements ITemplateEngine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private IPluginContext context;
    private ITypeStore typeStore;
    private String serviceId;

    public TemplateEngine(IPluginContext context, ITypeStore typeStore) {
        this.context = context;
        this.typeStore = typeStore;
    }

    public void setId(String serviceId) {
        this.serviceId = serviceId;
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
                    if (node.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                        Node parent = node.getParent();
                        while (parent != null) {
                            if (parent.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                                return getType(parent.getParent().getName() + ":" + parent.getName());
                            }
                            parent = parent.getParent();
                        }
                        return null;
                    }
                } else if (node.isNodeType(JcrConstants.NT_FROZENNODE)) {
                    String type = node.getProperty(JcrConstants.JCR_FROZENPRIMARYTYPE).getString();
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
        IPluginConfigService configService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig cluster = configService.getCluster("template/" + type.getName() + "/" + mode);
        if (cluster != null) {
            cluster.put(ITemplateEngine.ENGINE, serviceId);
        }
        return cluster;
    }

}
