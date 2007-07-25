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
package org.hippocms.repository.frontend.model;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.Model;

public class JcrValueModel extends Model {
    private static final long serialVersionUID = 1L;

    private JcrPropertyModel propertyModel;
    private String value;
    private int index;

    public JcrValueModel(int index, String value, JcrPropertyModel propertyModel) {
        this.propertyModel = propertyModel;
        this.index = index;
        this.value = value;
    }
    
    public int getIndex() {
        return index;
    }

    public Object getObject() {
        return value;
    }

    public void setObject(Object object) {
        if (object != null) {
            value = object.toString();
            try {
                Property prop = propertyModel.getProperty();
                if (prop.getDefinition().isMultiple()) {
                    Value[] oldValues = prop.getValues();
                    String[] newValues = new String[oldValues.length];
                    for (int i = 0; i < oldValues.length; i++) {
                        if (i == index) {
                            newValues[i] = value;
                        } else {
                            newValues[i] = oldValues[i].getString();
                        }
                    }
                    prop.setValue(newValues);
                } else {
                    prop.setValue(value);
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

