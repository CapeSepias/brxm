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
package org.hippoecm.frontend.plugins.reviewedactions;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullReviewedActionsWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowPlugin.class);

    public FullReviewedActionsWorkflowPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model,
            Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel) model, parentPlugin);

        try {
            add(new Label("caption", new JcrNodeModel(model).getNode().getName()));
        } catch (RepositoryException ex) {
            log.error("Could not obtain name of workflow document", ex);
            add(new Label("caption", "unknown document"));
        }

        String stateSummary = "UNKNOWN";
        try {
            Node node = ((WorkflowsModel) getModel()).getNodeModel().getNode();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();)
                    node = iter.nextNode(); // FIXME: take the last one, the first should be good enough
            }
            if (node.hasProperty("hippostd:stateSummary"))
                stateSummary = node.getProperty("hippostd:stateSummary").getString();
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
        }
        add(new Label("status", stateSummary));

        addWorkflowAction("edit-dialog", "Edit document", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                if (channel != null) {
                    Request request = channel.createRequest("edit", new JcrNodeModel(docNode));
                    return request;
                } else {
                    return null;
                }
            }
        });

        boolean requestPublication = !(stateSummary.equals("review") || stateSummary.equals("live"));
        addWorkflowAction("requestPublication-dialog", "Request publication", requestPublication, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestPublication();
                return null;
            }
        });
        
        boolean requestDepublication = !(stateSummary.equals("review") || stateSummary.equals("new"));
        addWorkflowAction("requestDePublication-dialog", "Request unpublication", requestDepublication, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
                return null;
            }
        });

        boolean requestDeletion = !(stateSummary.equals("review") || stateSummary.equals("live"));
        addWorkflowAction("requestDeletion-dialog", "Request deletion", requestDeletion, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
                return null;
            }
        });

        boolean publish = !(stateSummary.equals("review") || stateSummary.equals("live"));
        addWorkflowAction("publish-dialog", "Publish", publish, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
                return null;
            }
        });

        boolean depublish = !(stateSummary.equals("review") || stateSummary.equals("new"));
        addWorkflowAction("dePublish-dialog", "Unpublish", depublish, new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
                return null;
            }
        });

        addWorkflowAction("delete-dialog", "Unpublish and/or delete", new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();
                return null;
            }
        });
    }
}
