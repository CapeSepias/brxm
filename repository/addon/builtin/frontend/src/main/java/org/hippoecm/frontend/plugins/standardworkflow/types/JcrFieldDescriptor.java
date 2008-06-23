/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow.types;

import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrFieldDescriptor extends NodeModelWrapper implements IFieldDescriptor {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrFieldDescriptor.class);

    private Set<String> excluded;

    public JcrFieldDescriptor(JcrNodeModel model) {
        super(model);
    }

    public String getName() {
        return getString(HippoNodeType.HIPPO_NAME);
    }

    public String getPath() {
        return getString(HippoNodeType.HIPPO_PATH);
    }

    public void setPath(String path) {
        setString(HippoNodeType.HIPPO_PATH, path);
    }

    public String getType() {
        return getString(HippoNodeType.HIPPO_TYPE);
    }

    public void setType(String type) {
        setString(HippoNodeType.HIPPO_TYPE, type);
    }

    public boolean isMandatory() {
        return getBoolean(HippoNodeType.HIPPO_MANDATORY);
    }

    public void setMandatory(boolean mandatory) {
        setBoolean(HippoNodeType.HIPPO_MANDATORY, mandatory);
    }

    public boolean isMultiple() {
        return getBoolean(HippoNodeType.HIPPO_MULTIPLE);
    }

    public void setIsMultiple(boolean multiple) {
        setBoolean(HippoNodeType.HIPPO_MULTIPLE, multiple);
    }

    public boolean isOrdered() {
        return getBoolean(HippoNodeType.HIPPO_ORDERED);
    }

    public void setIsOrdered(boolean isOrdered) {
        setBoolean(HippoNodeType.HIPPO_ORDERED, isOrdered);
    }

    public Set<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(Set<String> set) {
        excluded = set;
    }

    public boolean isBinary() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isProtected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrFieldDescriptor) {
            if (object != null) {
                JcrFieldDescriptor otherModel = (JcrFieldDescriptor) object;
                return new EqualsBuilder().append(getName(), otherModel.getName()).append(getPath(),
                        otherModel.getPath()).isEquals();
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).append(getPath()).toHashCode();
    }

    private String getString(String path) {
        try {
            if (nodeModel.getNode().hasProperty(path)) {
                return nodeModel.getNode().getProperty(path).getString();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private void setString(String path, String value) {
        try {
            nodeModel.getNode().setProperty(path, value);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private boolean getBoolean(String path) {
        try {
            if (nodeModel.getNode().hasProperty(path)) {
                return nodeModel.getNode().getProperty(path).getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    private void setBoolean(String path, boolean value) {
        try {
            nodeModel.getNode().setProperty(path, value);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

}
