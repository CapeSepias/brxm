/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import org.onehippo.cm.model.ConfigurationProperty;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.ValueFormatException;
import org.onehippo.cm.model.ValueType;

public class ConfigurationPropertyImpl extends ConfigurationItemImpl implements ConfigurationProperty {


    private PropertyType type;
    private ValueType valueType;
    private ValueImpl value;
    private ValueImpl[] values;

    @Override
    public PropertyType getType() {
        return type;
    }

    public void setType(final PropertyType type) {
        this.type = type;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(final ValueType valueType) {
        this.valueType = valueType;
    }

    @Override
    public ValueImpl getValue() throws ValueFormatException {
        return value;
    }

    public void setValue(final ValueImpl value) {
        this.value = value;
    }

    @Override
    public ValueImpl[] getValues() throws ValueFormatException {
        return values;
    }

    public void setValues(final ValueImpl[] values) {
        this.values = values;
    }
}
