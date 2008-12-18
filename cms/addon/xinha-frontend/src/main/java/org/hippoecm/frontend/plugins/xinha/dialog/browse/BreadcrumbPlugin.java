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

package org.hippoecm.frontend.plugins.xinha.dialog.browse;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreadcrumbPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BreadcrumbPlugin.class);

    private final Set<String> roots;
    private final AjaxButton up;

    public BreadcrumbPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        roots = new HashSet<String>();
        String[] paths = config.getStringArray("root.paths");
        if (paths != null) {
            for (String path : paths) {
                roots.add(path);
            }
        } else {
            roots.add("/");
        }
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        add(getListView(nodeModel));

        if (config.getString("model.folder") != null) {
            context.registerService(new IModelListener() {
                private static final long serialVersionUID = 1L;

                public void updateModel(IModel model) {
                    update((JcrNodeModel) model);
                }

            }, config.getString("model.folder"));
        }
        up = new AjaxButton("up") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                JcrNodeModel model = (JcrNodeModel) BreadcrumbPlugin.this.getModel();
                model = model.getParentModel();
                if (model != null) {
                    BreadcrumbPlugin.this.setModel(model);
                }
            }
        };
        up.setModel(new StringResourceModel("dialog-breadcrumb-up", this, null));
        if (roots.contains(nodeModel.getItemModel().getPath())) {
            up.setEnabled(false);
        }
        add(up);
    }

    protected void update(JcrNodeModel model) {
        replace(getListView(model));

        JcrNodeModel parentModel = model.getParentModel();
        if (parentModel == null || roots.contains(model.getItemModel().getPath())) {
            up.setEnabled(false);
        } else {
            up.setEnabled(true);
        }
    }

    private ListView getListView(JcrNodeModel model) {
        final List<NodeItem> list = new LinkedList<NodeItem>();
        //add current folder as disabled
        list.add(new NodeItem(model, false));
        if (!roots.contains(model.getItemModel().getPath())) {
            model = model.getParentModel();
            while (model != null) {
                list.add(new NodeItem(model, true));
                if (roots.contains(model.getItemModel().getPath())) {
                    model = null;
                } else {
                    model = model.getParentModel();
                }
            }
        }
        Collections.reverse(list);
        ListView listview = new ListView("crumbs", list) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final NodeItem nodeItem = (NodeItem) item.getModelObject();
                AjaxLink link = new AjaxLink("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        BreadcrumbPlugin.this.setModel(nodeItem.model);
                    }

                };
                link.add(new Label("name", new Model(nodeItem.name)));
                link.setEnabled(nodeItem.enabled);
                item.add(link);
                String state = nodeItem.enabled ? "enabled" : "disabled";
                item.add(new AttributeAppender("class", new Model(state), " "));
                
                if (item.getIndex() == 0) {
                    item.add(new AttributeAppender("class", new Model("first"), " "));
                } else if (item.getIndex() == (list.size() - 1)) {
                    item.add(new AttributeAppender("class", new Model("last"), " "));
                }
            }
        };
        return listview;
    }

    private class NodeItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        boolean enabled;
        JcrNodeModel model;
        String name;

        public NodeItem(JcrNodeModel model, boolean enabled) {
            try {
                this.name = model.getNode().getName();
            } catch (RepositoryException e) {
                String path = model.getItemModel().getPath();
                this.name = path.substring(path.lastIndexOf('/'));

                log.warn("Error retrieving name from node[" + path + "]");
            }
            this.model = model;
            this.enabled = enabled;
        }
    }

}