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

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.JcrNodeModel;

public class LabelTreeNode extends AbstractTreeNode {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private JcrTreeNode parentNode;
    private long moreCount;

    public LabelTreeNode(JcrNodeModel parentModel, JcrTreeNode parentNode, long moreCount) {
        super(parentModel);
        this.parentNode = parentNode;
        this.moreCount = moreCount;
    }

    public TreeNode getParent() {
        return parentNode;
    }

    @Override
    protected int loadChildcount() throws RepositoryException {
        return 0;
    }

    @Override
    protected List<AbstractTreeNode> loadChildren() throws RepositoryException {
        return new ArrayList();
    }

    @Override
    public String renderNode() {
        return " ... " + moreCount + " more ...";
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if(!(object instanceof LabelTreeNode)) {
            return false;
        } else {
            LabelTreeNode treeNode = (LabelTreeNode) object;
            return new EqualsBuilder().append(nodeModel, treeNode.nodeModel).isEquals();
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 335).append(nodeModel).toHashCode();
    }
}
