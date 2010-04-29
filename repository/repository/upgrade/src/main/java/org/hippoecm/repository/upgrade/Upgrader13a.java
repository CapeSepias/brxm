/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.upgrade;

import java.io.InputStreamReader;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.ext.UpdaterItemVisitor.PathVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader13a implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(Upgrader13a.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v13a");
        context.registerStartTag("v12a");
        context.registerEndTag("v13a");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                /*
                 * The removal of the entire /hippo:log tree seems to be appropriate.  This is relatively volatile data as
                 * this is a sliding log file with the oldest entries being removed automatically.  Combine this with the
                 * fact that old entries might not contain the same information and the effort of converting data which is
                 * going to be removed quickly is unnecessary.
                 */
                if (node.hasNode("hippo:log")) {
                    for(NodeIterator iter=node.getNode("hippo:log").getNodes(); iter.hasNext(); ) {
                        iter.nextNode().remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hipposysedit:nodetype") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                context.setName(node, "hipposysedit_1_2:nodetype");
                context.setPrimaryNodeType(node, "hipposysedit_1_2:nodetype");
                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                    Node field = iter.nextNode();
                    context.setPrimaryNodeType(field, "hipposysedit_1_2:field");
                    if(field.hasProperty("hipposysedit_1_2:name")) {
                        Property nameProperty = field.getProperty("hipposysedit_1_2:name");
                        if(field.getName().equals("hipposysedit:field")) {
                            context.setName(field, nameProperty.getString());
                        }
                        nameProperty.remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposysedit", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposysedit.cnd"))));

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("editor:editable") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                if (node.hasNode("editor:templates")) {
                    Value compareToValue = node.getSession().getValueFactory().createValue("model.compareTo");
                    NodeIterator templates = node.getNode("editor:templates").getNodes();
                    while (templates.hasNext()) {
                        Node template = templates.nextNode();
                        if (!template.isNodeType("frontend:plugincluster")) {
                            continue;
                        }
                        // expect at the least a wicket.model reference
                        if (!template.hasProperty("frontend:references")) {
                            continue;
                        }

                        boolean handle = false;
                        NodeIterator plugins = template.getNodes();
                        while (plugins.hasNext()) {
                            Node plugin = plugins.nextNode();
                            if (!plugin.isNodeType("frontend:plugin")) {
                                continue;
                            }
                            if (plugin.hasProperty("plugin.class")) {
                                String clazz = plugin.getProperty("plugin.class").getString();
                                if ("org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin".equals(clazz)
                                        || "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin"
                                                .equals(clazz)) {
                                    handle = true;
                                    break;
                                }
                            }
                        }
                        if (handle) {
                            Value[] references = template.getProperty("frontend:references").getValues();
                            Value[] newRefs = new Value[references.length + 1];
                            System.arraycopy(references, 0, newRefs, 0, references.length);
                            newRefs[references.length] = compareToValue;
                            template.setProperty("frontend:references", newRefs);

                            plugins = template.getNodes();
                            while (plugins.hasNext()) {
                                Node plugin = plugins.nextNode();
                                if (!plugin.isNodeType("frontend:plugin")) {
                                    continue;
                                }
                                if (plugin.hasProperty("plugin.class")) {
                                    String clazz = plugin.getProperty("plugin.class").getString();
                                    if ("org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin".equals(clazz)
                                            || "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin"
                                                    .equals(clazz)) {
                                        plugin.setProperty("model.compareTo", "${model.compareTo}");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.QueryVisitor("//element(*,frontend:pluginconfig)[@encoding.node]", Query.XPATH) {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log.error("encoding.node property on "+node.getPath()+" no longer supported, please set /hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs/@encoding.node property instead");
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.QueryVisitor("//element(*,frontend:pluginconfig)[@encoding.display]", Query.XPATH) {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log.error("encoding.dos[;ay property on "+node.getPath()+" no longer supported, please set /hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs/@encoding.display property instead");
            } 
        });

        //Picker Update
        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log.info("Removing the cms-pickers node from " + node.getPath());
                node.getNode("cms-pickers").remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("cms-pickers")) {
                    log.info("Removing the cms-pickers node from " + node.getPath());
                    node.getNode("cms-pickers").remove();
                }
            }
        });
    }
}
