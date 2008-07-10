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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.CopyModelDialog;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditmodelWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(EditmodelWorkflowPlugin.class);

    public EditmodelWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        addWorkflowAction("editModel-action", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public void execute(Workflow workflow) throws Exception {
                EditmodelWorkflow emWorkflow = (EditmodelWorkflow) workflow;
                if (emWorkflow != null) {
                    String path = emWorkflow.edit();
                    try {
                        Node node = ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path.substring(1));
                        JcrItemModel itemModel = new JcrItemModel(node);
                        if (path != null) {
                            IEditService viewService = context.getService(config.getString(IEditService.EDITOR_ID),
                                    IEditService.class);
                             if (viewService != null) {
                                 viewService.edit(new JcrNodeModel(itemModel));
                             } else {
                                 log.warn("No view service found");
                             }
                        } else {
                            log.error("no model found to edit");
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    log.error("no workflow defined on model for selected node");
                }
            }
        });

        add(new DialogLink("copyModelRequest-dialog", new Model("Copy model"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new CopyModelDialog(EditmodelWorkflowPlugin.this, dialogService);
            }
        }, getDialogService()));
    }
}
