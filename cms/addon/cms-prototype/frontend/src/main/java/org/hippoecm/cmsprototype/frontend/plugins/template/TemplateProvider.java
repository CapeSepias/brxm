/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.template;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateProvider extends JcrNodeModel implements IDataProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateProvider.class);

    private TemplateDescriptor descriptor;
    private TemplateEngine engine;
    private ArrayList<FieldModel> fields;

    // Constructor

    public TemplateProvider(TemplateDescriptor descriptor, Node node, TemplateEngine engine) {
        super(null, node);
        this.descriptor = descriptor;
        this.engine = engine;

        reset();
    }

    public TemplateConfig getTemplateConfig() {
        return engine.getConfig();
    }

    public void clone(TemplateProvider provider) {
        setChainedModel(provider.getChainedModel());
        this.descriptor = provider.descriptor;
        reset();
    }

    // IDataProvider implementation, provides the fields of the chained itemModel

    public Iterator<FieldModel> iterator(int first, int count) {
        return fields.subList(first, first + count).iterator();
    }

    public IModel model(Object object) {
        FieldModel model = (FieldModel) object;
        return model;
    }

    public int size() {
        return fields.size();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", descriptor.toString())
                .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TemplateProvider == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        TemplateProvider fieldProvider = (TemplateProvider) object;
        return new EqualsBuilder().append(engine, fieldProvider.engine).append(descriptor, fieldProvider.descriptor)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(engine).append(descriptor).toHashCode();
    }
    
    private void reset() {
        Node node = getNode();
        fields = new ArrayList<FieldModel>();
        if (descriptor != null) {
            Iterator<FieldDescriptor> iter = descriptor.getFields().iterator();
            while (iter.hasNext()) {
                FieldDescriptor field = iter.next();
                try {
                    Item item = null;
                    if (node.hasProperty(field.getPath())) {
                        item = node.getProperty(field.getPath());
                    } else if (node.hasNode(field.getPath())) {
                        item = node.getNode(field.getPath());
                    }
                    if (item != null) {
                        fields.add(new FieldModel(item, field));
                    }
                } catch (RepositoryException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
