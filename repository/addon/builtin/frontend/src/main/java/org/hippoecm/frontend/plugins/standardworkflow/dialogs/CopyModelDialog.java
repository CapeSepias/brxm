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

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.EditmodelWorkflowPlugin;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyModelDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CopyModelDialog.class);

    private String name;

    public CopyModelDialog(EditmodelWorkflowPlugin plugin, IDialogService dialogWindow) {
        super(plugin, dialogWindow, new StringResourceModel("copy-model", (Component) null, null));

        WorkflowsModel wflModel = (WorkflowsModel) getPlugin().getModel();
        if (wflModel.getNodeModel().getNode() == null) {
            ok.setEnabled(false);
        }

        try {
            name = wflModel.getNodeModel().getNode().getName();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
    }

    @Override
    protected void execute() throws Exception {
        EditmodelWorkflow workflow = (EditmodelWorkflow) getWorkflow();
        if (workflow != null) {
            String path = workflow.copy(name);
            JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
            if (path != null) {
                IPluginContext context = getPlugin().getPluginContext();
                IPluginConfig config = getPlugin().getPluginConfig();

                IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
                jcrService.flush(nodeModel.getParentModel());
                
                IEditService viewService = context.getService(config.getString(IEditService.EDITOR_ID), IEditService.class);
                viewService.edit(nodeModel);
            } else {
                log.error("no model found to edit");
            }
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

}
