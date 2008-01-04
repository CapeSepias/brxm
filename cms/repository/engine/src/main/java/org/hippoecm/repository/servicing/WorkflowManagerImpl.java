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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowImpl;

public class WorkflowManagerImpl implements WorkflowManager {
    final Logger log = LoggerFactory.getLogger(Workflow.class);

    Session session;
    String configuration;

    public WorkflowManagerImpl(Session session) {
        this.session = session;
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" +
                                                          HippoNodeType.WORKFLOWS_PATH).getUUID();
        } catch (PathNotFoundException ex) {
            log.info("No workflow configuration found. Workflow not started.");
        } catch(RepositoryException ex) {
            log.error("workflow manager configuration failed: "+ex.getMessage(), ex);
        }
    }

    private Node getWorkflowNode(String category, Node item) {
        if(configuration == null) {
            return null;
        }
        try {
            log.debug("looking for workflow in category " + category + " for node " + (item == null ? "<none>" : item.getPath()));
            Node node = session.getNodeByUUID(configuration);
            if (node.hasNode(category)) {
                node = node.getNode(category);
                Node workflowNode = null;
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    workflowNode = iter.nextNode();
                    if(workflowNode == null)
                        continue;
                    log.debug("matching item type against " + workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString());
                    if (item.isNodeType(workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString())) {
                        log.debug("found workflow in category " + category + " for node " + (item == null ? "<none>" : item.getPath()));
                        return workflowNode;
                    }
                }
            } else {
                log.debug("workflow in category " + category + " for node " + (item == null ? "<none>" : item.getPath()) + " not found");
            }
        } catch (ItemNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing " + ex.getMessage());
        } catch (PathNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing " + ex.getMessage());
        } catch (ValueFormatException ex) {
            log.error("misconfiguration of workflow definition");
        } catch (RepositoryException ex) {
            log.error("generic error accessing workflow definitions " + ex.getMessage());
        }
        return null;
    }

    public WorkflowManagerImpl(Session session, String uuid) {
        this.session = session;
        configuration = uuid;
    }

    void save(Workflow workflow, String uuid, Node types) throws RepositoryException {
        HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        DocumentManagerImpl documentManager = (DocumentManagerImpl) workspace.getDocumentManager();
        documentManager.putObject(uuid, types, workflow);
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item);
        if(workflowNode != null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode, item);
        } else {
            log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
            return null;
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl) descriptor;
        try {
            String path = descriptorImpl.nodeAbsPath;
            if(path.startsWith("/"))
                path = path.substring(1);
            Node node = session.getRootNode();
            if (!path.equals(""))
                node = node.getNode(path);
            return getWorkflow(descriptorImpl.category, node);
        } catch(PathNotFoundException ex) {
            log.debug("Workflow no longer available "+descriptorImpl.nodeAbsPath);
            return null;
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item);
        if(workflowNode != null) {
            try {
                String classname = workflowNode.getProperty(HippoNodeType.HIPPO_SERVICE).getString();
                Node types = workflowNode.getNode(HippoNodeType.HIPPO_TYPES);
                DocumentManagerImpl manager = (DocumentManagerImpl) ((HippoWorkspace)session.getWorkspace())
                    .getDocumentManager();
                String uuid = item.getUUID();
                Object object = manager.getObject(uuid, classname, types);
                Workflow workflow = (Workflow) object;
                if(workflow instanceof WorkflowImpl) {
                    ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContext(session));
                }

                try {
                    Class[] interfaces = workflow.getClass().getInterfaces();
                    Vector vector = new Vector();
                    for(int i=0; i<interfaces.length; i++)
                        if(Remote.class.isAssignableFrom(interfaces[i])) {
                            vector.add(interfaces[i]);
                        }
                    interfaces = (Class[]) vector.toArray(new Class[vector.size()]);
                    InvocationHandler handler = new WorkflowInvocationHandler(workflow, uuid, types);
                    Class proxyClass = Proxy.getProxyClass(workflow.getClass().getClassLoader(), interfaces);
                    workflow = (Workflow) proxyClass.getConstructor(new Class[] { InvocationHandler.class }).
                        newInstance(new Object[] { handler });

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
                        java.rmi.server.UnicastRemoteObject.exportObject(workflow, 0);
                    } catch(java.rmi.RemoteException ex) {
                        throw new RepositoryException("Problem creating workflow proxy", ex);
                    }
                } catch(NoSuchMethodException ex) {
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                } catch(InstantiationException ex) {
                    log.error("Unable to create proxy for workflow");
                    throw new RepositoryException("Unable to create proxy for workflow", ex);
                } catch(IllegalAccessException ex) {
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                } catch(InvocationTargetException ex) {
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                }

                return workflow;
            } catch(PathNotFoundException ex) {
                log.error("Workflow specification corrupt on node " + workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch(ValueFormatException ex) {
                log.error("Workflow specification corrupt on node " + workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            }
        } else {
            log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
            return null;
        }
    }

    class WorkflowInvocationHandler implements InvocationHandler {
        Workflow upstream;
        String uuid;
        Node types;
        WorkflowInvocationHandler(Workflow upstream, String uuid, Node types) {
            this.upstream = upstream;
            this.uuid = uuid;
            this.types = types;
        }
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                Method targetMethod = upstream.getClass().getMethod(method.getName(), method.getParameterTypes());
                Object returnObject = targetMethod.invoke(upstream, args);
                save(upstream, uuid, types);
                return returnObject;
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException("Impossible failure for workflow proxy", ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException("Impossible failure for workflow proxy", ex);
            } catch(InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }
}
