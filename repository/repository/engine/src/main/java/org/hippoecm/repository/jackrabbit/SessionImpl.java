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
package org.hippoecm.repository.jackrabbit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.util.XMLChar;

import org.hippoecm.repository.jackrabbit.xml.DefaultContentHandler;
import org.hippoecm.repository.security.HippoAMContext;

public class SessionImpl extends org.apache.jackrabbit.core.SessionImpl {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    private final SessionImplHelper helper;

    protected SessionImpl(RepositoryImpl rep, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(rep, loginContext, wspConfig);
        helper = new SessionImplHelper(this, ntMgr, rep, loginContext.getSubject()) {
            @Override
            SessionItemStateManager getItemStateManager() {
                return itemStateMgr;
            }
        };
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)((WorkspaceImpl)wsp).getItemStateManager();
        rep.initializeLocalItemStateManager(localISM, this, subject);
    }

    protected SessionImpl(RepositoryImpl rep, Subject subject, WorkspaceConfig wspConfig) throws AccessDeniedException,
                                                                                                 RepositoryException {
        super(rep, subject, wspConfig);
        helper = new SessionImplHelper(this, ntMgr, rep, subject) {
            @Override
            SessionItemStateManager getItemStateManager() {
                return itemStateMgr;
            }
        };
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)((WorkspaceImpl)wsp).getItemStateManager();
        rep.initializeLocalItemStateManager(localISM, this, subject);
    }

    @Override
    protected AccessManager createAccessManager(Subject subject, HierarchyManager hierMgr) throws AccessDeniedException, RepositoryException {
        AccessManagerConfig amConfig = rep.getConfig().getAccessManagerConfig();
        try {
            HippoAMContext ctx = new HippoAMContext(new File(((RepositoryImpl)rep).getConfig().getHomeDir()),
                    ((RepositoryImpl)rep).getFileSystem(),
                    this, subject, hierMgr, this, getWorkspace().getName(), ntMgr, getItemStateManager());
            AccessManager accessMgr = (AccessManager)amConfig.newInstance();
            accessMgr.init(ctx);
            return accessMgr;
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = "failed to instantiate AccessManager implementation: "+amConfig.getClassName();
            log.error(msg, ex);
            throw new RepositoryException(msg, ex);
        }
    }

    @Override
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        try {
            super.checkPermission(absPath, actions);
        } catch(IllegalArgumentException ex) {
        }
        helper.checkPermission(absPath, actions);
    }

    @Override
    protected org.apache.jackrabbit.core.WorkspaceImpl createWorkspaceInstance(WorkspaceConfig wspConfig,
                                                                               SharedItemStateManager stateMgr, org.apache.jackrabbit.core.RepositoryImpl rep,
                                                                               org.apache.jackrabbit.core.SessionImpl session) {
        return new WorkspaceImpl(wspConfig, stateMgr, rep, this);
    }

    @Override
    protected SessionItemStateManager createSessionItemStateManager(LocalItemStateManager manager) {
        return new HippoSessionItemStateManager(((RepositoryImpl)rep).getRootNodeId(), manager, ((RepositoryImpl)rep).getNodeTypeRegistry());
    }

    @Override
    protected org.apache.jackrabbit.core.ItemManager createItemManager(SessionItemStateManager itemStateMgr, HierarchyManager hierMgr) {
        return new ItemManager(itemStateMgr, hierMgr, this, ntMgr.getRootNodeDefinition(), ((RepositoryImpl)rep).getRootNodeId());
    }

    @Override
    public String getUserID() {
        return helper.getUserID();
    }

    /**
     * Method to expose the authenticated users' principals
     * @return Set An unmodifiable set containing the principals
     */
    public Set<Principal> getUserPrincipals() {
        return helper.getUserPrincipals();
    }
    
    @Override
    public void logout() {
        helper.logout();
    }

    //------------------------------------------------< Namespace handling >--
    @Override
    public String getNamespacePrefix(String uri)
            throws NamespaceException, RepositoryException {
        // accessmanager is instantiated before the helper is set
        if (helper == null) {
            return super.getNamespacePrefix(uri);
        }
        return helper.getNamespacePrefix(uri);
    }

    @Override
    public String getNamespaceURI(String prefix)
            throws NamespaceException, RepositoryException {
        // accessmanager is instantiated before the helper is set
        if (helper == null) {
            return super.getNamespaceURI(prefix);
        }
        return helper.getNamespaceURI(prefix);
    }

    @Override
    public String[] getNamespacePrefixes()
            throws RepositoryException {
        return helper.getNamespacePrefixes();
    }

    @Override
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        helper.setNamespacePrefix(prefix, uri);
    }


    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        return helper.pendingChanges(node, nodeType, prune);
    }

    public ContentHandler getDereferencedImportContentHandler(String parentAbsPath, int uuidBehavior,
            int referenceBehavior, int mergeBehavior) throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        return helper.getDereferencedImportContentHandler(parentAbsPath, uuidBehavior, referenceBehavior, mergeBehavior);
    }

    public void importDereferencedXML(String parentAbsPath, InputStream in, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
            RepositoryException {
        ContentHandler handler =
            getDereferencedImportContentHandler(parentAbsPath, uuidBehavior, referenceBehavior, mergeBehavior);
        new DefaultContentHandler(handler).parse(in);
    }


    @Override
    public SessionItemStateManager getItemStateManager() {
        return super.getItemStateManager();
    }

    public Node getCanonicalNode(Node node) throws RepositoryException {
        return helper.getCanonicalNode((NodeImpl)node);
    }

    @Override
    public void finalize() {
        if(log.isDebugEnabled()) {
            super.finalize();
        } else {
            if (alive) {
                log.info("Unclosed session detected.");
                logout();
                alive = false;
            }
        }
    }
}
