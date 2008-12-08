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
package org.hippoecm.repository.updater;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;

final class UpdaterNode extends UpdaterItem implements Node {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    boolean hollow;
    Map<String, List<UpdaterItem>> children;
    Map<UpdaterItem, String> reverse;
    Set<UpdaterItem> removed;

    UpdaterNode(UpdaterSession session, UpdaterNode target) {
        super(session, target);
        hollow = false;
    }

    UpdaterNode(UpdaterSession session, Node origin, UpdaterNode target) {
        super(session, origin, target);
        hollow = true;
    }

    private final void substantiate() throws RepositoryException {
        if (!hollow)
            return;
        children = new LinkedHashMap<String, List<UpdaterItem>>();
        reverse = new HashMap<UpdaterItem, String>();
        removed = new HashSet<UpdaterItem>();
        for (NodeIterator iter = ((Node) origin).getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            String name = child.getName();
            if (!children.containsKey(name))
                children.put(name, new LinkedList<UpdaterItem>());
            List<UpdaterItem> siblings = children.get(name);
            siblings.add(new UpdaterNode(session, child, this));
        }
        for (PropertyIterator iter = ((Node) origin).getProperties(); iter.hasNext();) {
            Property child = iter.nextProperty();
            String name = ":" + child.getName();
            if (!children.containsKey(name))
                children.put(name, new LinkedList<UpdaterItem>());
            List<UpdaterItem> siblings = children.get(name);
            siblings.add(new UpdaterProperty(session, child, this));
        }
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            String name = items.getKey();
            for (UpdaterItem item : items.getValue()) {
                reverse.put(item, name);
            }
        }
        hollow = false;
    }

    private UpdaterProperty setProperty(String name, UpdaterProperty property) throws RepositoryException {
        name = ":" + name;
        List<UpdaterItem> siblings = new LinkedList<UpdaterItem>();
        siblings.add(property);
        children.put(name, siblings);
        reverse.put(property, name);
        return property;
    }

    public void setPrimaryNodeType(String name) throws RepositoryException {
        setProperty("jcr:primaryType", name);
    }

    private String[] getInternalProperty(String name) throws ValueFormatException, RepositoryException {
        if (hollow) {
            if (origin != null && ((Node) origin).hasProperty(name)) {
                String[] strings;
                Property property = ((Node) origin).getProperty(name);
                if (property.getDefinition().isMultiple()) {
                    Value[] values = property.getValues();
                    strings = new String[values.length];
                    for (int i = 0; i < strings.length; i++)
                        strings[i] = values[i].getString();
                } else
                    strings = new String[]{property.getString()};
                return strings;
            } else
                return new String[0];
        } else {
            List<UpdaterItem> items = children.get(":" + name);
            if (items != null && items.size() > 0) {
                String[] strings;
                UpdaterProperty property = (UpdaterProperty) items.get(0);
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    strings = new String[values.length];
                    for (int i = 0; i < strings.length; i++)
                        strings[i] = values[i].getString();
                } else
                    strings = new String[]{property.getString()};
                return strings;
            } else
                return new String[0];
        }
    }

    void commit() throws RepositoryException {
        Item oldOrigin = null;
        boolean nodeTypesChanged;
        boolean nodeLocationChanged;
        boolean nodeRelinked = false;

        if (origin != null) {
            if(origin instanceof HippoNode) {
                if(origin.isSame(((HippoNode)origin).getCanonicalNode())) {
                    return;
                }
            }
        }

        if (getInternalProperty("jcr:primaryType").length > 0)
            nodeTypesChanged = !((Node) origin).getPrimaryNodeType().getName().equals(getInternalProperty("jcr:primaryType")[0]);
        else
            nodeTypesChanged = true;

        if (parent == null)
            nodeLocationChanged = false;
        else
            nodeLocationChanged = (!parent.origin.isSame(origin.getParent()) || !origin.getName().equals(getName()));

        if(UpdaterEngine.log.isDebugEnabled()) {
            UpdaterEngine.log.debug("commit node "+getPath()+" origin "+(origin!=null?origin.getPath():"null")+(nodeTypesChanged?" type changed":"")+(nodeLocationChanged?" location changed":""));
        }

        if (!hollow && origin != null && origin.isNode()) {
            if(((Node)origin).isNodeType("jcr:versionable")) {
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit checkout "+origin.getPath());
                }
                ((Node)origin).checkout();
            }
        }
        if (nodeTypesChanged) {
            if (!hollow) {
                oldOrigin = origin;
                if ((((Node)parent.origin).hasNode(getName())) &&
                        ((Node)parent.origin).getNode(getName()).getDefinition().isAutoCreated() &&
                        !((Node)parent.origin).getNode(getName()).getDefinition().allowsSameNameSiblings()) {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit autocreated "+origin.getPath());
                    }
                    origin = ((Node)parent.origin).getNode(getName());
                } else {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit create "+getPath()+" in "+((Node)parent.origin).getPath()+" type "+getInternalProperty("jcr:primaryType")[0]);
                    }
                    origin = ((Node)parent.origin).addNode(getName(), getInternalProperty("jcr:primaryType")[0]);
                    nodeRelinked = true;
                }
            }
        } else {
            if (nodeLocationChanged) {
                // first move item
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit move "+origin.getPath()+" to "+parent.origin.getPath() + "/" + getName());
                }
                origin.getSession().move(origin.getPath(), parent.origin.getPath() + "/" + getName());
            }
        }
        if (!hollow) {
            Set<String> curMixins = new TreeSet();
            Set<String> newMixins = new TreeSet();
            String[] mixins = getInternalProperty("jcr:mixinTypes");
            if (mixins != null) {
                for(String mixin : mixins) {
                    newMixins.add(mixin);
                }
            }
            if(((Node)origin).hasProperty("jcr:mixinTypes")) {
                for(Value mixin : ((Node)origin).getProperty("jcr:mixinTypes").getValues()) {
                    curMixins.add(mixin.getString());
                }
            }
            for(String mixin : curMixins) {
                if(!newMixins.contains(mixin)) {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit removeMixin "+origin.getPath()+" mixin "+mixin);
                    }
                    ((Node)origin).removeMixin(mixin);
                }
            }
            for(String mixin : newMixins) {
                if(!curMixins.contains(mixin)) {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit addMixin "+origin.getPath()+" mixin "+mixin);
                    }
                    ((Node)origin).addMixin(mixin);
                }
            }
            if (nodeRelinked) {
                session.relink((Node)oldOrigin, (Node)origin);
            }

            List<NodeType> nodetypes = new LinkedList<NodeType>();
            NodeTypeManager ntMgr = origin.getSession().getWorkspace().getNodeTypeManager();
            nodetypes.add(ntMgr.getNodeType(getInternalProperty("jcr:primaryType")[0]));
            for(String mixin : newMixins) {
                nodetypes.add(ntMgr.getNodeType(mixin));
            }
            for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
                String name = items.getKey();
                if (name.startsWith(":")) {
                    name = name.substring(1);
                    Node node = (Node) origin;

                    //if(node.hasProperty(name) && node.getProperty(name).getDefinition().isProtected())
                    //continue;

                    boolean isValid = false;
                    for(NodeType nodeType : nodetypes) {
                        PropertyDefinition[] defs = nodeType.getPropertyDefinitions();
                        boolean breakLoop = false;
                        for (int j = 0; j < defs.length; j++) {
                            if (defs[j].getName().equals("*")) {
                                isValid = true;
                            } else if (defs[j].getName().equals(name)) {
                                if (defs[j].isProtected()) {
                                    isValid = false;
                                } else {
                                    isValid = true;
                                }
                                // break out of the outermost loop
                                breakLoop = true;
                                break;
                            }
                        }
                        if(breakLoop)
                            break;
                    }
                    if (!isValid) {
                        continue;
                    }

                    if (items.getValue().size() > 0) {
                        UpdaterProperty property = (UpdaterProperty) items.getValue().get(0);
                        property.commit();
                        if (property.isMultiple()) {
                            if(UpdaterEngine.log.isDebugEnabled()) {
                                UpdaterEngine.log.debug("commit set multivalue property "+name+ " on "+getPath());
                            }
                            ((Node) origin).setProperty(name, property.getValues());
                        } else {
                            if(UpdaterEngine.log.isDebugEnabled()) {
                                UpdaterEngine.log.debug("commit set singlevalue property "+name+ " on "+getPath());
                            }
                            ((Node) origin).setProperty(name, property.getValue());
                        }
                    }
                } else {
                    for (UpdaterItem item : items.getValue()) {
                        ((UpdaterNode) item).commit();
                    }
                }
            }
            for (UpdaterItem item : removed) {
                if (item.origin != null) {
                    ItemDefinition definition = ( item.origin.isNode() ? ((Node)item.origin).getDefinition() : ((Property)item.origin).getDefinition() );
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit remove old child "+item.origin.getPath());
                    }
                    if(!definition.isProtected()) {
                        item.origin.remove();
                    }
                }
            }
            if (oldOrigin != null) {
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit remove old origin "+oldOrigin.getPath());
                }
                oldOrigin.remove();
            }
        }
    }

    private UpdaterNode resolveNode(String relPath) throws PathNotFoundException, RepositoryException {
        if (!relPath.contains("/"))
            return this;
        Node node = this, last = this;
        for (StringTokenizer iter = new StringTokenizer(relPath, "/"); iter.hasMoreTokens();) {
            last = node;
            node = node.getNode(iter.nextToken());
        }
        return (UpdaterNode) last;
    }

    private String resolveName(String relPath) {
        if (relPath.contains("/"))
            relPath = relPath.substring(0, relPath.lastIndexOf("/"));
        if (relPath.contains("[") && relPath.endsWith("]"))
            relPath = relPath.substring(0, relPath.indexOf("["));
        return relPath;
    }

    private int resolveIndex(String name) {
        if (name.contains("["))
            return Integer.parseInt(name.substring(name.indexOf("[") + 1, name.lastIndexOf("]")));
        else
            return 0;
    }

    private UpdaterItem getItem(String relPath, boolean isProperty) throws PathNotFoundException {
        String name = relPath;
        String sequel = null;
        int index = 0;
        if (name.contains("/")) {
            sequel = name.substring(name.indexOf("/") + 1);
            name = name.substring(0, name.indexOf("/"));
        }
        if (name.contains("[") && name.endsWith("]")) {
            index = Integer.parseInt(name.substring(name.indexOf("[") + 1, name.length() - 1));
            name = name.substring(0, name.indexOf("["));
        }
        if (sequel != null) {
            List<UpdaterItem> items = children.get(name);
            if (items == null)
                throw new PathNotFoundException(name);
            if (index >= items.size())
                throw new PathNotFoundException(name + "[" + index + "]");
            UpdaterNode item = (UpdaterNode) items.get(index);
            return item.getItem(sequel, isProperty);
        } else {
            if (isProperty) {
                List<UpdaterItem> items = children.get(":" + name);
                if (items == null)
                    throw new PathNotFoundException(name);
                if (items.isEmpty())
                    throw new PathNotFoundException(name);
                return items.get(0);
            } else {
                List<UpdaterItem> items = children.get(name);
                if (items == null)
                    throw new PathNotFoundException(name);
                if (index >= items.size())
                    throw new PathNotFoundException(name + "[" + index + "]");
                return items.get(index);
            }
        }
    }

    public NodeType[] getNodeTypes() throws RepositoryException {
        Vector<NodeType> nodeTypes = new Vector<NodeType>();
        NodeType nodeType = session.getNewType(getProperty("jcr:primaryType").getString());
        if (nodeType == null) {
            // big fat error
        }
        nodeTypes.add(nodeType);
        if (hasProperty("jcr:mixinTypes")) {
            Value[] mixins = getProperty("jcr:mixinTypes").getValues();
            for (int i = 0; i < mixins.length; i++) {
                nodeType = session.getNewType(mixins[i].getString());
                nodeTypes.add(nodeType);
            }
        }
        return nodeTypes.toArray(new NodeType[nodeTypes.size()]);
    }

    // javax.jcr.Item interface

    public boolean isNode() {
        return true;
    }

    @Deprecated
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    // javax.jcr.Node interface

    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        substantiate();
        return this.addNode(relPath, null);
    }

    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        substantiate();
        Node node = this;
        String name = relPath;
        if (relPath.contains("/")) {
            HierarchyResolver manager = ((HippoWorkspace) (session.getWorkspace())).getHierarchyResolver();
            HierarchyResolver.Entry last = new HierarchyResolver.Entry();
            manager.getItem(this, relPath, false, last);
            node = last.node;
            name = last.relPath;
            if (name.contains("/")) {
                throw new PathNotFoundException(name.substring(0, name.lastIndexOf("/")));
            }
        }
        UpdaterNode child = new UpdaterNode(session, this);
        List<UpdaterItem> siblings;
        if (children.containsKey(name)) {
            siblings = children.get(name);
        } else {
            siblings = new LinkedList<UpdaterItem>();
            children.put(name, siblings);
        }
        siblings.add(child);
        reverse.put(child, name);
        return child;
    }

    @Deprecated
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        substantiate();
        if (srcChildRelPath.contains("/") || destChildRelPath.contains("/"))
            throw new ConstraintViolationException();
        throw new UpdaterException("illegal method");
    }

    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, value, value.getType());
    }

    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        substantiate();
        if (!hasProperty(name)) {
            UpdaterNode propertyParent = resolveNode(name);
            UpdaterProperty child = new UpdaterProperty(session, propertyParent);
            propertyParent.setProperty(resolveName(name), child);
            child.setValue(value);
            return child;
        } else {
            Property child = getProperty(name);
            child.setValue(value);
            return child;
        }
    }

    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        substantiate();
        if (values == null) {
            Property property = getProperty(name);
            property.remove();
            return property;
        }
        if (!hasProperty(name)) {
            UpdaterProperty child = new UpdaterProperty(session, this);
            resolveNode(name).setProperty(resolveName(name), child);
            child.setValue(values);
            return child;
        } else {
            Property child = getProperty(name);
            child.setValue(values);
            return child;
        }
    }

    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, values);
    }

    public Property setProperty(String name, String[] strings) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (strings == null) {
            Property property = getProperty(name);
            property.remove();
            return property;
        }
        Value[] values = new Value[strings.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = session.valueFactory.createValue(strings[i]);
        }
        return setProperty(name, values);
    }

    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, values);
    }

    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        substantiate();
        return (UpdaterNode) getItem(relPath, false);
    }

    public NodeIterator getNodes() throws RepositoryException {
        substantiate();
        Set<UpdaterNode> set = new LinkedHashSet<UpdaterNode>();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (!items.getKey().startsWith(":")) {
                for (UpdaterItem item : items.getValue()) {
                    set.add((UpdaterNode) item);
                }
            }
        }
        return new SetNodeIterator(set);
    }

    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        substantiate();
        Set<UpdaterNode> set = new LinkedHashSet<UpdaterNode>();
        if (children.containsKey(namePattern)) {
            for (UpdaterItem item : children.get(namePattern)) {
                set.add((UpdaterNode) item);
            }
        }
        return new SetNodeIterator(set);
    }

    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        substantiate();
        return (UpdaterProperty) getItem(relPath, true);
    }

    public PropertyIterator getProperties() throws RepositoryException {
        substantiate();
        Set<UpdaterProperty> set = new LinkedHashSet<UpdaterProperty>();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (items.getKey().startsWith(":")) {
                for (UpdaterItem item : items.getValue()) {
                    set.add((UpdaterProperty) item);
                }
            }
        }
        return new SetPropertyIterator(set);
    }

    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        substantiate();
        Set<UpdaterProperty> set = new LinkedHashSet<UpdaterProperty>();
        if (children.containsKey(":" + namePattern)) {
            for (UpdaterItem item : children.get(":" + namePattern)) {
                set.add((UpdaterProperty) item);
            }
        }
        return new SetPropertyIterator(set);
    }

    @Deprecated
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public int getIndex() throws RepositoryException {
        if (parent == null)
            return 1;
        String name = parent.reverse.get(this);
        Iterator<UpdaterItem> iter = parent.children.get(name).iterator();
        for (int index = 0; iter.hasNext(); index++) {
            if (iter.next() == this) {
                return index + 1;
            }
        }
        throw new UpdaterException("internal error");
    }

    public PropertyIterator getReferences() throws RepositoryException {
        substantiate();
        // FIXME
        return new SetPropertyIterator(new LinkedHashSet<UpdaterProperty>());
    }

    public boolean hasNode(String relPath) throws RepositoryException {
        substantiate();
        if (resolveNode(relPath).children.containsKey(resolveName(relPath))) {
            if (resolveIndex(resolveName(relPath)) < resolveNode(relPath).children.get(resolveName(relPath)).size())
                return true;
        }
        return false;
    }

    public boolean hasProperty(String relPath) throws RepositoryException {
        substantiate();
        if (resolveNode(relPath).children.containsKey(":" + resolveName(relPath))) {
            if (resolveIndex(resolveName(relPath)) < resolveNode(relPath).children.get(":" + resolveName(relPath)).size())
                return true;
        }
        return false;
    }

    public boolean hasNodes() throws RepositoryException {
        substantiate();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (!items.getKey().startsWith(":") && !items.getValue().isEmpty())
                return true;
        }
        return false;
    }

    public boolean hasProperties() throws RepositoryException {
        substantiate();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (items.getKey().startsWith(":") && !items.getValue().isEmpty())
                return true;
        }
        return false;
    }

    @Deprecated
    public NodeType getPrimaryNodeType() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        substantiate();
        // FIXME
        return false;
    }

    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        Property mixinsProperty = getProperty("jcr:mixinTypes");
        Value[] mixins = mixinsProperty.getValues();

        for (int i = 0; i < mixins.length; i++) {
            if (mixins[i].getString().equals(mixinName)) {
                Value[] newMixins = new Value[mixins.length - 1];
                System.arraycopy(mixins, 0, newMixins, 0, i);
                System.arraycopy(mixins, i + 1, newMixins, i, newMixins.length - i);
                mixinsProperty.setValue(newMixins);
                return;
            }
        }
    }

    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        Property mixinsProperty = getProperty("jcr:mixinTypes");
        Value[] mixins = mixinsProperty.getValues();
        for (int i = 0; i < mixins.length; i++) {
            if (mixins[i].getString().equals(mixinName)) {
                Value[] newMixins = new Value[mixins.length - 1];
                System.arraycopy(mixins, 0, newMixins, 0, i);
                System.arraycopy(mixins, i + 1, newMixins, i, newMixins.length - i);
                mixinsProperty.setValue(newMixins);
                return;
            }
        }
    }

    @Deprecated
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeDefinition getDefinition() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isCheckedOut() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean holdsLock() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isLocked() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }
}
