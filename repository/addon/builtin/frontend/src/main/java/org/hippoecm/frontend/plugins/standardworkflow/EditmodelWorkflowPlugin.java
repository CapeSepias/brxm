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

import org.apache.wicket.model.Model;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.CopyModelDialog;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.EditModelDialog;

public class EditmodelWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public EditmodelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new DialogLink("editModelRequest-dialog", new Model("Edit model"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new EditModelDialog(EditmodelWorkflowPlugin.this, dialogService);
            }
        }, getDialogService()));

        add(new DialogLink("copyModelRequest-dialog", new Model("Copy model"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new CopyModelDialog(EditmodelWorkflowPlugin.this, dialogService);
            }
        }, getDialogService()));
    }
}
