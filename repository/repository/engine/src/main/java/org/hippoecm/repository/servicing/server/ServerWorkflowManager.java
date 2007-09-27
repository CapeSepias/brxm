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
package org.hippoecm.repository.servicing.server;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.ServerObject;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteServicingAdapterFactory;

public class ServerWorkflowManager extends ServerObject implements RemoteWorkflowManager {
    private WorkflowManager workflowManager;

    public ServerWorkflowManager(WorkflowManager manager, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(factory);
        this.workflowManager = manager;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, String absPath) throws RepositoryException,
            RemoteException {
        try {
            String path = absPath;
            if (absPath.startsWith("/"))
                path = path.substring(1);
            Node node = workflowManager.getSession().getRootNode().getNode(path);
            return workflowManager.getWorkflowDescriptor(category, node);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Workflow getWorkflow(String category, String absPath) throws RepositoryException, RemoteException {
        try {
            String path = absPath;
            if (absPath.startsWith("/"))
                path = path.substring(1);
            Node node = workflowManager.getSession().getRootNode().getNode(path);
            return workflowManager.getWorkflow(category, node);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException, RemoteException {
        try {
            return workflowManager.getWorkflow(descriptor);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
