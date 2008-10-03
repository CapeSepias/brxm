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
package org.hippoecm.frontend.plugins.standardworkflow.dialogs;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.TemplateEditorWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(NamespaceDialog.class);

    private String prefix;

    private String url;

    public NamespaceDialog(AbstractWorkflowPlugin plugin, IDialogService dialogWindow) {
        super(plugin, dialogWindow, "Create new namespace");

        add(new TextFieldWidget("prefix", new PropertyModel(this, "prefix")));

        add(new TextFieldWidget("url", new PropertyModel(this, "url")));
    }

    @Override
    protected void execute() throws Exception {
        TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) getWorkflow();
        workflow.createNamespace(prefix, url);
    }
}
