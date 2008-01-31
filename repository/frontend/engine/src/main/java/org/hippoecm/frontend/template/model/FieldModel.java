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
package org.hippoecm.frontend.template.model;

import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldModel extends NodeModelWrapper implements IPluginModel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FieldModel.class);

    private FieldDescriptor descriptor;

    //  Constructor
    public FieldModel(FieldDescriptor descriptor, JcrNodeModel parent) {
        super(parent);
        this.descriptor = descriptor;
    }

    public FieldModel(IPluginModel model, TemplateEngine engine) {
        super(new JcrNodeModel(model));
        Map<String, Object> map = model.getMapRepresentation();
        this.descriptor = new FieldDescriptor((Map) map.get("field"), engine);
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = getNodeModel().getMapRepresentation();
        map.put("field", descriptor.getMapRepresentation());
        return map;
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }

    public void remove() {
        if (descriptor.getPath() != null) {
            Node node = getNodeModel().getNode();
            try {
                NodeIterator iterator = node.getNodes(descriptor.getPath());
                while (iterator.hasNext()) {
                    JcrItemModel itemModel = new JcrItemModel(iterator.nextNode());

                    if (itemModel.exists()) {
                        Item item = (Item) itemModel.getObject();

                        // remove the item
                        log.info("removing item " + item.getPath());
                        item.remove();
                    } else {
                        log.info("item " + itemModel.getPath() + " does not exist");
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    // override Object methods

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("descriptor", descriptor).append(
                "node", getNodeModel()).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof FieldModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        FieldModel fieldModel = (FieldModel) object;
        return new EqualsBuilder().append(descriptor, fieldModel.descriptor).
            append(nodeModel.getItemModel(), fieldModel.nodeModel.getItemModel()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 67).append(descriptor).append(nodeModel.getItemModel()).toHashCode();
    }
}
