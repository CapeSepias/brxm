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
package org.hippoecm.frontend.plugins.console.menu.move;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveDialog extends LookupDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(MoveDialog.class);

    private MenuPlugin plugin;
    private String name;
    @SuppressWarnings("unused")
    private String target;
    private Label targetLabel;

    public MoveDialog(MenuPlugin plugin) {
        super(new JcrTreeNode(new JcrNodeModel("/")));

        this.plugin = plugin;

        JcrNodeModel model = (JcrNodeModel) plugin.getModel();
        setSelectedNode(model);
        try {
            if (model.getParentModel() != null) {
                add(new Label("source", model.getNode().getPath()));

                target = StringUtils.substringBeforeLast(model.getNode().getPath(), "/") + "/";
                targetLabel = new Label("target", new PropertyModel(this, "target"));
                targetLabel.setOutputMarkupId(true);
                add(targetLabel);

                name = model.getNode().getName();
                TextFieldWidget nameField = new TextFieldWidget("name", new PropertyModel(this, "name"));
                nameField.setSize(String.valueOf(name.length() + 5));
                add(nameField);
            } else {
                add(new Label("source", "Cannot move the root node"));
                add(new EmptyPanel("target"));
                add(new EmptyPanel("name"));
                ok.setVisible(false);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            add(new Label("source", e.getClass().getName()));
            add(new Label("target", e.getMessage()));
            add(new EmptyPanel("name"));
            ok.setVisible(false);
        }
    }

    public IModel getTitle() {
        return new Model("Move Node");
    }

    @Override
    public void onSelect(JcrNodeModel model) {
        if (model != null) {
            try {
                target = model.getNode().getPath() + "/";
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        AjaxRequestTarget requestTarget = AjaxRequestTarget.get();
        if (requestTarget != null) {
            requestTarget.addComponent(targetLabel);
        }
    }

    @Override
    protected boolean isValidSelection(AbstractTreeNode targetModel) {
        return true;
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();

        JcrNodeModel selectedNode = getSelectedNode().getNodeModel();
        if (selectedNode != null && name != null && !"".equals(name)) {
            JcrNodeModel targetNodeModel = getSelectedNode().getNodeModel();
            String targetPath = targetNodeModel.getNode().getPath();
            if (!targetPath.endsWith("/")) {
                targetPath += "/";
            }
            targetPath += name;

            // The actual move
            UserSession wicketSession = (UserSession) getSession();
            HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();
            String sourcePath = nodeModel.getNode().getPath();
            jcrSession.move(sourcePath, targetPath);

            Node rootNode = nodeModel.getNode().getSession().getRootNode();
            Node targetNode = rootNode.getNode(targetPath.substring(1));
            plugin.setModel(new JcrNodeModel(targetNode));
            plugin.flushNodeModel(new JcrNodeModel(rootNode));
        }
    }

}
