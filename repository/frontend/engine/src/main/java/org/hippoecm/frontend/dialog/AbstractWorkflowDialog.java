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
package org.hippoecm.frontend.dialog;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog operating in a workflow context. Each workflow action should
 * extend this class and implement the doOk() method.
 *
 */
public abstract class AbstractWorkflowDialog extends AbstractDialog {

    static final Logger log = LoggerFactory.getLogger(AbstractWorkflowDialog.class);

    public AbstractWorkflowDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
    }

    protected Workflow getWorkflow(String category) {
        Plugin owningPlugin = getOwningPlugin();
        Workflow workflow = null;
        try {
            // cast the plugin model to a jcr model
            JcrNodeModel nodeModel = new JcrNodeModel(owningPlugin.getPluginModel());
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();

            WorkflowDescriptor descriptor = manager.getWorkflowDescriptor(category, nodeModel.getNode());
            if (descriptor != null) {
                workflow = manager.getWorkflow(descriptor);
            }
        } catch (MappingException e) {
            log.error(e.getMessage());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return workflow;
    }

    @Override
    protected void ok() throws Exception {
        // before saving (which possibly means deleting), find the handle
        JcrNodeModel handle = dialogWindow.getNodeModel();
        while (handle.getParentModel() != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
            handle = handle.getParentModel();
        }

        // save the handle so that the workflow uses the correct content
        handle.getNode().save();
        doOk();

        ((UserSession) Session.get()).getJcrSession().refresh(true);

        // enqueue a request to select the handle
        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("select", dialogWindow.getNodeModel());
            channel.send(request);
        }
    }

    /**
     * This abstract method is called from ok() and should implement
     * the action to be performed when the dialog's ok button is clicked.
     */
    protected abstract void doOk() throws Exception;

}
