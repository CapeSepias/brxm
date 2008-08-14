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
package org.hippoecm.frontend.plugins.reviewedactions;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class DefaultEditWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    @SuppressWarnings("unused")
    private String caption = "unknown document";

    public DefaultEditWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new Label("caption", new PropertyModel(this, "caption")));

        onModelChanged();

        addWorkflowAction("edit-dialog", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                Node docNode = ((WorkflowsModel)DefaultEditWorkflowPlugin.this.getModel()).getNodeModel().getNode();
                IEditService viewer = getPluginContext().getService(getPluginConfig().getString(IEditService.EDITOR_ID), IEditService.class);
                if (viewer != null) {
                    viewer.edit(new JcrNodeModel(docNode));
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
            }
        });
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowsModel model = (WorkflowsModel) getModel();
        try {
            Node node = model.getNodeModel().getNode();
            caption = ISO9075Helper.decodeLocalName(node.getName());
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
            log.info(ex.getClass().getName()+": "+ex.getMessage());
        }
    }
}
