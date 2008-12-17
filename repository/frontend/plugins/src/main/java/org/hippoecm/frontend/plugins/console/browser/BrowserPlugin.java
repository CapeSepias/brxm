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
package org.hippoecm.frontend.plugins.console.browser;

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.wicket1985.Tree;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserPlugin.class);

    protected Tree tree;
    protected JcrTreeModel treeModel;
    protected AbstractTreeNode rootNode;

    public BrowserPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IJcrService.class.getName());

        this.rootNode = new JcrTreeNode(new JcrNodeModel("/"), null);

        treeModel = new JcrTreeModel(rootNode);
        tree = newTree(treeModel);
        add(tree);

        add(new ScrollBehavior());
        onModelChanged();
    }

    protected Tree newTree(JcrTreeModel treeModel) {
        return new JcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                BrowserPlugin.this.onSelect(treeNodeModel, target);
            }
        };
    }

    protected void onSelect(final AbstractTreeNode treeNodeModel, AjaxRequestTarget target) {
        setModel(treeNodeModel.getNodeModel());
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getModel();
        AbstractTreeNode node = treeModel.lookup(model);
        if (node != null) {
            TreeNode parentNode = node.getParent();
            while (parentNode != null) {
                if(!tree.getTreeState().isNodeExpanded(parentNode)) {
                    tree.getTreeState().expandNode(parentNode);
                }
                parentNode = parentNode.getParent();
            }
            tree.getTreeState().selectNode(node, true);
            redraw();
        }
    }

    // implements IJcrNodeModelListener

    public void onFlush(JcrNodeModel nodeModel) {
        AbstractTreeNode node = treeModel.lookup(nodeModel);
        if (node != null) {
            node.detach();
            treeModel.nodeStructureChanged(node);
            redraw();
        }
    }

}
