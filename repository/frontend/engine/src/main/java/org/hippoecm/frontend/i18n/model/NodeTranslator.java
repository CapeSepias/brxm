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
package org.hippoecm.frontend.i18n.model;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeTranslator extends NodeModelWrapper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(NodeTranslator.class);

    private static final long serialVersionUID = 1L;

    private LoadableDetachableModel name;
    private transient TreeMap<String, Property> properties;

    public NodeTranslator(JcrNodeModel nodeModel) {
        super(nodeModel);
        name = new NodeNameModel();
    }

    public IModel getNodeName() {
        return name;
    }

    public IModel getPropertyName(String property) {
        attach();
        if (!properties.containsKey(property)) {
            properties.put(property, new Property(property));
        }
        return properties.get(property).name;
    }

    public IModel getValueName(String property, String value) {
        attach();
        if (!properties.containsKey(property)) {
            properties.put(property, new Property(property));
        }
        return properties.get(property).getValue(value);
    }

    @Override
    public void detach() {
        super.detach();
        name.detach();
        properties = null;
    }

    private void attach() {
        if (properties == null) {
            properties = new TreeMap<String, Property>();
        }
    }

    private class NodeNameModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        @Override
        protected Object load() {
            Node node = nodeModel.getNode();
            String name = "node name";
            if (node != null) {
                try {
                    name = ISO9075Helper.decodeLocalName(node.getName());
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
            return name;
        }

    }

    private class Property implements IDetachable {
        private static final long serialVersionUID = 1L;

        String property;
        PropertyNameModel name;
        Map<String, PropertyValueModel> values;
        transient boolean attached = false;

        Property(String property) {
            this.property = property;
            this.name = new PropertyNameModel();
            attached = true;
        }

        IModel getValue(String name) {
            if (values == null) {
                values = new TreeMap<String, PropertyValueModel>();
            }
            if (!values.containsKey(name)) {
                values.put(name, new PropertyValueModel(name));
            }
            return values.get(name);
        }

        void attach() {
            attached = true;
        }

        public void detach() {
            if (attached) {
                name.onDetachProperty();
                if (values != null) {
                    for (Map.Entry<String, PropertyValueModel> entry : values.entrySet()) {
                        entry.getValue().onDetachProperty();
                    }
                }
                attached = false;
            }
        }

        class PropertyNameModel extends LoadableDetachableModel {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                Property.this.attach();
                String name = property;
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        if (node.isNodeType("hippo:translated")) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes("hippo:translation");
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType("hippo:translation") && child.hasProperty("hippo:property")
                                        && !child.hasProperty("hippo:value")) {
                                    if (child.getProperty("hippo:property").getString().equals(property)) {
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
                return name;
            }

            void onDetachProperty() {
                super.detach();
            }

            @Override
            public void detach() {
                Property.this.detach();
            }
        }

        class PropertyValueModel extends LoadableDetachableModel {
            private static final long serialVersionUID = 1L;

            private String value;

            PropertyValueModel(String value) {
                this.value = value;
            }

            @Override
            protected Object load() {
                Property.this.attach();
                String name = property;
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
                                            && child.getProperty("hippo:value").getString().equals(value)) {
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
                return name;
            }

            void onDetachProperty() {
                super.detach();
            }

            @Override
            public void detach() {
                Property.this.detach();
            }

        }

    }

}
