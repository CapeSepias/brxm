package org.hippoecm.frontend.plugins.console.menu.deletemultiple;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;

import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.TreeTable;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a dialog with subtree of node that's been selected
 * and allows user to select multiple nodes to delete those.
 *
 * @version "$Id$"
 */
public class DeleteMultipleDialog extends AbstractDialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(DeleteMultipleDialog.class);
    private static final long serialVersionUID = 1L;

    private NodeModelReference modelReference;
    private final TreeTable tree;
    private JcrNodeModel selectedModel;
    private IModel<Boolean> checkboxModel;

    public DeleteMultipleDialog(final NodeModelReference modelReference) {
        this.modelReference = modelReference;

        DefaultTreeModel model = null;
        try {

            selectedModel = (JcrNodeModel) modelReference.getModel();
            final JcrTreeNode root = new JcrTreeNode(new JcrNodeModel(selectedModel.getNode().getPath()), null);
            model = new DefaultTreeModel(root);
        } catch (RepositoryException e) {
            log.error("Error initializing tree", e);
        }
        IColumn columns[] = new IColumn[]{new PropertyTreeColumn(new ColumnLocation(Alignment.MIDDLE, 8,
                Unit.PROPORTIONAL), "Name", "nodeModel.node.name")

        };
        tree = new TreeTable("multitree", model, columns);
        tree.getTreeState().setAllowSelectMultiple(true);
        add(tree);
        if (model != null) {
            tree.getTreeState().expandNode(model.getRoot());
        }
        checkboxModel = new Model<Boolean>(Boolean.FALSE);
        add(new CheckBox("deleteFolders", checkboxModel));

    }


    @Override
    protected void onOk() {
        final Collection<Object> selectedNodes = tree.getTreeState().getSelectedNodes();
        // do not delete root (first selected node):
        if (rootSelected(selectedNodes)) {
            error("You've selected root node for deletion");
            return;
        }
        boolean deleteFolders = checkboxModel.getObject() == null ? false : checkboxModel.getObject();

        for (Object selectedNode : selectedNodes) {
            JcrTreeNode deleteNode = (JcrTreeNode) selectedNode;
            JcrNodeModel nodeModel = (JcrNodeModel) deleteNode.getChainedModel();
            final Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    // check if node has subnodes
                    if (node.getNodes().hasNext()) {
                        // delete only when allowed
                        if (deleteFolders) {
                            node.remove();
                        }
                    } else {
                        node.remove();
                    }

                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.error("Error removing node:", e);
                    }
                }
            }
        }
        modelReference.setModel(selectedModel);
    }


    private boolean rootSelected(Iterable<Object> selectedNodes) {
        for (Object selectedNode : selectedNodes) {
            JcrTreeNode deleteNode = (JcrTreeNode) selectedNode;
            JcrNodeModel nodeModel = (JcrNodeModel) deleteNode.getChainedModel();
            if (nodeModel.equals(selectedModel)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Delete multiple nodes");
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=640,height=650").makeImmutable();
    }

}
