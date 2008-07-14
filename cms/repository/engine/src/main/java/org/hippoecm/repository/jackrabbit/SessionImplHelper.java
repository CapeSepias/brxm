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

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.decorating.NodeDecorator;
import org.hippoecm.repository.jackrabbit.xml.DereferencedImportHandler;
import org.hippoecm.repository.jackrabbit.xml.DereferencedSessionImporter;
import org.hippoecm.repository.security.principals.AdminPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

abstract class SessionImplHelper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    /**
     * the user ID that was used to acquire this session
     */
    private String userId;

    NodeTypeManagerImpl ntMgr;
    RepositoryImpl rep;
    Subject subject;
    org.apache.jackrabbit.core.SessionImpl sessionImpl;

    SessionImplHelper(org.apache.jackrabbit.core.SessionImpl sessionImpl,
                      NodeTypeManagerImpl ntMgr, RepositoryImpl rep, Subject subject) throws RepositoryException {
        this.sessionImpl = sessionImpl;
        this.ntMgr = ntMgr;
        this.rep = rep;
        this.subject = subject;
        setUserId();
    }

    /**
     * Override jackrabbits default userid, because it just uses
     * the first principal it can find, which can lead to strange "usernames"
     */
    protected void setUserId() {
        if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
            Principal principal = (Principal)subject.getPrincipals(SystemPrincipal.class).iterator().next();
            userId = principal.getName();
        } else if (!subject.getPrincipals(AdminPrincipal.class).isEmpty()) {
            Principal principal = (Principal)subject.getPrincipals(AdminPrincipal.class).iterator().next();
            userId = principal.getName();
        } else if (!subject.getPrincipals(UserPrincipal.class).isEmpty()) {
            Principal principal = (Principal)subject.getPrincipals(UserPrincipal.class).iterator().next();
            userId = principal.getName();
        } else if (!subject.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
            Principal principal = (Principal)subject.getPrincipals(AnonymousPrincipal.class).iterator().next();
            userId = principal.getName();
        } else {
            userId = "Unknown";
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return userId;
    }

    /**
     * Method to expose the authenticated users' principals
     * @return Set An unmodifialble set containing the principals
     */
    public Set<Principal> getUserPrincipals() {
        return Collections.unmodifiableSet(subject.getPrincipals());
    }

    abstract SessionItemStateManager getItemStateManager();

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                                         NoSuchNodeTypeException, RepositoryException {
        Name ntName;
        try {
            ntName = (nodeType!=null ? sessionImpl.getQName(nodeType) : null);
        } catch (IllegalNameException ex) {
            throw new NoSuchNodeTypeException(nodeType);
        }
        final Set<NodeId> filteredResults = new HashSet<NodeId>();
        if (node==null) {
            node = sessionImpl.getRootNode();
            if (node.isModified()&&(nodeType==null||node.isNodeType(nodeType))) {
                filteredResults.add(((org.apache.jackrabbit.core.NodeImpl)node).getNodeId());
            }
        }
        NodeId nodeId = ((org.apache.jackrabbit.core.NodeImpl)NodeDecorator.unwrap(node)).getNodeId();

        Iterator iter = getItemStateManager().getDescendantTransientItemStates(nodeId);
        while (iter.hasNext()) {
            ItemState itemState = (ItemState)iter.next();
            NodeState state = null;
            if (!itemState.isNode()) {
                try {
                    if (filteredResults.contains(itemState.getParentId()))
                        continue;
                    state = (NodeState)getItemStateManager().getItemState(itemState.getParentId());
                //continue;
                } catch (NoSuchItemStateException ex) {
                    log.error("Cannot find parent of changed property", ex);
                    continue;
                } catch (ItemStateException ex) {
                    log.error("Cannot find parent of changed property", ex);
                    continue;
                }
            } else {
                state = (NodeState)itemState;
            }

            /* if the node type of the current node state is not of required
             * type (if set), continue with next.
             */
            if (nodeType!=null) {
                if (!ntName.equals(state.getNodeTypeName())) {
                    Set mixins = state.getMixinTypeNames();
                    if (!mixins.contains(ntName)) {
                        // build effective node type of mixins & primary type
                        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
                        Name[] types = new Name[mixins.size()+1];
                        mixins.toArray(types);
                        types[types.length-1] = state.getNodeTypeName();
                        try {
                            if (!ntReg.getEffectiveNodeType(types).includesNodeType(ntName))
                                continue;
                        } catch (NodeTypeConflictException ntce) {
                            String msg = "internal error: failed to build effective node type";
                            log.debug(msg);
                            throw new RepositoryException(msg, ntce);
                        }
                    }
                }
            }

            /* if pruning, check that there are already children in the
             * current list.  If so, remove them.
             */
            if (prune) {
                HierarchyManager hierMgr = sessionImpl.getHierarchyManager();
                for (Iterator<NodeId> i = filteredResults.iterator(); i.hasNext();) {
                    if (hierMgr.isAncestor(state.getNodeId(), i.next()))
                        i.remove();
                }
            }

            filteredResults.add(state.getNodeId());
        }

        return new NodeIterator() {
            private final org.apache.jackrabbit.core.ItemManager itemMgr = sessionImpl.getItemManager();
            private Iterator<NodeId> iterator = filteredResults.iterator();
            private int pos = 0;

            public Node nextNode() {
                return (Node)next();
            }

            public long getPosition() {
                return pos;
            }

            public long getSize() {
                return -1;
            }

            public void skip(long skipNum) {
                if (skipNum<0) {
                    throw new IllegalArgumentException("skipNum must not be negative");
                } else if (skipNum==0) {
                    return;
                } else {
                    do {
                        NodeId id = iterator.next();
                        ++pos;
                    } while (--skipNum>0);
                }
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Object next() {
                try {
                    NodeId id = iterator.next();
                    ++pos;
                    return itemMgr.getItem(id);
                } catch (AccessDeniedException ex) {
                    return null;
                } catch (ItemNotFoundException ex) {
                    return null;
                } catch (RepositoryException ex) {
                    return null;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }
    
    /**
     * {@inheritDoc}
     */
    public ContentHandler getDereferencedImportContentHandler(String parentAbsPath, int uuidBehavior,
            int referenceBehavior, int mergeBehavior) throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {

        // check sanity of this session
        if (!sessionImpl.isLive()) {
            throw new RepositoryException("this session has been closed");
        }

        NodeImpl parent;
        try {
            Path p = sessionImpl.getQPath(parentAbsPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + parentAbsPath);
            }
            parent = sessionImpl.getItemManager().getNode(p);
        } catch (NameException e) {
            String msg = parentAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(parentAbsPath);
        }

        // verify that parent node is checked-out
        if (!parent.isNew()) {
            NodeImpl node = parent;
            while (node.getDepth() != 0) {
                if (node.hasProperty(NameConstants.JCR_ISCHECKEDOUT)) {
                    if (!node.getProperty(NameConstants.JCR_ISCHECKEDOUT).getBoolean()) {
                        String msg = parentAbsPath + ": cannot add a child to a checked-in node";
                        log.debug(msg);
                        throw new VersionException(msg);
                    }
                }
                node = (NodeImpl) node.getParent();
            }
        }


        // check protected flag of parent node
        if (parent.getDefinition().isProtected()) {
            String msg = parentAbsPath + ": cannot add a child to a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        if (!parent.isNew()) {
            sessionImpl.getLockManager().checkLock(parent);
        }

        DereferencedSessionImporter importer = new DereferencedSessionImporter(parent, sessionImpl, uuidBehavior, referenceBehavior, mergeBehavior);
        //return new ImportHandler(importer, sessionImpl.getNamespaceResolver(), rep.getNamespaceRegistry());
        return new DereferencedImportHandler(importer, sessionImpl.getNamespaceResolver(), rep.getNamespaceRegistry());
    }
}
