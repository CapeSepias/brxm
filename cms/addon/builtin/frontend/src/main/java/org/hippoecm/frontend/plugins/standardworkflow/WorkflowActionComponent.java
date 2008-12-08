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
package org.hippoecm.frontend.plugins.standardworkflow;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.DialogAction;

public class WorkflowActionComponent implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String id;
    private IModel label;
    private String icon;
    private DialogAction action;

    public WorkflowActionComponent(String id, IModel label, String icon, DialogAction action) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public IModel getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public DialogAction getAction() {
        return action;
    }
}
