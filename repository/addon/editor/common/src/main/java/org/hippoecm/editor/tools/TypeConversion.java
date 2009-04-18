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
package org.hippoecm.editor.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeStore;

public class TypeConversion implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private TypeUpdate update;

    public TypeConversion(ITypeStore currentConfig, ITypeStore draftConfig, ITypeDescriptor current, ITypeDescriptor draft) {
        update = new TypeUpdate();

        if (draft != null) {
            update.newName = draft.getName();
        } else {
            update.newName = current.getName();
        }

        update.renames = new HashMap<FieldIdentifier, FieldIdentifier>();
        for (Map.Entry<String, IFieldDescriptor> entry : current.getFields().entrySet()) {
            IFieldDescriptor origField = entry.getValue();
            FieldIdentifier oldId = new FieldIdentifier();
            oldId.path = origField.getPath();
            oldId.type = currentConfig.getTypeDescriptor(origField.getType()).getType();

            if (draft != null) {
                IFieldDescriptor newField = draft.getField(entry.getKey());
                if (newField != null) {
                    FieldIdentifier newId = new FieldIdentifier();
                    newId.path = newField.getPath();
                    ITypeDescriptor newType = draftConfig.getTypeDescriptor(newField.getType());
                    if (newType == null) {
                        // FIXME: test namespace prefix before resorting to the current config.
                        newType = currentConfig.getTypeDescriptor(newField.getType());
                    }
                    newId.type = newType.getType();

                    update.renames.put(oldId, newId);
                }
            } else {
                update.renames.put(oldId, oldId);
            }
        }
    }

    public TypeUpdate getTypeUpdate() {
        return update;
    }
}
