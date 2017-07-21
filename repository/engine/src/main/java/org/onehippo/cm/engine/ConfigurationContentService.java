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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.util.ConfigurationModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

/**
 * Applies content definitions to the repository
 */
public class ConfigurationContentService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationContentService.class);
    public static final String SEPARATOR = "/";

    private final JcrContentProcessor contentProcessingService;
    private final ConfigurationBaselineService configurationBaselineService;

    ConfigurationContentService(final ConfigurationBaselineService configurationBaselineService, final JcrContentProcessor contentProcessingService) {
        this.configurationBaselineService = configurationBaselineService;
        this.contentProcessingService = contentProcessingService;
    }

    /**
     * Apply content definitions from modules contained within configuration model
     *
     * @param model {@link ConfigurationModel}
     * @param session active {@link Session}
     */
    public void apply(final ConfigurationModelImpl model, final Session session) throws RepositoryException {
        final boolean isUpgradeTo12 = checkUpgradeTo12  (session);
        boolean allModulesHaveSucceeded = true;

        for (ModuleImpl module : model.getModules()) {
            if (isNotEmpty(module.getActionsMap()) || isNotEmpty(module.getContentDefinitions())) {
                final boolean success = apply(module, model, session, isUpgradeTo12);
                if (!success) {
                    allModulesHaveSucceeded = false;
                }
            }
        }

        if (isUpgradeTo12 && allModulesHaveSucceeded) {
            completeUpgradeTo12(session);
        }
    }

    /**
     * Import ContentDefinition
     *
     * @param node {@link DefinitionNodeImpl} to import
     * @param parentNode parent node
     * @param actionType action type
     */
    public void importNode(DefinitionNodeImpl node, Node parentNode, ActionType actionType) throws RepositoryException, IOException {
        contentProcessingService.importNode(node, parentNode, actionType);
    }

    /**
     * Export node to module
     *
     * @param node node to export
     * @return Module containing single content definition
     */
    public ModuleImpl exportNode(Node node) throws RepositoryException {
        final ExportContentProcessor exportContentProcessor = new ExportContentProcessor();
        return exportContentProcessor.exportNode(node);
    }

    /**
     * Apply content definitions from module
     *
     * @param module target {@link Module}
     * @param session active {@link Session}
     */
    private boolean apply(final ModuleImpl module, final ConfigurationModel model,
                          final Session session, final boolean isUpgradeTo12) throws RepositoryException {
        final double moduleSequenceNumber = module.getSequenceNumber() != null ? module.getSequenceNumber() : Double.MIN_VALUE;
        final List<ActionItem> actionsToProcess = collectNewActions(moduleSequenceNumber, module.getActionsMap());
        processItemsToDelete(actionsToProcess, model, session, isUpgradeTo12);
        session.save();

        final Collection<String> failedPaths = new ArrayList<>();
        List<ContentDefinitionImpl> sortedDefinitions = getSortedDefinitions(module.getContentDefinitions());
        for (final ContentDefinitionImpl contentDefinition : sortedDefinitions) {
            final DefinitionNode contentNode = contentDefinition.getNode();
            final String baseNodePath = contentNode.getPath();
            final Optional<ActionType> optionalAction = findLastActionToApply(baseNodePath, actionsToProcess);
            final boolean nodeAlreadyProcessed = configurationBaselineService.getAppliedContentPaths(session).contains(baseNodePath);

            if (failedPaths.stream().anyMatch(baseNodePath::startsWith)) {
                log.info(String.format("Skipping the (re-)loading of content based at '%s', " +
                        "because the processing of an ancestor node has failed.", baseNodePath));
                continue;
            }

            if (optionalAction.isPresent() || !nodeAlreadyProcessed) {
                final ActionType action = optionalAction.orElse(ActionType.APPEND);

                try {
                    if (ConfigurationModelUtils.getCategoryForNode(baseNodePath, model) == ConfigurationItemCategory.CONTENT) {
                        log.debug("Processing {} action for node: {}", action, baseNodePath);
                        contentProcessingService.apply(contentNode, action, session, isUpgradeTo12);

                        if (!nodeAlreadyProcessed) {
                            // will save all the session changes!
                            configurationBaselineService.addAppliedContentPath(baseNodePath, session);
                        } else {
                            session.save();
                        }
                    } else {
                        log.error(String.format("Base node '%s' in '%s' is not categorized as content, skipping action '%s'.",
                                baseNodePath, contentDefinition.getOrigin(), action));
                    }
                } catch (RepositoryException ex) {
                    log.error(String.format("Processing '%s' action for node failed: '%s'", action, baseNodePath), ex);
                    failedPaths.add(baseNodePath);
                }
            }
        }

        configurationBaselineService.updateModuleSequenceNumber(module, session);
        return failedPaths.isEmpty();
    }

    /**
     * Collect new items that were not processed yet
     *
     * @param currentVersion Latest sequence number that was processed
     * @param actionsMap     Action items per version map
     * @return New action items
     */
    private List<ActionItem> collectNewActions(final double currentVersion, final Map<Double, Set<ActionItem>> actionsMap) {
        return actionsMap.entrySet().stream().filter(e -> e.getKey() > currentVersion)
                .flatMap(e -> e.getValue().stream()).collect(toList());
    }

    /**
     * Sort content definitions in natural order of their root node paths and order before factor
     */
    List<ContentDefinitionImpl> getSortedDefinitions(final List<ContentDefinitionImpl> contentDefinitions) {

        final Map<String, List<ContentDefinitionImpl>> itemsPerPath = contentDefinitions.stream().collect(Collectors.groupingBy(getParentPath(), TreeMap::new, mapping(a -> a, toList())));
        for (String path : itemsPerPath.keySet()) {

            final List<ContentDefinitionImpl> siblings = itemsPerPath.get(path);
            siblings.sort(comparing(z -> z.getNode().getName(), naturalOrder()));

            //sort nodes to reorder so that nodes which dont have any dependencies on nodes which also has order before tag get ordered first
            final Stream<ContentDefinitionImpl> orderSortedStream = siblings.stream()
                    .filter(definition -> StringUtils.isNotEmpty(definition.getNode().getOrderBefore()))
                    .sorted(Comparator.comparing(x -> this.getNodeIndex(x.getNode().getOrderBefore(), siblings)));

            orderSortedStream.forEach(nodeToReorder -> {
                final String orderBeforeNodeName = nodeToReorder.getNode().getOrderBefore();
                int nodeIndex = getNodeIndex(orderBeforeNodeName, siblings);
                if (nodeIndex != -1) {
                    siblings.remove(nodeToReorder);
                    siblings.add(nodeIndex, nodeToReorder);
                }
            });
        }

        return itemsPerPath.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private int getNodeIndex(final String orderBeforeNodeName, final List<ContentDefinitionImpl> contentDefinitions) {
        for (ContentDefinitionImpl contentDefinition : contentDefinitions) {
            if (contentDefinition.getNode().getName().equals(orderBeforeNodeName)) {
                return contentDefinitions.indexOf(contentDefinition);
            }
        }
        return -1;
    }

    private Function<ContentDefinitionImpl, String> getParentPath() {
        return p -> Paths.get(p.getNode().getPath()).getParent().toString();
    }

    /**
     * Delete nodes from action list
     *
     * @param items items to delete
     * @param session active {@link Session}
     */
    private void processItemsToDelete(final List<ActionItem> items, final ConfigurationModel model,
                                      final Session session, final boolean isUpgradeTo12) throws RepositoryException {
        for (final ActionItem item : items) {
            if (item.getType() == ActionType.DELETE) {
                final String baseNodePath = item.getPath();
                if (ConfigurationModelUtils.getCategoryForNode(baseNodePath, model) == ConfigurationItemCategory.CONTENT) {
                    log.debug("Processing delete action for node: {}", baseNodePath);

                    final DefinitionNode deleteNode = new DefinitionNodeImpl(baseNodePath,
                            StringUtils.substringAfterLast(baseNodePath, SEPARATOR), null);
                    contentProcessingService.apply(deleteNode, ActionType.DELETE, session, isUpgradeTo12);
                } else {
                    log.warn(String.format("Base node '%s' is not categorized as content, skipping delete action.",
                            baseNodePath));
                }
            }
        }
    }

    /**
     * Find last action for the path specified, DELETE action type is excluded
     *
     * @param absolutePath full path of the node
     * @param actions      available actions
     * @return If found, contains last action for specified path
     */
    private Optional<ActionType> findLastActionToApply(final String absolutePath, final List<ActionItem> actions) {
        return actions.stream()
                .filter(x -> x.getPath().equals(absolutePath) && x.getType() != ActionType.DELETE)
                .map(ActionItem::getType)
                .reduce((__, second) -> second);
    }

    /**
     * Below upgradeTo12 functionality works as follows:
     *
     * Upon entering ConfigurationContentService#apply, we check if the content node (/hcm:hcm/hcm:content) exists.
     * If it doesn't, we create it, and we also create a "content upgrade to 12 marker" node at the root of the
     * repository. The content node doesn't exist when bootstrapping the first time against a CMS 11 repository, but
     * also when bootstrapping a fresh repository. In the latter case, there will be no content in the repository
     * either, and therefore, the upgrade flag's effect will be nil.
     *
     * While that marker node exists, we accept/ignore the situation that a content definition is attempted to be
     * applied (i.e. it has not yet been marked as applied in the baseline), while the content definition's base node
     * already exists in the repository. We do, however, in such a case, add the content definition base path to the
     * baseline's set of applied content paths, such that subsequently, this content definition is ignored (unless it
     * is associated with a content action).
     *
     * The first time the content of all modules is applied successfully, the marker node is removed, and therefore,
     * the system returns to its normal content bootstrapping behaviour.
     *
     * Above-described logic pertains to the upgrade from CMS 11 to CMS 12. In CMS 13, this logic should be cleaned up.
     */
    private static final String UPGRADE_TO_12_MARKER_NODE_NAME = "content-upgrade-to-12-marker";

    private boolean checkUpgradeTo12(final Session session) throws RepositoryException {
        if (!configurationBaselineService.contentNodeExists(session)) {
            configurationBaselineService.createContentNode(session);
            session.getRootNode()
                    .addNode(UPGRADE_TO_12_MARKER_NODE_NAME, NodeType.NT_UNSTRUCTURED)
                    .setProperty("description", "This node is used by the bootstrapping mechanism " +
                            "to track the upgrade of repository content to CMS 12. Please don't remove " +
                            "it manually; it will be removed automatically.");
            session.save();
        }

        return session.nodeExists("/" + UPGRADE_TO_12_MARKER_NODE_NAME);
    }

    private void completeUpgradeTo12(final Session session) throws RepositoryException {
        session.removeItem("/" + UPGRADE_TO_12_MARKER_NODE_NAME);
        session.save();
    }
}
