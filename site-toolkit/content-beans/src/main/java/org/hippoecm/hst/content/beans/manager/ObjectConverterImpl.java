/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNodeUtils;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNode;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.REQUEST_CONTEXT_PREVIEW_SESSION_ATTR_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class ObjectConverterImpl implements ObjectConverter {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ObjectConverterImpl.class);

    protected Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs;
    protected Map<Class<? extends HippoBean>, String> jcrBeanPrimaryNodeTypePairs;
    protected String [] fallBackJcrNodeTypes;
    
    public ObjectConverterImpl(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs, String [] fallBackJcrNodeTypes) {
        this.jcrPrimaryNodeTypeBeanPairs = jcrPrimaryNodeTypeBeanPairs;
        this.jcrBeanPrimaryNodeTypePairs = new HashMap<Class<? extends HippoBean>, String>();
        
        for(Entry<String, Class<? extends HippoBean>> entry: jcrPrimaryNodeTypeBeanPairs.entrySet()) {
            jcrBeanPrimaryNodeTypePairs.put(entry.getValue(), entry.getKey());
        }
        
        if (fallBackJcrNodeTypes != null) {
            this.fallBackJcrNodeTypes = new String[fallBackJcrNodeTypes.length];
            System.arraycopy(fallBackJcrNodeTypes, 0, this.fallBackJcrNodeTypes, 0, fallBackJcrNodeTypes.length);
        }
    }

    public Object getObject(Session session, String path) throws ObjectBeanManagerException {
        if(StringUtils.isEmpty(path) || !path.startsWith("/")) {
            log.warn("Illegal argument for '{}' : not an absolute path", path);
            return null;
        }
        String relPath = path.substring(1);
        try {
            return getObject(session.getRootNode(), relPath);
        } catch (RepositoryException re) {
            throw new ObjectBeanManagerException("Impossible to get the object at " + path, re);
        }
           
    }

    public Object getObject(Node node, String relPath) throws ObjectBeanManagerException {
        if(StringUtils.isEmpty(relPath) || relPath.startsWith("/")) {
            log.info("'{}' is not a valid relative path. Return null.", relPath);
            return null;
        }
        if(node == null) {
            log.info("Node is null. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        String nodePath = null;
        try {
            if (node instanceof HippoBeanFrozenNode) {
                nodePath = ((HippoBeanFrozenNode)node).getFrozenNode().getPath();
            } else {
                nodePath = node.getPath();
            }
            final Node relNode = JcrUtils.getNodeIfExists(node, relPath);
            if (relNode == null) {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath , relPath);
                return null;
            }
            if (relNode.isNodeType(NT_HANDLE)) {
                // if its a handle, we want the child node. If the child node is not present,
                // this node can be ignored
                final Node document = JcrUtils.getNodeIfExists(relNode, relNode.getName());
                if (document == null) {
                    log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
                    return null;
                } else {
                    return getObject(document);
                }
            } else {
                return getObject(relNode);
            }   
            
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath, e);
            } else {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
            }
            return null;
        }
    }
    
    public Object getObject(String uuid, Session session) throws ObjectBeanManagerException {
        checkUUID(uuid);
        try {
            Node node = session.getNodeByIdentifier(uuid);
            return this.getObject(node);
        } catch (ItemNotFoundException e) {
            log.info("ItemNotFoundException for uuid '{}'. Return null.", uuid);
        } catch (RepositoryException e) {
            log.info("RepositoryException for uuid '{}' : {}. Return null.",uuid, e);
        }
        return null;
    }

    public Object getObject(String uuid, Node node) throws ObjectBeanManagerException {
        try {
            return this.getObject(uuid, node.getSession());
        } catch (RepositoryException e) {
            log.info("Failed to get object for uuid '{}'. Return null.", uuid, e);
        }
        return null;
    }
    
    public Object getObject(final Node node) throws ObjectBeanManagerException {

        String jcrPrimaryNodeType;
        String path;
        try {

            final Node useNode = getActualNode(node);

            if (useNode.isSame(useNode.getSession().getRootNode()) && getAnnotatedClassFor("rep:root") == null) {
                log.debug("Root useNode is not mapped to be resolved to a bean.");
                return null;
            }

            if(useNode.isNodeType(NT_HANDLE) ) {
                if(useNode.hasNode(useNode.getName())) {
                    return getObject(useNode.getNode(useNode.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = useNode.getPrimaryNodeType().getName();
            Class<? extends HippoBean> proxyInterfacesOrDelegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
          
            if (proxyInterfacesOrDelegateeClass == null) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.info("Encountered useNode of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                // no exact match, try a fallback type
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {
                    
                    if(!useNode.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    proxyInterfacesOrDelegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(fallBackJcrPrimaryNodeType);
                    if(proxyInterfacesOrDelegateeClass != null) {
                    	log.debug("No bean found for {}, using fallback class  {} instead", jcrPrimaryNodeType, proxyInterfacesOrDelegateeClass);
                        break;
                    }
                }
            }
            
            if (proxyInterfacesOrDelegateeClass != null) {
                Object object = ServiceFactory.create(useNode, proxyInterfacesOrDelegateeClass);
                if (object instanceof NodeAware) {
                    ((NodeAware) object).setNode(useNode);
                }
                if (object instanceof ObjectConverterAware) {
                    ((ObjectConverterAware) object).setObjectConverter(this);
                }
                return object;
            }
            path = useNode.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the object from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to convert the useNode", e);
        }
        log.info("No Descriptor found for useNode '{}'. Cannot return a Bean for '{}'.", path , jcrPrimaryNodeType);
        return null;
    }

    protected Node getActualNode(final Node node) throws RepositoryException {
        if (node instanceof HippoBeanFrozenNode) {
            return node;
        }

        if (node.isNodeType(NT_FROZEN_NODE)) {
            log.error("Unexpected {} node since we always expect a decorated node of type '{}'", NT_FROZEN_NODE,
                    HippoBeanFrozenNode.class.getName(), new Exception(String.format("Unexpected frozen node for '{}'", node.getPath())));
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(node, node.getPath());
        }

        if ((node instanceof HippoNode) && ((HippoNode) node).isVirtual()) {
            // TODO support virtual nodes for version history. (via virtual node to canonical to version history)
            return node;
        }

        if (!node.getParent().isNodeType(NT_HANDLE)) {
            return node;
        }

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            return node;
        }

        final Mount mount = requestContext.getResolvedMount().getMount();
        final Channel channel = mount.getChannel();
        if (channel == null || channel.getBranchOf() == null) {
            return node;
        }

        // should we serve a versioned history node or just workspace.

        final VersionHistory versionHistory = getVersionHistory(requestContext, node, mount);

        if (versionHistory == null) {
            // there is no history at all
            return node;
        }

        final String versionLabel;
        if (mount.isPreview()) {
            versionLabel = channel.getBranchId() + "-preview";
        } else {
            versionLabel = channel.getBranchId() + "-live";
        }
        if (!versionHistory.hasVersionLabel(versionLabel)) {
            // no version specific variant present, return workspace node
            return node;
        }

        final Version versionByLabel = versionHistory.getVersionByLabel(versionLabel);
        if (node.getSession() == versionByLabel.getSession()) {
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(versionByLabel.getFrozenNode(), node.getPath());
        }
        // node most likely belongs to live user session
        return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(node.getSession().getNode(versionByLabel.getFrozenNode().getPath()), node.getPath());

    }

    private VersionHistory getVersionHistory(final HstRequestContext requestContext, final Node documentVariant, final Mount mount) throws RepositoryException {
        if (mount.isPreview()) {
            // document is already preview, so we can directly access the version history
            if (!documentVariant.isNodeType(MIX_VERSIONABLE)) {
                return null;
            }
            return documentVariant.getSession().getWorkspace().getVersionManager().getVersionHistory(documentVariant.getPath());
        } else {
            final Session previewSession = (Session) requestContext.getAttribute(REQUEST_CONTEXT_PREVIEW_SESSION_ATTR_NAME);
            if (previewSession == null) {
                log.error("There is no preview session available which is needed for version history lookup. Return null for version history");
                return null;
            }

            if (previewSession.nodeExists(documentVariant.getPath())) {
                final Node previewVariant = previewSession.getNode(documentVariant.getPath());
                if (!previewVariant.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                    return null;
                }
                return previewSession.getWorkspace().getVersionManager().getVersionHistory(previewVariant.getPath());
            } else {
                log.debug("There is no preview variant of '{}' hence cannot retrieve version history", documentVariant.getPath());
                return null;
            }
        }
    }

    public String getPrimaryObjectType(Node node) throws ObjectBeanManagerException {
        String jcrPrimaryNodeType;
        String path;
        try {

            if(node.isNodeType(NT_HANDLE) ) {
                if(node.hasNode(node.getName())) {
                    return getPrimaryObjectType(node.getNode(node.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            boolean isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(jcrPrimaryNodeType);
          
            if (!isObjectType) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.info("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                // no exact match, try a fallback type
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {
                    
                    if(!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(fallBackJcrPrimaryNodeType);
                    if(isObjectType) {
                    	log.debug("No primary node type found for {}, using fallback type {} instead", jcrPrimaryNodeType, fallBackJcrPrimaryNodeType);
                    	jcrPrimaryNodeType = fallBackJcrPrimaryNodeType;
                        break;
                    }
                }
            }
            
            if (isObjectType) {
            	return jcrPrimaryNodeType;
            }
            path = node.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the node from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to determine node type for node", e);
        }
        log.info("No Descriptor found for node '{}'. Cannot return a matching node type for '{}'.", path , jcrPrimaryNodeType);
        return null;
    }
    
    private void checkUUID(String uuid) throws ObjectBeanManagerException{
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e){
            throw new ObjectBeanManagerException("Uuid is not parseable to a valid uuid: '"+uuid+"'");
        }
    }

    public Class<? extends HippoBean> getAnnotatedClassFor(String jcrPrimaryNodeType) {
        return this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
    }
    
    public String getPrimaryNodeTypeNameFor(Class<? extends HippoBean> hippoBean) {
        return jcrBeanPrimaryNodeTypePairs.get(hippoBean);
    }
}
