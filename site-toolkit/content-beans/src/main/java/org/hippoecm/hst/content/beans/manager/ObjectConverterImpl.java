/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.dynamic.AutoEnhancedBean;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanService;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanServiceImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNode;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNodeUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_VERSIONLABELS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.onehippo.repository.util.JcrConstants.JCR_FROZEN_NODE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;
import static org.onehippo.repository.util.JcrConstants.NT_VERSION_HISTORY;

public class ObjectConverterImpl implements ObjectConverter {

    private static final Logger log = LoggerFactory.getLogger(ObjectConverterImpl.class);

    protected Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs;
    protected Map<Class<? extends HippoBean>, String> jcrBeanPrimaryNodeTypePairs;
    protected String[] fallBackJcrNodeTypes;
    protected DynamicBeanService dynamicBeanService;
    
    //TODO: After we integrate content type service, there is no need to this field.
    private Set<String> dynabeans = new HashSet<String>();

    public ObjectConverterImpl(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs, String[] fallBackJcrNodeTypes) {
        this.jcrPrimaryNodeTypeBeanPairs = jcrPrimaryNodeTypeBeanPairs;
        this.jcrBeanPrimaryNodeTypePairs = new HashMap<>();

        for (Entry<String, Class<? extends HippoBean>> entry : jcrPrimaryNodeTypeBeanPairs.entrySet()) {
            jcrBeanPrimaryNodeTypePairs.put(entry.getValue(), entry.getKey());
        }

        if (fallBackJcrNodeTypes != null) {
            this.fallBackJcrNodeTypes = new String[fallBackJcrNodeTypes.length];
            System.arraycopy(fallBackJcrNodeTypes, 0, this.fallBackJcrNodeTypes, 0, fallBackJcrNodeTypes.length);
        }

        dynamicBeanService = new DynamicBeanServiceImpl();
    }

