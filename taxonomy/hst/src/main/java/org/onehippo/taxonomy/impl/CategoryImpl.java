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
package org.onehippo.taxonomy.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.collection.CompositeCollection;
import org.apache.commons.collections.map.LazyMap;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryImpl extends AbstractJCRService implements Category {

    static Logger log = LoggerFactory.getLogger(CategoryImpl.class);
    private static final long serialVersionUID = 1L;

    private Taxonomy taxonomy;
    private Category parent;
    private List<Category> childCategories = new ArrayList<Category>();
    private Map<String, CategoryInfo> translations = new HashMap<String, CategoryInfo>();
    private String name;
    private String relPath;
    private String path;
    private String key;

    public CategoryImpl(Node item, Category parent, TaxonomyImpl taxonomyImpl) throws ServiceException {
        super(item);
        this.taxonomy = taxonomyImpl;
        this.parent = parent;
        this.name = this.getValueProvider().getName();
        this.path = this.getValueProvider().getPath();
        if (!this.path.startsWith(taxonomyImpl.getPath() + "/")) {
            throw new ServiceException("Path of a category cannot start with other path then  root taxonomy");
        }
        this.relPath = path.substring(taxonomyImpl.getPath().length() + 1);
        try {
            this.key = this.getValueProvider().getString(TaxonomyNodeTypes.HIPPOTAXONOMY_KEY);

            // populate translations
            if (item.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATED)) {
                NodeIterator nodes = item.getNodes(TaxonomyNodeTypes.HIPPOTAXONOMY_TRANSLATION);
                while (nodes.hasNext()) {
                    Node childItem = nodes.nextNode();
                    if (childItem != null) {
                        try {
                            CategoryInfo translation = new CategoryInfoImpl(childItem);
                            translations.put(translation.getLanguage(), translation);
                        } catch (ServiceException e) {
                            log.warn("Skipping translation because '{}', {}", e.getMessage(), e);
                        }
                    }
                }
            }

            // populate childs:
            NodeIterator nodes = item.getNodes();
            while (nodes.hasNext()) {
                Node childItem = nodes.nextNode();
                if (childItem != null) {
                    if (childItem.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                        try {
                            Category taxonomyItem = new CategoryImpl(childItem, this, taxonomyImpl);
                            childCategories.add(taxonomyItem);
                        } catch (ServiceException e) {
                            log.warn("Skipping category because '{}', {}", e.getMessage(), e);
                        }
                    } else {
                        // warn, except when it concerns a (valid) translation
                        if (!childItem.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATION)) {
                            log.warn("Skipping child nodes that are not of type '{}' or '{}'.",
                                    TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY, 
                                    TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATION
                                    + " Primary node type is " + childItem.getPrimaryNodeType().getName() + ".");
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Error while creating category", e);
        }

        // if no exception happened, add this item to the descendant list.
        taxonomyImpl.addDescendantItem(this);
    }

    public List<Category> getChildren() {
        return Collections.unmodifiableList(childCategories);
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.relPath;
    }

    public String getKey() {
        return this.key;
    }
    
    public Taxonomy getTaxonomy() {
        return this.taxonomy;
    }

    public Service[] getChildServices() {
        Collection<Service> composite =  new CompositeCollection(new Collection [] {
                translations.values(), childCategories });
        return composite.toArray(new Service[composite.size()]);
        
    }

    public Category getParent() {
        return this.parent;
    }

    public LinkedList<Category> getAncestors() {
        LinkedList<Category> ancestors = new LinkedList<Category>();
        Category item = this;
        while (item.getParent() != null) {
            item = item.getParent();
            ancestors.addFirst(item);
        }
        return ancestors;
    }

    public CategoryInfo getInfo(String language) {
        CategoryInfo info = translations.get(language);
        if (info == null) {
            return new TransientCategoryInfoImpl(this);
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ? extends CategoryInfo> getInfos() {
        final Map<String, CategoryInfo> map = new HashMap<String, CategoryInfo>();
        
        return LazyMap.decorate(map, new Transformer() {
            @Override
            public Object transform(Object input) {
                return getInfo((String) input);
            }
        });
    }

}
