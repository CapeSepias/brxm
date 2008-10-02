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
package org.hippoecm.repository.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.EventLoggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowManagerImpl implements WorkflowManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(WorkflowManagerImpl.class);

    /** Session from which this WorkflowManager instance was created.  Is used
     * to look-up which workflows are active for a user.  It is however not
     * used to instantiate workflows, persist and as execution context when
     * performing a workflow step (i.e. method invocatin).
     */
    Session session;
    Session rootSession;
    String configuration;
    List<WorkflowInvocation> invocationChain;
    ListIterator<WorkflowInvocation> invocationIndex;
    DocumentManagerImpl documentManager;

    public WorkflowManagerImpl(Session session, Session rootSession) {
        this.session = session;
        this.rootSession = rootSession;
        documentManager = new DocumentManagerImpl(rootSession);
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH+"/"+
                    HippoNodeType.WORKFLOWS_PATH).getUUID();
        } catch (PathNotFoundException ex) {
            log.info("No workflow configuration found. Workflow not started.");
        } catch (RepositoryException ex) {
            log.error("workflow manager configuration failed: "+ex.getMessage(), ex);
        }
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    private Node getWorkflowNode(String category, Node item, Session session) {
        if (configuration==null) {
            return null;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("looking for workflow in category "+category+" for node "+(item==null ? "<none>" : item.getPath()));
            }
            
            // if the user session has not yet been saved, no workflow is possible
            // as the root session will not be able to find it.  (ItemNotFoundException)
            if (!item.isNodeType("mix:referenceable") && !item.isNodeType("rep:root")) {
                log.debug("no workflow for node because node is not mix:referenceable");
                return null;
            }
            if(!item.isNodeType("rep:root")) {
                rootSession.getNodeByUUID(item.getUUID());
            }

            Node node = session.getNodeByUUID(configuration);
            if (node.hasNode(category)) {
                node = node.getNode(category);
                Node workflowNode = null;
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    workflowNode = iter.nextNode();
                    if (workflowNode==null) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("matching item type against " +
                                  workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString());
                    }
                    if (item.isNodeType(workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString())) {
                        boolean hasPermission = true;
                        if(workflowNode.hasProperty(HippoNodeType.HIPPO_ROLES)) {
                            Value[] privileges = workflowNode.getProperty(HippoNodeType.HIPPO_ROLES).getValues();
                            for(int i=0; i<privileges.length; i++) {
                                try {
                                    item.getSession().checkPermission(item.getPath(), privileges[i].getString());
                                } catch(AccessDeniedException ex) {
                                    hasPermission = false;
                                    break;
                                }
                            }
                        }
                        if(hasPermission) {
                            if(log.isDebugEnabled()) {
                                log.debug("found workflow in category " + category + " for node " +
                                          (item==null ? "<none>" : item.getPath()));
                            }
                        return workflowNode;
                        }
                    }
                }
            } else {
                log.debug("workflow in category "+category+" for node "+(item==null ? "<none>" : item.getPath())+" not found");
            }
        } catch (ItemNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (PathNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (ValueFormatException ex) {
            log.error("misconfiguration of workflow definition");
        } catch (RepositoryException ex) {
            log.error("generic error accessing workflow definitions "+ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return null;
    }

    private Node getWorkflowNode(String category, Document document, Session session) {
        if (configuration==null) {
            return null;
        }
        if (document==null) {
            log.error("cannot retrieve workflow for non-existing document");
            return null;
        }
        try {
            Node node = session.getNodeByUUID(configuration);
            if (node.hasNode(category)) {
                node = node.getNode(category);
                Node workflowNode = null;
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    workflowNode = iter.nextNode();
                    if (workflowNode==null) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("matching document type against " +
                                workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString());
                    }
                    try {
                        Class documentClass = Class.forName(workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString());
                        Node documentNode = (document.getIdentity()!=null ? session.getNodeByUUID(document.getIdentity()) : null);
                        if ((documentNode != null && documentNode.isNodeType(workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString())) || documentClass.isAssignableFrom(document.getClass())) {
                            boolean hasPermission = true;
                            if(workflowNode.hasProperty(HippoNodeType.HIPPO_ROLES)) {
                                Value[] privileges = workflowNode.getProperty(HippoNodeType.HIPPO_ROLES).getValues();
                                for(int i=0; i<privileges.length; i++) {
                                    try {
                                        session.checkPermission(documentNode.getPath(), privileges[i].getString());
                                    } catch(AccessDeniedException ex) {
                                        hasPermission = false;
                                        break;
                                    }
                                }
                            }
                            if(hasPermission) {
                                if(log.isDebugEnabled()) {
                                    log.debug("found workflow in category "+category+" for document");
                                }
                                return workflowNode;
                            }
                        }
                    } catch (ClassNotFoundException ex) {
                    }
                }
            } else {
                log.debug("workflow in category "+category+" for document not found");
            }
        } catch (ItemNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (PathNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (ValueFormatException ex) {
            log.error("misconfiguration of workflow definition");
        } catch (RepositoryException ex) {
            log.error("generic error accessing workflow definitions "+ex.getMessage());
        }
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item, session);
        if (workflowNode!=null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode, item);
        }
        log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
        return null;
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl)descriptor;
        try {
            String path = descriptorImpl.nodeAbsPath;
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            Node node = session.getRootNode();
            if (!path.equals("")) {
                node = node.getNode(path);
            }
            return getWorkflow(descriptorImpl.category, node);
        } catch (PathNotFoundException ex) {
            log.debug("Workflow no longer available "+descriptorImpl.nodeAbsPath);
            return null;
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item, session);
        if (workflowNode!=null) {
            try {
                String classname;
                if (workflowNode.hasProperty(HippoNodeType.HIPPO_WORKFLOW))
                    classname = workflowNode.getProperty(HippoNodeType.HIPPO_WORKFLOW).getString();
                else
                    classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                Node types = workflowNode.getNode(HippoNodeType.HIPPO_TYPES);

                String uuid = null;
                /* The synchronized must operate on the core root session, because there is
                 * only one such session, while there may be many decorated ones.
                 */
                synchronized (SessionDecorator.unwrap(rootSession)) {
                    documentManager.reset();
                    Workflow workflow;
                    Class clazz = Class.forName(classname);
                    if (InternalWorkflow.class.isAssignableFrom(clazz)) {
                        try {
                            Constructor constructor = clazz.getConstructor(new Class[] {Session.class, Session.class, Node.class});
                            workflow = (Workflow)constructor.newInstance(getSession(), rootSession, item);
                        } catch (IllegalAccessException ex) {
                            throw new RepositoryException("no access to standards plugin", ex);
                        } catch (NoSuchMethodException ex) {
                            throw new RepositoryException("standards plugin invalid", ex);
                        } catch (InstantiationException ex) {
                            throw new RepositoryException("standards plugin invalid", ex);
                        } catch (InvocationTargetException ex) {
                            throw new RepositoryException("standards plugin invalid", ex);
                        }
                    } else {
                        uuid = item.getUUID();
                        Object object = documentManager.getObject(uuid, classname, types);
                        workflow = (Workflow)object;
                        if (workflow instanceof WorkflowImpl) {
                            ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContextImpl(documentManager, item));
                        }
                    }

                    try {
                        Class[] interfaces = workflow.getClass().getInterfaces();
                        Vector vector = new Vector();
                        for (int i = 0; i<interfaces.length; i++) {
                            if (Remote.class.isAssignableFrom(interfaces[i])) {
                                vector.add(interfaces[i]);
                            }
                        }
                        interfaces = (Class[])vector.toArray(new Class[vector.size()]);
                        InvocationHandler handler = new WorkflowInvocationHandler(documentManager, workflow, uuid, types);
                        Class proxyClass = Proxy.getProxyClass(workflow.getClass().getClassLoader(), interfaces);
                        workflow = (Workflow)proxyClass.getConstructor(new Class[] {InvocationHandler.class}).
                                newInstance(new Object[] {handler});

                        /*
                         * The following statement will fail under Java4, and requires Java5 and NO stub
                         * generation (through rmic).
                         *
                         * This code here, where we use a proxy to wrap a workflow class, is to have control
                         * before and after each call to a workflow.  This in order to automatically persist
                         * changes made by the workflow, and let the workflow operate in a different session.
                         * This requires intercepting each call to a workflow, which is exactly where auto-
                         * generated proxy classes are good for.
                         * However Proxy classes and RMI stub generated are not integrated in Java4.
                         *
                         * The reason for the failure is that the exportObject in Java4 will lookup the stub for
                         * the proxy class generated above.  We cannot however beforehand generate the stub for
                         * the proxy class, as these are generated on the fly.  We can also not use the stub of
                         * the original workflow, as then we would bypass calling the proxy class.  This is
                         * because the classname of the exported object must match the name of the stub class
                         * being looked up.
                         *
                         * A labor-intensive solution, to be developed if really needed, is to perform an exportObject
                         * on the original workflow (pre-wrapping it with a proxy).  But then modifying the stub
                         * generated by rmic, not to call the workflow directly, but call the proxy class.
                         * This solution is labor-intensive, hard to explain, and negates the easy to implement
                         * workflows as they are now.  So if this route is the route to go, we would be better off
                         * writing our own rmic, which performs this automatically.
                         */

                        try {
                            UnicastRemoteObject.exportObject(workflow, 0);
                        } catch (RemoteException ex) {
                            throw new RepositoryException("Problem creating workflow proxy", ex);
                        }
                    } catch (NoSuchMethodException ex) {
                        throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                    } catch (InstantiationException ex) {
                        log.error("Unable to create proxy for workflow");
                        throw new RepositoryException("Unable to create proxy for workflow", ex);
                    } catch (IllegalAccessException ex) {
                        throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                    } catch (InvocationTargetException ex) {
                        throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                    }

                    return workflow;
                }
            } catch (ClassNotFoundException ex) {
                log.error("Workflow specified at "+workflowNode.getPath()+" not present");
                throw new RepositoryException("workflow not present", ex);
            } catch (PathNotFoundException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch (ValueFormatException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            }
        }
        log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
        return null;
    }

    private Workflow getWorkflow(Node workflowNode, WorkflowChainHandler handler) throws RepositoryException, WorkflowException {
        try {
            /* The synchronized must operate on the core root session, because there is
             * only one such session, while there may be many decorated ones.
             */
            synchronized (SessionDecorator.unwrap(rootSession)) {
                Workflow workflow = null;

                String workflowClassName;
                if (workflowNode.hasProperty(HippoNodeType.HIPPO_WORKFLOW))
                    workflowClassName = workflowNode.getProperty(HippoNodeType.HIPPO_WORKFLOW).getString();
                else
                    workflowClassName = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                try {
                    Class workflowClass = Class.forName(workflowClassName);
                    Class[] interfaces = workflowClass.getInterfaces();
                    Vector vector = new Vector();
                    for (int i = 0; i<interfaces.length; i++) {
                        if (Remote.class.isAssignableFrom(interfaces[i])) {
                            vector.add(interfaces[i]);
                        }
                    }
                    interfaces = (Class[])vector.toArray(new Class[vector.size()]);
                    Class proxyClass = Proxy.getProxyClass(workflowClass.getClassLoader(), interfaces);
                    workflow = (Workflow)proxyClass.getConstructor(new Class[] {InvocationHandler.class}).
                            newInstance(new Object[] {handler});

                } catch (ClassNotFoundException ex) {
                    throw new RepositoryException("Unable to locate workflow class", ex);
                } catch (NoSuchMethodException ex) {
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                } catch (InstantiationException ex) {
                    log.error("Unable to create proxy for workflow");
                    throw new RepositoryException("Unable to create proxy for workflow", ex);
                } catch (IllegalAccessException ex) {
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                } catch (InvocationTargetException ex) {
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                }

                return workflow;
            }
        } catch (PathNotFoundException ex) {
            log.error("Workflow specification corrupt on node "+workflowNode.getPath());
            throw new RepositoryException("workflow specification corrupt", ex);
        } catch (ValueFormatException ex) {
            log.error("Workflow specification corrupt on node "+workflowNode.getPath());
            throw new RepositoryException("workflow specification corrupt", ex);
        }
    }

    Workflow getWorkflow(String category, Document document, DocumentManagerImpl documentManager) throws RepositoryException, WorkflowException {
        Node workflowNode = getWorkflowNode(category, document, documentManager.getSession());
        if (workflowNode != null) {
            return getWorkflow(workflowNode, new WorkflowChainDocumentHandler(documentManager, workflowNode, document));
        }
        log.debug("Workflow for category "+category+" on document is not available");
        throw new MappingException("Workflow for category "+category+" on document is not available");
    }

    Workflow getWorkflow(String category, Node item, DocumentManagerImpl documentManager) throws RepositoryException, WorkflowException {
        Node workflowNode = getWorkflowNode(category, item, documentManager.getSession());
        if (workflowNode != null) {
            return getWorkflow(workflowNode, new WorkflowChainItemHandler(documentManager, workflowNode, item));
        }
        log.debug("Workflow for category "+category+" on document is not available");
        throw new MappingException("Workflow for category "+category+" on document is not available");
    }

    class WorkflowInvocationHandler implements InvocationHandler {
        DocumentManagerImpl documentManager;
        Workflow upstream;
        String uuid;
        Node types;

        WorkflowInvocationHandler(DocumentManagerImpl documentManager, Workflow upstream, String uuid, Node types) {
            this.documentManager = documentManager;
            this.upstream = upstream;
            this.uuid = uuid;
            this.types = types;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method targetMethod = null;
            Object returnObject = null;
            Throwable returnException = null;

            invocationChain = new LinkedList<WorkflowInvocation>();
            invocationIndex = invocationChain.listIterator();

            try {
                String path = getPath(uuid);
                targetMethod = upstream.getClass().getMethod(method.getName(), method.getParameterTypes());
                returnObject = targetMethod.invoke(upstream, args);
                if (uuid!=null) {
                    synchronized (SessionDecorator.unwrap(rootSession)) {
                        documentManager.putObject(uuid, types, upstream);
                        rootSession.save();
                    }
                }
                if (returnObject instanceof Document) {
                    returnObject = new Document(((Document)returnObject).getIdentity());
                }
                EventLoggerImpl eventLogger = new EventLoggerImpl(rootSession);
                eventLogger.logWorkflowStep(session.getUserID(), upstream.getClass().getName(),
                        targetMethod.getName(), args, returnObject, path);

                while (!invocationChain.isEmpty()) {
                    WorkflowInvocation current = invocationChain.remove(0);
                    invocationIndex = invocationChain.listIterator();
                    current.invoke();
                }

                return returnObject;
            } catch (NoSuchMethodException ex) {
                throw returnException = new RepositoryException("Impossible failure for workflow proxy", ex);
            } catch (IllegalAccessException ex) {
                throw returnException = new RepositoryException("Impossible failure for workflow proxy", ex);
            } catch (InvocationTargetException ex) {
                ex.getCause().printStackTrace(System.err);
                throw returnException = ex.getCause();
            } finally {
                StringBuffer sb = new StringBuffer();
                sb.append("AUDIT workflow invocation ");
                sb.append(uuid);
                sb.append(".");
                sb.append(upstream!=null ? upstream.getClass().getName() : "<unknown>");
                sb.append(".");
                sb.append(method!=null ? method.getName() : "<unknown>");
                sb.append("(");
                if (args!=null) {
                    for (int i = 0; i<args.length; i++) {
                        if (i>0) {
                            sb.append(", ");
                        }
                        sb.append(args[i]!=null ? args[i].toString() : "null");
                    }
                }
                sb.append(") -> ");
                if (returnException!=null) {
                    sb.append(returnException.getClass().getName());
                } else if (returnObject!=null) {
                    sb.append(returnObject.toString());
                } else {
                    sb.append("<<null>>");
                }
                log.info(new String(sb));
            }
        }

        String getPath(String uuid) {
            if (uuid==null||uuid.equals("")) {
                return null;
            }
            try {
                Node node = session.getNodeByUUID(uuid);
                return node.getPath();
            } catch (ItemNotFoundException e) {
                return null;
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }

    private abstract class WorkflowChainHandler implements InvocationHandler {
        protected Node workflowNode;
        DocumentManagerImpl documentManager;

        WorkflowChainHandler(DocumentManagerImpl documentManager, Node workflowNode) {
            this.documentManager = documentManager;
            this.workflowNode = workflowNode;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            invoke(method, args);
            return null;
        }

        protected abstract void invoke(Method method, Object[] args);
    }

    private class WorkflowChainDocumentHandler extends WorkflowChainHandler implements InvocationHandler {
        Document workflowSubject;

        WorkflowChainDocumentHandler(DocumentManagerImpl documentManager, Node workflowNode, Document document) {
            super(documentManager, workflowNode);
            this.workflowSubject = document;
        }

        @Override
        protected void invoke(Method method, Object[] args) {
            invocationIndex.add(new WorkflowInvocation(documentManager, workflowNode, workflowSubject, method, args));
        }
    }

    private class WorkflowChainItemHandler extends WorkflowChainHandler implements InvocationHandler {
        Node workflowSubject;

        WorkflowChainItemHandler(DocumentManagerImpl documentManager, Node workflowNode, Node item) {
            super(documentManager, workflowNode);
            this.workflowSubject = item;
        }

        @Override
        protected void invoke(Method method, Object[] args) {
            invocationIndex.add(new WorkflowInvocation(documentManager, workflowNode, workflowSubject, method, args));
        }
    }

    private class WorkflowInvocation {
        DocumentManagerImpl documentManager;
        Node workflowNode;
        Document workflowSubject;
        Node workflowSubjectNode;
        Method method;
        Object[] arguments;

        WorkflowInvocation(DocumentManagerImpl documentManager, Node workflowNode, Document workflowSubject, Method method, Object[] args) {
            this.documentManager = documentManager;
            this.workflowNode = workflowNode;
            this.workflowSubject = workflowSubject;
            this.method = method;
            this.arguments = (args!=null ? args.clone() : args);
            this.workflowSubjectNode = null;
        }

        WorkflowInvocation(DocumentManagerImpl documentManager, Node workflowNode, Node workflowSubject, Method method, Object[] args) {
            this.documentManager = documentManager;
            this.workflowNode = workflowNode;
            this.workflowSubject = null;
            this.method = method;
            this.arguments = (args!=null ? args.clone() : args);
            this.workflowSubjectNode = workflowSubject;
        }

        Object invoke() throws RepositoryException {
            try {
                Workflow workflow;
                String classname;
                if (workflowNode.hasProperty(HippoNodeType.HIPPO_WORKFLOW))
                    classname = workflowNode.getProperty(HippoNodeType.HIPPO_WORKFLOW).getString();
                else
                    classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                Node types = workflowNode.getNode(HippoNodeType.HIPPO_TYPES);
                String uuid = null;

                Node item = workflowSubjectNode;
                if (item==null) {
                    item = rootSession.getNodeByUUID(workflowSubject.getIdentity());
                }
                if (classname.startsWith("org.hippoecm.repository.standardworkflow.")) {
                    try {
                        Class clazz = Class.forName(classname);
                        Constructor constructor = clazz.getConstructor(new Class[] {Session.class, Session.class, Node.class});
                        workflow = (Workflow)constructor.newInstance(getSession(), rootSession, item);
                    } catch (IllegalAccessException ex) {
                        throw new RepositoryException("no access to standards plugin", ex);
                    } catch (ClassNotFoundException ex) {
                        throw new RepositoryException("standards plugin missing", ex);
                    } catch (NoSuchMethodException ex) {
                        throw new RepositoryException("standards plugin invalid", ex);
                    } catch (InstantiationException ex) {
                        throw new RepositoryException("standards plugin invalid", ex);
                    } catch (InvocationTargetException ex) {
                        throw new RepositoryException("standards plugin invalid", ex);
                    }
                } else {
                    uuid = item.getUUID();
                    Object object = documentManager.getObject(uuid, classname, types);
                    workflow = (Workflow)object;
                    if (workflow instanceof WorkflowImpl) {
                        ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContextImpl(documentManager, item));
                    }
                }

                Method targetMethod = workflow.getClass().getMethod(method.getName(), method.getParameterTypes());
                Object returnObject = targetMethod.invoke(workflow, arguments);
                if (uuid!=null) {
                    synchronized (SessionDecorator.unwrap(rootSession)) {
                        documentManager.putObject(uuid, types, workflow);
                        rootSession.save();
                    }
                }
                return returnObject;
            } catch (InvocationTargetException ex) {
                throw new RepositoryException("standards plugin invalid", ex);
            } catch (NoSuchMethodException ex) {
                throw new RepositoryException("standards plugin invalid", ex);
            } catch (IllegalAccessException ex) {
                throw new RepositoryException("no access to standards plugin", ex);
            } catch (PathNotFoundException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch (ValueFormatException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch (NoSuchNodeTypeException ex) {
                throw ex;
            } catch (LockException ex) {
                throw ex;
            } catch (VersionException ex) {
                throw ex;
            } catch (ConstraintViolationException ex) {
                throw ex;
            } catch (AccessDeniedException ex) {
                throw ex;
            } catch (ItemNotFoundException ex) {
                throw ex;
            } catch (UnsupportedRepositoryOperationException ex) {
                throw ex;
            } catch (ItemExistsException ex) {
                throw ex;
            } catch (InvalidItemStateException ex) {
                throw ex;
            }
        }
    }

    private class WorkflowContextImpl implements WorkflowContext {
        Node subject;
        DocumentManagerImpl documentManager;

        WorkflowContextImpl(DocumentManagerImpl documentManager, Node subject) {
            this.documentManager = documentManager;
            this.subject = subject;
        }

        public Document getDocument(String category, String identifier) throws RepositoryException {
            return documentManager.getDocument(category, identifier);
        }

        public Workflow getWorkflow(String category, Document document) throws MappingException, WorkflowException, RepositoryException {
            return WorkflowManagerImpl.this.getWorkflow(category, document, documentManager);
        }

        public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
            return WorkflowManagerImpl.this.getWorkflow(category, subject, documentManager);
        }

        public String getUsername() {
            return session.getUserID();
        }
    }
}
