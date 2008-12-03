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
package org.hippoecm.frontend.i18n.types;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.nodetypes.NodeTypeModelWrapper;
import org.hippoecm.frontend.types.JcrTypeDescriptor;
import org.hippoecm.frontend.types.JcrTypeStore;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeTranslator extends NodeTypeModelWrapper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(TypeTranslator.class);

    private static final long serialVersionUID = 1L;

    private TypeNameModel name;
    private transient boolean attached = false;
    private transient JcrNodeModel nodeModel;

    public TypeTranslator(JcrNodeTypeModel nodeTypeModel) {
        super(nodeTypeModel);
        name = new TypeNameModel();
    }

    public IModel getTypeName() {
        return name;
    }

    public IModel getValueName(String property, IModel value) {
        attach();
        return new PropertyValueModel(property, value);
    }

    public void detach() {
        if (attached) {
            super.detach();
            name.onDetachTranslator();
            nodeModel = null;
            attached = false;
        }
    }

    // internals

    private void attach() {
        if (!attached) {
            String type = getNodeTypeModel().getType();
            JcrTypeStore typeStore = new JcrTypeStore();
            JcrTypeDescriptor descriptor = typeStore.getTypeDescriptor(type);
            if (descriptor != null) {
                nodeModel = descriptor.getNodeModel().getParentModel().getParentModel();
            }
            attached = true;
        }
    }

    private JcrNodeModel getNodeModel() {
        attach();
        return nodeModel;
    }

    private class TypeNameModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        @Override
        protected Object load() {
            String name = getNodeTypeModel().getType();
            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        name = NodeNameCodec.decode(node.getName());
                        if (node.isNodeType("hippo:translated")) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes("hippo:translation");
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType("hippo:translation") && !child.hasProperty("hippo:property")) {
                                    String language = child.getProperty("hippo:language").getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty("hippo:message").getString();
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
            }
            return name;
        }

        void onDetachTranslator() {
            super.detach();
        }

        @Override
        public void detach() {
            TypeTranslator.this.detach();
        }
    }

    class PropertyValueModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        private String property;
        private IModel value;

        PropertyValueModel(String property, IModel value) {
            this.property = property;
            this.value = value;
        }

        @Override
        protected Object load() {
            IModel name = value;
            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        if (node.isNodeType("hippo:translated")) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes("hippo:translation");
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType("hippo:translation") && child.hasProperty("hippo:property")
                                        && child.hasProperty("hippo:value")) {
                                    if (child.getProperty("hippo:property").getString().equals(property)
                                            && child.getProperty("hippo:value").getString().equals(value.getObject())) {
                                        String language = child.getProperty("hippo:language").getString();
                                        if (locale.getLanguage().equals(language)) {
                                            return child.getProperty("hippo:message").getString();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
            }
            return name.getObject();
        }

        @Override
        public void detach() {
            super.detach();
            value.detach();
            TypeTranslator.this.detach();
        }

    }

}
