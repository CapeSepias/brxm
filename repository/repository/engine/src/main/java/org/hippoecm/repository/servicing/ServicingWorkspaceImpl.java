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
package org.hippoecm.repository.servicing;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.xml.sax.ContentHandler;

/**
 * Simple workspace decorator.
 */
public class ServicingWorkspaceImpl extends AbstractDecorator implements HippoWorkspace {
    final static String SVN_ID = "$Id$";

    /** The underlying workspace instance. */
    protected final Workspace workspace;
    protected DocumentManager documentManager;
    protected WorkflowManager workflowManager;

    /**
     * Creates a workspace decorator.
     *
     * @param factory
     * @param session
     * @param workspace
     */
    public ServicingWorkspaceImpl(DecoratorFactory factory, Session session, Workspace workspace) {
        super(factory, session);
        this.workspace = workspace;
        documentManager = null;
        workflowManager = null;
    }

    /** {@inheritDoc} */
    public Session getSession() {
        return session;
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public String getName() {
        return workspace.getName();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.copy(srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        workspace.copy(srcWorkspace, srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.move(srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        Version[] tmp = new Version[versions.length];
        for (int i = 0; i < versions.length; i++) {
            tmp[i] = VersionDecorator.unwrap(versions[i]);
        }
        workspace.restore(tmp, removeExisting);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public QueryManager getQueryManager() throws RepositoryException {
        return factory.getQueryManagerDecorator(session, workspace.getQueryManager());
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        if (documentManager == null) {
            documentManager = new DocumentManagerImpl(session);
        }
        return documentManager;
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        if (workflowManager == null) {
            workflowManager = new WorkflowManagerImpl(session);
        }
        return workflowManager;
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return workspace.getNamespaceRegistry();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return workspace.getNodeTypeManager();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        return workspace.getObservationManager();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        return workspace.getAccessibleWorkspaceNames();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehaviour)
            throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return workspace.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void importXML(String parentAbsPath, InputStream in, int uuidBehaviour) throws IOException,
            PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException,
            LockException, RepositoryException {
        workspace.importXML(parentAbsPath, in, uuidBehaviour);
    }

}
