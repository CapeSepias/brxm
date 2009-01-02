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
package org.hippoecm.frontend.model.tree;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class JcrTreeNode extends AbstractTreeNode {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static int MAXCOUNT = 2000;

    private JcrTreeNode parent;

    public JcrTreeNode(JcrNodeModel nodeModel, JcrTreeNode parent) {
        super(nodeModel);

        this.parent = parent;
    }

    public TreeNode getParent() {
        return parent;
    }

    /**
     * Checks if the wrappen jcr node is a virtual node
     * @return true if the node is virtual else false
     */
    public boolean isVirtual() {
        if (nodeModel == null) {
            return false;
        }
        HippoNode jcrNode = nodeModel.getNode();
        if (jcrNode == null) {
            return false;
        }
        try {
            Node canonical = jcrNode.getCanonicalNode();
            if (canonical == null) {
                return true;
            }
            return !jcrNode.getCanonicalNode().isSame(jcrNode);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected int loadChildcount() throws RepositoryException {
        int result;
        HippoNode node = nodeModel.getNode();
        if (node.isNodeType(HippoNodeType.NT_FACETRESULT) || node.isNodeType(HippoNodeType.NT_FACETSEARCH)
                || node.getCanonicalNode() == null) {
            result = 1;
        } else {
            result = (int) node.getNodes().getSize();
        }
        return result;
    }

    @Override
    protected List<AbstractTreeNode> loadChildren() throws RepositoryException {
        Node node = nodeModel.getNode();
        List<AbstractTreeNode> newChildren = new ArrayList();
        NodeIterator jcrChildren = node.getNodes();
        int count = 0;
        while (jcrChildren.hasNext() && count < MAXCOUNT) {
            Node jcrChild = jcrChildren.nextNode();
            if (jcrChild != null) {
                ++count;
                JcrNodeModel childModel = new JcrNodeModel(jcrChild);
                JcrTreeNode treeNodeModel = new JcrTreeNode(childModel, this);
                newChildren.add(treeNodeModel);
            }
        }
        if (jcrChildren.hasNext()) {
            LabelTreeNode treeNodeModel = new LabelTreeNode(nodeModel, this, jcrChildren.getSize()
                    - jcrChildren.getPosition());
            newChildren.add(treeNodeModel);
        }
        return newChildren;
    }

    @Override
    public String renderNode() {
        String result = "unknown";
        HippoNode node = getNodeModel().getNode();
        if (node != null) {
            try {
                result = node.getDisplayName();
                if (node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                    result += " [" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]";
                }
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrTreeNode == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrTreeNode treeNode = (JcrTreeNode) object;
        return new EqualsBuilder().append(nodeModel, treeNode.nodeModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 335).append(nodeModel).toHashCode();
    }

}
