/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

public class MockWorkflowContext implements WorkflowContext {

    private String userIdentity;
    private Session session;

    public MockWorkflowContext(String userIdentity) throws RepositoryException {
        this.userIdentity = userIdentity;
        this.session = new MockSession(MockNode.root());
    }

    public MockWorkflowContext(String userIdentity, Session session) throws RepositoryException {
        this.userIdentity = userIdentity;
        this.session = session;
    }

    public MockWorkflowContext(String userIdentity, MockNode rootNode) throws RepositoryException {
        this.userIdentity = userIdentity;
        this.session = new MockSession(rootNode);
    }

    @Override
    public WorkflowContext getWorkflowContext(final Object specification) throws MappingException, RepositoryException {
        return this;
    }

    @Override
    public Workflow getWorkflow(final String category) throws MappingException, WorkflowException, RepositoryException {
        return null;
    }

    @Override
    public Workflow getWorkflow(final String category, final Document document) throws MappingException, WorkflowException, RepositoryException {
        return null;
    }

    @Override
    public String getUserIdentity() {
        return userIdentity;
    }

    @Override
    public Session getUserSession() {
        return session;
    }

    @Override
    public Session getInternalWorkflowSession() {
        return session;
    }

    @Override
    public RepositoryMap getWorkflowConfiguration() {
        return null;
    }
}