    public Object getObject(Session session, String path) throws ObjectBeanManagerException {
        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {
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
        if (StringUtils.isEmpty(relPath) || relPath.startsWith("/")) {
            log.info("'{}' is not a valid relative path. Return null.", relPath);
            return null;
        }
        if (node == null) {
            log.info("Node is null. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        String nodePath = null;
        try {
            if (node instanceof HippoBeanFrozenNode) {
                nodePath = ((HippoBeanFrozenNode) node).getFrozenNode().getPath();
            } else {
                nodePath = node.getPath();
            }
            final Node relNode = JcrUtils.getNodeIfExists(node, relPath);
            if (relNode == null) {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
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
            log.info("RepositoryException for uuid '{}' : {}. Return null.", uuid, e);
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

            if (useNode.isNodeType(NT_HANDLE)) {
                if (useNode.hasNode(useNode.getName())) {
                    return getObject(useNode.getNode(useNode.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = useNode.getPrimaryNodeType().getName();
            Class<? extends HippoBean> delegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);

            if (delegateeClass != null && useNode.isNodeType(NT_DOCUMENT) && useNode.getParent().isNodeType(NT_HANDLE)) {
                //Check if the document bean is marked as enhanced, which means this bean will be generated by dynamic bean service
                AutoEnhancedBean autoEnhanceBean = delegateeClass.getDeclaredAnnotation(AutoEnhancedBean.class);
                if (autoEnhanceBean != null && autoEnhanceBean.allowModifications()) {
                    delegateeClass = createDynamicBean(node, delegateeClass);
                }
            }
            
            if (delegateeClass == null) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.info("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                //TODO: Enhancing of existing bean is not supported while creating beans only the fly, but it could be.
                if (useNode.isNodeType(HippoNodeType.NT_DOCUMENT) && useNode.getParent().isNodeType(NT_HANDLE)) {
                    // There isn't any java bean class for this document so it will be generated on the fly.
                    delegateeClass = createDynamicBean(useNode, null);
                } else {
                    // no exact match, try a fallback type
                    for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {

                        if (!useNode.isNodeType(fallBackJcrPrimaryNodeType)) {
                            continue;
                        }
                        // take the first fallback type
                        delegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(fallBackJcrPrimaryNodeType);
                        if (delegateeClass != null) {
                            log.debug("No bean found for {}, using fallback class  {} instead", jcrPrimaryNodeType, delegateeClass);
                            break;
                        }
                    }
                }
            }
            
            if (delegateeClass != null) {
				Object object = ServiceFactory.create(delegateeClass);
				if (object != null) {
					if (object instanceof NodeAware) {
						((NodeAware) object).setNode(useNode);
					}
					if (object instanceof ObjectConverterAware) {
						((ObjectConverterAware) object).setObjectConverter(this);
					}
				}
                return object;
            }
            path = useNode.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the object from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to convert the useNode", e);
        }
        log.info("No Descriptor found for useNode '{}'. Cannot return a Bean for '{}'.", path, jcrPrimaryNodeType);
        return null;
    }

    //TODO: After we integrate content type service for JCR events, there is no need to this method.
    public void resetType(String type){
        if (dynabeans.contains(type)){
            Class cl = jcrPrimaryNodeTypeBeanPairs.get(type);
            jcrPrimaryNodeTypeBeanPairs.remove(type);
            jcrBeanPrimaryNodeTypePairs.remove(cl);
        }
    }
    
    private Class<? extends HippoBean> createDynamicBean(final Node node, Class<? extends HippoBean> baseDocument) throws RepositoryException, ObjectBeanManagerException {

        Class<? extends HippoBean> dynamicType = null;

        final String beanType = node.getPrimaryNodeType().getName();

        if (baseDocument == null) {
            baseDocument = getProjectBaseDocument();
        }
        if (baseDocument != null) {
            dynamicType = dynamicBeanService.createBean(node, baseDocument);
        } else {
            dynamicType = dynamicBeanService.createBean(node);
        }

        this.jcrBeanPrimaryNodeTypePairs.put(dynamicType, beanType);
        this.jcrPrimaryNodeTypeBeanPairs.put(beanType, dynamicType);

        this.dynabeans.add(beanType);

        return dynamicType;
    }

    private Class<? extends HippoBean> getProjectBaseDocument() {
        //returns the BaseDocument class if the project has one.
        for (Class<? extends HippoBean> cls : jcrBeanPrimaryNodeTypePairs.keySet()) {
            org.hippoecm.hst.content.beans.Node node = cls.getDeclaredAnnotation(org.hippoecm.hst.content.beans.Node.class);
            if (node != null && node.jcrType() != null && node.jcrType().endsWith(":basedocument")) {
                return cls;
            }
        }
        return null;
    }
    
    protected Node getActualNode(final Node node) throws RepositoryException {
        if (node instanceof HippoBeanFrozenNode) {
            return node;
        }

        if (node.isNodeType(NT_FROZEN_NODE)) {
            log.error("Unexpected {} node since we always expect a decorated node of type '{}'", NT_FROZEN_NODE,
                    HippoBeanFrozenNode.class.getName(), new Exception(String.format("Unexpected frozen node for '{}'", node.getPath())));
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(node, node.getPath(), node.getName());
        }

        final Node canonicalNode;
        if ((node instanceof HippoNode) && ((HippoNode) node).isVirtual()) {
            final Node canonical = ((HippoNode) node).getCanonicalNode();
            if (canonical == null) {
                // virtual only, there is never a versioned node for it
                return node;
            }
            if (!(canonical.isNodeType(NT_DOCUMENT) && canonical.getParent().isNodeType(NT_HANDLE))) {
                // not a document
                return node;
            }
            canonicalNode = canonical;
        } else {
            canonicalNode = node;
        }

        final Node handle  = canonicalNode.getParent();

        if (!handle.isNodeType(NT_HANDLE)) {
            // node is not a variant below the handle
            return node;
        }

        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            // no version history information on handle, return
            return node;
        }

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            return node;
        }

        final String branchId = HstRequestUtils.getBranchIdFromContext(requestContext);
        final String branchIdOfNode = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, BranchConstants.MASTER_BRANCH_ID);
        if (branchIdOfNode.equals(branchId)) {
            return node;
        }

        if (!handle.hasProperty(HIPPO_VERSION_HISTORY_PROPERTY)) {
            // Without a version history identifier we can't find it in version history
            return node;
        }

        // should we serve a versioned history node or just workspace.
        try {
            final Node versionHistory = node.getSession().getNodeByIdentifier(handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString());
            if (!versionHistory.isNodeType(NT_VERSION_HISTORY)) {
                log.warn("'{}/@{}' does not point to a node of type '{}' which is not allowed. Correct the handle manually.",
                        handle.getPath(), HIPPO_VERSION_HISTORY_PROPERTY, NT_VERSION_HISTORY);
                return node;
            }

            final boolean preview = requestContext.getResolvedMount().getMount().isPreview();
            Optional<Node> version = getVersionForLabel(versionHistory, branchId, preview);
            if (!version.isPresent() || !version.get().hasNode(JCR_FROZEN_NODE)) {
                // lookup master revision in absence of a branch version
                version = getVersionForLabel(versionHistory, BranchConstants.MASTER_BRANCH_ID, preview);
            }
            if (!version.isPresent() || !version.get().hasNode(JCR_FROZEN_NODE)) {
                // return current (published or unpublished) in absence of a branch and master version
                return node;
            }

            log.info("Found version '{}' to use for rendering.", version.get().getPath());

            final Node frozenNode = version.get().getNode(JCR_FROZEN_NODE);
            // we can only decorate a frozen node to the canonical location
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(frozenNode, canonicalNode.getPath(), canonicalNode.getName());

        } catch (ItemNotFoundException e) {
            log.warn("Version history node with id stored on '{}/@{}' does not exist. Correct the handle manually.",
                    handle.getPath(), HIPPO_VERSION_HISTORY_PROPERTY);
            return node;
        } catch (RepositoryException e) {
            log.warn("Failed to get frozen node from version history for handle '{}', returning '{}' instead.",
                    handle.getPath(), node.getPath(), e);
            return node;
        }

    }

    private Optional<Node> getVersionForLabel(final Node versionHistory, final String branchId, final boolean preview) throws RepositoryException {
        final String versionLabel;
        if (preview) {
            versionLabel = branchId + "-" + HippoStdNodeType.UNPUBLISHED;
        } else {
            versionLabel = branchId + "-" + HippoStdNodeType.PUBLISHED;
        }
        if (versionHistory.hasProperty(JCR_VERSIONLABELS + "/" + versionLabel)) {
            return Optional.of(versionHistory.getProperty(JCR_VERSIONLABELS + "/" + versionLabel).getNode());
        }
        return Optional.empty();
    }

    public String getPrimaryObjectType(Node node) throws ObjectBeanManagerException {
        String jcrPrimaryNodeType;
        String path;
        try {

            if (node.isNodeType(NT_HANDLE)) {
                if (node.hasNode(node.getName())) {
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

                    if (!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(fallBackJcrPrimaryNodeType);
                    if (isObjectType) {
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
        log.info("No Descriptor found for node '{}'. Cannot return a matching node type for '{}'.", path, jcrPrimaryNodeType);
        return null;
    }

    private void checkUUID(String uuid) throws ObjectBeanManagerException {
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new ObjectBeanManagerException("Uuid is not parseable to a valid uuid: '" + uuid + "'");
        }
    }

    public Class<? extends HippoBean> getAnnotatedClassFor(String jcrPrimaryNodeType) {
        return this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
    }

    public String getPrimaryNodeTypeNameFor(Class<? extends HippoBean> hippoBean) {
        return jcrBeanPrimaryNodeTypePairs.get(hippoBean);
    }
}
