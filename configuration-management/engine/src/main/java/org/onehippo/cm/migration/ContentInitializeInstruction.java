/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.ConfigSourceImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;

import static org.onehippo.cm.api.model.PropertyType.SINGLE;

public class ContentInitializeInstruction extends InitializeInstruction {

    protected ContentInitializeInstruction(final EsvNode instructionNode, final Type type,
                                           final InitializeInstruction combinedWith) throws EsvParseException {
        super(instructionNode, type, combinedWith);
    }

    private String getNodePath() {
        String path;
        if (Type.CONTENTDELETE == getType()) {
            path = getContentPath();
        } else {
            path = StringUtils.substringBeforeLast(getContentPath(), "/");
        }
        return path.equals("") ? "/" : path;
    }

    public void processContentInstruction(final SourceImpl source, final Map<String, DefinitionNodeImpl> nodeDefinitions,
                                          Set<DefinitionNode> deltaNodes) throws EsvParseException {
        final String nodePath = getNodePath();
        final String name = StringUtils.substringAfterLast(getContentPath(), "/");

        DefinitionNodeImpl node = nodeDefinitions.get(nodePath);

        log.info("processing " + getType().getPropertyName() + " " + getContentPath());

        switch (getType()) {
            case CONTENTDELETE:
                if (node != null) {
                    if (node.isDeleted()) {
                        log.warn("Ignoring hippo:contentdelete " + getName() + " at " + getInstructionNode().getSourceLocation() +
                                ": path " + nodePath + " already deleted at " + node.getSourceLocation());
                    } else {
                        final boolean removable = isRemovableNode(node, deltaNodes);
                        log.info((removable ? "Removing" : "Deleting") + " path " + nodePath + " defined at " + node.getSourceLocation() +
                                " by hippo:contentdelete " + getName() + " at " + getInstructionNode().getSourceLocation());
                        if (removable) {
                            nodeDefinitions.remove(nodePath);
                        }
                        deleteChildren(nodePath + "/", nodeDefinitions);
                        if (deltaNodes.contains(node)) {
                            deltaNodes.remove(node);
                        }
                        if (node.isRoot()) {
                            deleteDefinition(node.getDefinition());
                            if (!removable) {
                                // force add new (root) delete on nodePath, will *replace* nodeDefinition on nodePath
                                node = null;
                            }
                        } else {
                            if (removable) {
                                deleteDefinitionNode(node);
                                DefinitionNodeImpl parentNode = (DefinitionNodeImpl) node.getParent();
                                while (parentNode.isEmpty()) {
                                    // empty (delta) parent: remove as well
                                    nodeDefinitions.remove(parentNode.getPath());
                                    deltaNodes.remove(parentNode);
                                    if (parentNode.isRoot()) {
                                        // can remove whole of delta definition
                                        deleteDefinition(parentNode.getDefinition());
                                        break;
                                    } else {
                                        // first time redundant
                                        deleteDefinitionNode(node);
                                        node = parentNode;
                                    }
                                }
                            } else {
                                node.delete();
                                node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
                            }
                        }
                    }
                } else {
                    log.info("Adding hippo:contentdelete for " + nodePath + " defined at " + getInstructionNode().getSourceLocation());
                }
                if (node == null) {
                    ConfigDefinitionImpl def = ((ConfigSourceImpl)source).addConfigDefinition();
                    node = new DefinitionNodeImpl(nodePath, name, def);
                    def.setNode(node);
                    node.setDelete(true);
                    node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
                    nodeDefinitions.put(nodePath, node);
                }
                break;
            case CONTENTPROPDELETE:
                if (node != null) {
                    if (node.isDeleted()) {
                        log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                " at " + getInstructionNode().getSourceLocation() +
                                ":  parent node already deleted at " + node.getSourceLocation());
                        // skip adding delete property
                        node = null;
                    } else {
                        DefinitionPropertyImpl prop = node.getModifiableProperties().get(name);
                        if (prop != null) {
                            if (PropertyOperation.DELETE == prop.getOperation()) {
                                log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                        " at " + getInstructionNode().getSourceLocation() +
                                        ": property already deleted at " + prop.getSourceLocation());
                                // skip adding delete property
                                node = null;
                            } else {
                                final boolean removable = isRemovableProp(prop, deltaNodes);
                                log.info((removable ? "Removing" : "Deleting") + " property " + getContentPath()
                                        + " defined at " + prop.getSourceLocation() + " by hippo:contentpropdelete " +
                                        getName() + " at " + getInstructionNode().getSourceLocation());
                                if (removable) {
                                    node.getModifiableProperties().remove(name);
                                    // no need to add delete property
                                    node = null;
                                }
                            }
                        } else {
                            log.info("Merging hippo:contentpropdelete for " + nodePath + " defined at " + node.getSourceLocation());
                            // add delete property at the end
                        }
                    }
                } else {
                    node = findNearestParent(nodePath, nodeDefinitions, deltaNodes);
                    if (node != null) {
                        if (node.isDeleted()) {
                            log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                    " at " + getInstructionNode().getSourceLocation() +
                                    ":  nearest parent node already deleted at " + node.getSourceLocation());
                            // skip adding delete property
                            node = null;
                        } else {
                            if (deltaNodes.contains(node)) {
                                node = addIntermediateParents(node, nodePath, nodeDefinitions, deltaNodes);
                            } else {
                                log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                        " at " + getInstructionNode().getSourceLocation() +
                                        ": no direct or intermediate delta node parent defined.");
                                // skip adding delete property
                                node = null;
                            }
                        }
                    } else {
                        node = addDeltaRootNode(source, nodePath, name, nodeDefinitions, deltaNodes);
                    }
                    if (node != null) {
                        log.info("Adding hippo:contentpropdelete for " + nodePath + " defined at " + getInstructionNode().getSourceLocation());
                    }
                }
                if (node != null) {
                    DefinitionPropertyImpl prop = node.addProperty(name, ValueType.STRING, new ValueImpl[0]);
                    prop.setOperation(PropertyOperation.DELETE);
                    prop.getSourceLocation().copy(getInstructionNode().getSourceLocation());
                }
                break;
            case CONTENTPROPSET:
            case CONTENTPROPADD:
                EsvProperty property = getTypeProperty();
                if (!property.isMultiple() && EsvProperty.isKnownMultipleProperty(name)) {
                    property.setMultiple(true);
                }
                DefinitionPropertyImpl prop = null;
                if (node != null) {
                    if (node.isDeleted()) {
                        log.warn("Ignoring " + getType().getPropertyName() + " " + getName() + " for property " + getContentPath() +
                                " at " + getInstructionNode().getSourceLocation() +
                                ":  parent node already deleted at " + node.getSourceLocation());
                        // skip adding delete property
                        node = null;
                    } else {
                        prop = node.getModifiableProperties().get(name);
                        if (prop != null) {
                            if (PropertyOperation.DELETE == prop.getOperation()) {
                                prop = null;
                            } else if (getType() == Type.CONTENTPROPADD || PropertyOperation.OVERRIDE != prop.getOperation()) {
                                // check same type and valueType
                                if (prop.getValueType().ordinal() != property.getType()) {
                                    throw new EsvParseException("Unsupported " + getType().getPropertyName() +
                                            " " + prop.getPath() + " type change to " + ValueType.values()[property.getType()].name() +
                                            " at " + property.getSourceLocation() + " (from " + prop.getValueType().toString() +
                                            " at " + prop.getSourceLocation() + ")");
                                }
                                if ((prop.getType() == SINGLE && property.isMultiple()) || (prop.getType() != SINGLE && !property.isMultiple())) {
                                    throw new EsvParseException("Unsupported " + getType().getPropertyName() + " " + prop.getPath() +
                                            " multiplicity change to " + property.isMultiple() + " at " + property.getSourceLocation() +
                                            " (from " + !property.isMultiple() + " at " + prop.getSourceLocation() + ")");
                                }
                            }
                        }
                        log.info("Merging " + getType().getPropertyName() + " for " + nodePath + " defined at " + node.getSourceLocation());
                    }
                } else {
                    node = findNearestParent(nodePath, nodeDefinitions, deltaNodes);
                    if (node != null) {
                        if (node.isDeleted()) {
                            log.warn("Ignoring " + getType().getPropertyName() + " " + getName() + " for property " + getContentPath() +
                                    " at " + getInstructionNode().getSourceLocation() +
                                    ":  nearest parent node already deleted at " + node.getSourceLocation());
                            // skip adding property
                            node = null;
                        } else {
                            node = addIntermediateParents(node, nodePath, nodeDefinitions, deltaNodes);
                        }
                    } else {
                        node = addDeltaRootNode(source, nodePath, name, nodeDefinitions, deltaNodes);
                    }
                    if (node != null) {
                        log.info("Adding " + getType().getPropertyName() + " for " + nodePath + " defined at " + getInstructionNode().getSourceLocation());
                    }
                }
                if (node != null) {
                    PropertyOperation op = PropertyOperation.REPLACE;
                    if (prop != null && PropertyOperation.OVERRIDE == prop.getOperation()) {
                        op = prop.getOperation();
                    } else if (getType() == Type.CONTENTPROPADD) {
                        op = PropertyOperation.ADD;
                    }
                    addProperty(node, property, name, prop, op, false);
                }
                break;
        }
    }

    protected DefinitionPropertyImpl addProperty(final DefinitionNodeImpl node, final EsvProperty property,
                                                 final String propertyName, final DefinitionPropertyImpl current,
                                                 final PropertyOperation op, final boolean isPathReference)
            throws EsvParseException {
        DefinitionPropertyImpl prop;
        ValueType valueType = isPathReference ? ValueType.REFERENCE : ValueType.values()[property.getType()];
        ValueImpl[] newValues = property.isMultiple() ? new ValueImpl[property.getValues().size()] : new ValueImpl[1];
        for (int i = 0; i < newValues.length; i++) {
            EsvValue value = property.getValues().get(i);
            Object valueObject = value.getValue();
            if (isPathReference) {
                final String referencePath = (String)valueObject;
                if (!referencePath.startsWith("/")) {
                    // While esv path references may be relative, for esv2yaml migration we cannot easily support this
                    // as the esv export *may* generate relative references *outside* the scope of the esv (and thus yaml)
                    // content/config definition itself.
                    // The latter is *not* supported for relative yaml path references, which always must be relative to
                    // the definition root path ("" being used/needed for a relative reference to the root node itself).
                    valueObject = StringUtils.substringBeforeLast(getContentPath(), "/") + "/" + referencePath;
                }
            }
            newValues[i] = new ValueImpl(valueObject, valueType, value.isResourcePath(), isPathReference);
        }
        if (current != null && PropertyOperation.ADD == op) {
            ValueImpl[] oldValues = cloneValues(current.getValues());
            ValueImpl[] values = new ValueImpl[oldValues.length + newValues.length];
            System.arraycopy(oldValues, 0, values, 0, oldValues.length);
            System.arraycopy(newValues, 0, values, oldValues.length, newValues.length);
            newValues = values;
        }
        if (property.isMultiple() || newValues.length > 1) {
            prop = node.addProperty(propertyName, valueType, newValues);
        } else {
            prop = node.addProperty(propertyName, newValues[0]);
        }
        if (current != null) {
            prop.setOperation(current.getOperation());
        }
        if (PropertyOperation.ADD != op || current == null) {
            prop.setOperation(op);
        }
        prop.getSourceLocation().copy(property.getSourceLocation());
        return prop;
    }

    private ValueImpl[] cloneValues(final Value[] values) {
        final ValueImpl[] result = new ValueImpl[values.length];
        for (int i = 0; i < values.length; i++) {
            final Value value = values[i];
            result[i] = new ValueImpl(value.getObject(), value.getType(), value.isResource(), value.isPath());
        }
        return result;
    }

    private DefinitionNodeImpl addIntermediateParents(final DefinitionNodeImpl parentNode, final String nodePath,
                                                      final Map<String, DefinitionNodeImpl> nodeDefinitions,
                                                      final Set<DefinitionNode> deltaNodes) {
        DefinitionNodeImpl node = parentNode;
        String[] parentsToAdd = nodePath.substring(parentNode.getPath().length() + 1).split("/");
        for (String parent : parentsToAdd) {
            node = node.addNode(parent);
            node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
            nodeDefinitions.put(node.getPath(), node);
            deltaNodes.add(node);
        }
        return node;
    }

    protected DefinitionNodeImpl addDeltaRootNode(final SourceImpl source, final String nodePath, final String name,
                                                final Map<String, DefinitionNodeImpl> nodeDefinitions,
                                                final Set<DefinitionNode> deltaNodes) {
        ConfigDefinitionImpl def = ((ConfigSourceImpl)source).addConfigDefinition();
        DefinitionNodeImpl node = new DefinitionNodeImpl(nodePath, name, def);
        def.setNode(node);
        deltaNodes.add(node);
        node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
        nodeDefinitions.put(nodePath, node);
        return node;
    }

    private DefinitionNodeImpl findNearestParent(final String nodePath, final Map<String, DefinitionNodeImpl> nodeDefinitions,
                                                      final Set<DefinitionNode> deltaNodes) {
        String path = StringUtils.substringBeforeLast(nodePath, "/");
        if (path.equals("")) {
            path = "/";
        }
        DefinitionNodeImpl parent = nodeDefinitions.get(path);
        if (parent != null) {
            return parent;
        } else if (!path.equals("/")) {
            return findNearestParent(path, nodeDefinitions, deltaNodes);
        }
        return null;
    }

    private void deleteChildren(final String childPathPrefix, final Map<String, DefinitionNodeImpl> nodeDefinitions) {
        TreeSet<String> deletePaths = nodeDefinitions.keySet().stream()
                .filter(p -> p.startsWith(childPathPrefix))
                .collect(Collectors.toCollection(TreeSet::new));
        for (Iterator<String> delIter = deletePaths.descendingIterator(); delIter.hasNext(); ) {
            String delPath = delIter.next();
            DefinitionNodeImpl delNode = nodeDefinitions.get(delPath);
            if (delNode.isRoot()) {
                // remove definition from source
                deleteDefinition(delNode.getDefinition());
            } else {
                deleteDefinitionNode(delNode);
            }
            nodeDefinitions.remove(delPath);
        }
    }

    protected void deleteDefinition(final Definition definition) {
        // remove definition from source
        SourceImpl defSource = (SourceImpl) definition.getSource();
        for (Iterator<Definition> defIterator = defSource.getModifiableDefinitions().iterator(); defIterator.hasNext(); ) {
            if (defIterator.next() == definition) {
                defIterator.remove();
                break;
            }
        }
    }

    private void deleteDefinitionNode(final DefinitionNodeImpl node) {
        // remove definitionNode from parent
        ((DefinitionNodeImpl) node.getParent()).getModifiableNodes().remove(node.getName());
    }

    protected boolean isRemovableProp(final DefinitionProperty property, final Set<DefinitionNode> deltaNodes) {
        return !deltaNodes.contains(property.getParent());
    }

    protected boolean isRemovableNode(final DefinitionNode node, final Set<DefinitionNode> deltaNodes) {
        if (node.isRoot()) {
            return !deltaNodes.contains(node);
        } else {
            return !deltaNodes.contains(node) || !deltaNodes.contains(node.getParent());
        }
    }
}
