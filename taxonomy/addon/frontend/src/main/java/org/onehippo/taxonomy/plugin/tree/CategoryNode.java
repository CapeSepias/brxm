/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.tree;

import java.util.List;

import javax.swing.tree.TreeNode;

import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.model.CategoryModel;

public class CategoryNode extends AbstractNode {
    private static final long serialVersionUID = 1L;

    AbstractNode parent;
    CategoryModel model;

    public CategoryNode(CategoryModel model, String language) {
        super(model.getTaxonomyModel(), language);
        this.model = model;

        final Category category = getCategory();
        if (category == null) {
            throw new IllegalStateException("Could not get a Category for '"+model.getKey()+"'.");
        }
        Category parentCat = category.getParent();
        if (parentCat == null) {
            parent = new TaxonomyNode(model.getTaxonomyModel(), language);
        } else {
            parent = new CategoryNode(new CategoryModel(model.getTaxonomyModel(), parentCat.getKey()), language);
        }
    }

    public Category getCategory() {
        return model.getObject();
    }

    @Override
    List<? extends Category> getCategories() {
        return getCategory().getChildren();
    }

    public TreeNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CategoryNode) {
            return ((CategoryNode) obj).model.equals(model);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return model.hashCode() ^ 34603;
    }

    @Override
    public void detach() {
        model.detach();
        super.detach();
    }

}
