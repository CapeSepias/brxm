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
package org.hippoecm.repository.reviewedactions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;

public abstract class ReviewedActionsWorkflowAbstractTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected static final String LOREM = "Lorem ipsum dolor sit amet, consectetaur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum";

    protected HippoRepository server;
    protected Session session;
    protected WorkflowManager workflowMgr = null;

    @Before
    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node, root = session.getRootNode();

        // set up the workflow specification as a node "/hippo:configuration/hippo:workflows/default/reviewedactions"
        node = root.getNode("hippo:configuration");
        if (node.hasNode("hippo:workflows"))
            node.getNode("hippo:workflows").remove();
        node = node.addNode("hippo:workflows", "hippo:workflowfolder");
        node.addMixin("mix:referenceable");
        Node wfs = node.addNode("default", "hippo:workflowcategory");

        node = wfs.addNode("reviewedactions", "hippo:workflow");
        node.setProperty("hippo:nodetype", "hippostd:publishable");
        node.setProperty("hippo:display", "Reviewed actions workflow");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflowImpl");
        Node types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublishableDocument", "hippo:type");
        node.setProperty("hippo:nodetype", "hippostd:publishable");
        node.setProperty("hippo:display", "PublishableDocument");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublishableDocument");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublicationRequest", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:request");
        node.setProperty("hippo:display", "PublicationRequest");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublicationRequest");

        node = wfs.addNode("reviewedrequests", "hippo:workflow");
        node.setProperty("hippo:nodetype", "hippo:request");
        node.setProperty("hippo:display", "Reviewed requests workflow");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.FullRequestWorkflowImpl");
        types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublishableDocument", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:publishable");
        node.setProperty("hippo:display", "PublishableDocument");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublishableDocument");
        node = types.addNode("org.hippoecm.repository.reviewedactions.PublicationRequest", "hippo:type");
        node.setProperty("hippo:nodetype", "hippo:request");
        node.setProperty("hippo:display", "PublicationRequest");
        node.setProperty("hippo:classname", "org.hippoecm.repository.reviewedactions.PublicationRequest");

        session.save();
    }

    @After
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("documents"))
            session.getRootNode().getNode("documents").remove();
        session.save();
        server.close();
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
