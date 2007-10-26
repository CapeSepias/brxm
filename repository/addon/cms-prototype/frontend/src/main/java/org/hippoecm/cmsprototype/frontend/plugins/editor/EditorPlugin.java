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
package org.hippoecm.cmsprototype.frontend.plugins.editor;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.cmsprototype.frontend.plugins.tabs.IConsumer;

public class EditorPlugin extends Plugin implements IConsumer {
    private static final long serialVersionUID = 1L;

    private NodeEditor editor;

    public EditorPlugin(String id, JcrNodeModel model) {
        super(id, model);        
        editor = new NodeEditor("editor", model);
        add(editor);
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        editor.update(target, jcrEvent);
    }

    public void setModel(JcrNodeModel model) {
        editor.setModel(model);
    }
}
