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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import java.util.Iterator;

import javax.swing.tree.TreeNode;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;

import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class FolderTreePlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    protected Tree tree;
    protected AbstractTreeNode rootNode;
    private String startingPath = "/";

    public FolderTreePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IJcrService.class.getName());

        startingPath = config.getString("path", startingPath);
        FolderTreeConfig folderTreeConfig = new FolderTreeConfig(config);
        this.rootNode = new FolderTreeNode(new JcrNodeModel(startingPath), folderTreeConfig);

        JcrTreeModel treeModel = new JcrTreeModel(rootNode);
        tree = new CmsJcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                FolderTreePlugin.this.setModel(treeNodeModel.getNodeModel());
            }
        };
        add(tree);

        tree.setRootLess(config.getBoolean("rootless"));

        onModelChanged();
    }

    @Override
    public void focus(IRenderService child) {
        super.focus(child);
        setModel(new JcrNodeModel(startingPath));
    }

    public void onFlush(JcrNodeModel nodeModel) {
        AbstractTreeNode node = rootNode.getTreeModel().lookup(nodeModel);
        if (node != null) {
            node.markReload();
            node.getTreeModel().nodeStructureChanged(node);
            redraw();
        } else {
            rootNode.markReload();
            rootNode.getTreeModel().nodeStructureChanged(rootNode);
            redraw();
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getModel();
        AbstractTreeNode node = null;
        boolean nodesSelected = false;

        while (model != null) {
            node = rootNode.getTreeModel().lookup(model);
            if(node == null) {
                redraw();
                break;
            }
            try {
                if(node.getNodeModel().getNode() != null) {
                    if (node.getNodeModel().getNode().getPath().startsWith(startingPath)) {
                        nodesSelected = true;
                    }
                }
            } catch(RepositoryException ex) {
                // FIXME log some warning
            }
            if (node != null) {
                TreeNode parentNode = node.getParent();
                while (parentNode != null && !tree.getTreeState().isNodeExpanded(parentNode)) {
                    tree.getTreeState().expandNode(parentNode);
                    parentNode = parentNode.getParent();
                }
                tree.getTreeState().selectNode(node, true);
                redraw();
                break;
            }
            model = model.getParentModel();
        }

        //if(!nodesSelected) {
        //    tree.getTreeState().collapseAll();
        //}
    }
}
