/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.dashboard.contentblocks.matcher.HasProviderMatcher;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GalleryUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationUtils;
import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.PropertyRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.TranslationRestful;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentTypes;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageProcessorRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageSetRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageVariantRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/imagegallery/")
public class ImageGalleryResource extends BaseResource {

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(ImageGalleryResource.class);

    private static final String GALLERY_PROCESSOR_SERVICE_PATH = "/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    @GET
    @Path("/")
    public ImageProcessorRestful getImageProcessor() {


        final ImageProcessorRestful processorRestful = new ImageProcessorRestful();
        // TODO verify the use and creation of the plugin context
        final PluginContext pluginContext = getPluginContext();

        final Session session = pluginContext.getSession();


        try {


            final Node processorNode = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);
            processorRestful.setPath(processorNode.getPath());
            processorRestful.setClassName(processorNode.getProperty("plugin.class").getString());
            processorRestful.setId(processorNode.getProperty("gallery.processor.id").getString());

            final Map<String, ImageVariantRestful> variantMap = fetchImageProcessorVariants(session, processorNode);
            processorRestful.addVariants(variantMap.values());


            populateImageSetsInVariants(session, variantMap.values());


        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }
        return processorRestful;
    }

    @PUT
    @Path("/save")
    public String saveImageProcessor() {
        return "TODO save image processor";
    }

    private Map<String, ImageVariantRestful> fetchImageProcessorVariants(final Session session, final Node processorNode) throws RepositoryException {
        final Map<String, ImageVariantRestful> variants = new HashMap<>();
        final Map<String, Map<String, TranslationRestful>> variantTranslationsMap = getVariantTranslationsMap(session);

        final NodeIterator variantNodes = processorNode.getNodes();
        while(variantNodes.hasNext()) {
            final Node variantNode = variantNodes.nextNode();
            final ImageVariantRestful variantRestful = new ImageVariantRestful();
            variantRestful.setId(variantNode.getIdentifier());
            final String variantName = variantNode.getName();
            variantRestful.setNamespace(HippoNodeUtils.getPrefixFromType(variantName));
            variantRestful.setName(HippoNodeUtils.getNameFromType(variantName));
            if(variantNode.hasProperty("width")) {
                variantRestful.setWidth((int)variantNode.getProperty("width").getLong());
            }
            if(variantNode.hasProperty("height")) {
                variantRestful.setHeight((int) variantNode.getProperty("height").getLong());
            }
            if(variantNode.hasProperty("upscaling")) {
                final PropertyRestful property = new PropertyRestful();
                property.setName("upscaling");
                property.setValue(variantNode.getProperty("upscaling").getString());
                property.setType(PropertyRestful.PropertyType.BOOLEAN);
                variantRestful.addProperty(property);
            }
            if(variantTranslationsMap.get(variantName) != null) {
                System.out.println("Translations for " + variantName + ": " + variantTranslationsMap.get(variantName).size());
            } else {
                System.out.println("No translations for " + variantName);
            }
            variantRestful.addTranslations(variantTranslationsMap.get(variantName).values());

            for(String t : variantTranslationsMap.keySet()) {
                System.out.println("Map entry " + t);
            }

            variants.put(variantRestful.getNodeType(), variantRestful);
        }
        return variants;
    }

    private void test() {
        final PluginContext pluginContext = getPluginContext();
        for(final String imageSetType : listImageSetTypes(pluginContext)) {

        }
    }

    private void fetchImageSet(final Session session, final String type) {
        //GalleryUtils.
    }

    private Map<String,Map<String, TranslationRestful>> getVariantTranslationsMap(final Session session) throws RepositoryException {
        final Map<String, Map<String,TranslationRestful>> map = new HashMap<>();
        for(final Node node : fetchVariantTranslations(session)) {
            final TranslationRestful translation = new TranslationRestful();
            translation.setLocale(TranslationUtils.getHippoLanguage(node));
            translation.setMessage(TranslationUtils.getHippoMessage(node));

            final String propertyName = TranslationUtils.getHippoProperty(node);
            if(StringUtils.isBlank(propertyName)) {
                System.out.println("Skipping translation: " + node.getPath());
                continue;
            } else {
                System.out.println("Adding translation: " + node.getPath());
            }
            if(!map.containsKey(propertyName)) {
                map.put(propertyName, new HashMap<String, TranslationRestful>());
            }
            map.get(propertyName).put(translation.getLocale(), translation);
        }
        return map;
    }

    @PUT
    @Path("/imagesets/save")
    public String saveImageSets() throws RepositoryException {
        return "TODO save image sets";
    }

    @GET
    @Path("/imagesets/")
    public List<ImageSetRestful> fetchImageSets() throws RepositoryException {

        final ImageProcessorRestful processorRestful = new ImageProcessorRestful();
        // TODO verify the use and creation of the plugin context
        final PluginContext pluginContext = getPluginContext();

        final Session session = pluginContext.getSession();


        final Node processorNode = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);
        final List<ImageSetRestful> imageSets = fetchImageSets(session);
        populateVariantsInImageSets(session, imageSets, processorNode);

        return imageSets;
    }

    private void populateImageSetsInVariants(final Session session, final Collection<ImageVariantRestful> variants) throws RepositoryException {
        final List<ImageSetRestful> imageSets = fetchImageSets(session);
        for(final ImageVariantRestful variant : variants) {
            populateImageSetsInVariant(variant, imageSets);
        }
    }

    private void populateImageSetsInVariant(final ImageVariantRestful variant, final List<ImageSetRestful> availableImageSets) throws RepositoryException {
        final List<ImageSetRestful> imageSets = new ArrayList<>();
        for(final ImageSetRestful imageSet: availableImageSets) {
            if(imageSet.hasVariant(variant.getNamespace(),variant.getName())) {
                imageSets.add(imageSet);
            }
        }
        variant.setImageSets(imageSets);
    }


    private void populateVariantsInImageSets(final Session session, final List<ImageSetRestful> imageSets, final Node processorNode) throws RepositoryException {
        final Map<String, ImageVariantRestful> availableVariants = fetchImageProcessorVariants(session, processorNode);
        for(final ImageSetRestful imageSet : imageSets) {
            populateVariantsInImageSet(imageSet, availableVariants);
        }
    }

    private void populateVariantsInImageSet(ImageSetRestful imageSet, final Map<String, ImageVariantRestful> availableVariants) throws RepositoryException {
        final List<ImageVariantRestful> variants = new ArrayList<>();
        for(final ImageVariantRestful tempVariant : imageSet.getVariants()) {
            final ImageVariantRestful variant = availableVariants.get(tempVariant.getNodeType());
            variants.add(variant);
        }
        imageSet.setVariants(variants);
    }


    private List<ImageSetRestful> fetchImageSets(final Session session) throws RepositoryException {
        final List<ImageSetRestful> imageSets = new ArrayList<>();
        final PluginContext pluginContext = getPluginContext();
        final List<Node> nodes = fetchImageSetNamespaceNodes(session, listImageSetTypes(pluginContext));
        for(final Node node : nodes) {
            final ImageSetRestful imageSet = new ImageSetRestful();
            imageSet.setId(node.getIdentifier());
            imageSet.setPath(node.getPath());
            imageSet.setName(node.getName());
            imageSet.setNamespace(node.getParent().getName());
            imageSet.setVariants(getVariantsForImageSetNamespaceNode(node));

            imageSets.add(imageSet);
        }
        return imageSets;
    }

    private List<Node> fetchFieldsFromNamespaceNode(final Node namespaceNode, final String fieldType) throws RepositoryException {
        if(!namespaceNode.isNodeType("hipposysedit:templatetype")) {
            return Collections.emptyList();
        }

        final Node nodeTypeHandle = JcrUtils.getNodeIfExists(namespaceNode, "hipposysedit:nodetype");
        if(nodeTypeHandle == null) {
            return Collections.emptyList();
        }

        final Node nodeTypeNode = JcrUtils.getNodeIfExists(nodeTypeHandle, "hipposysedit:nodetype");
        if(nodeTypeNode == null) {
            return Collections.emptyList();
        }

        final List<Node> fields = new ArrayList<>();
        final NodeIterator iterator = nodeTypeNode.getNodes();
        while(iterator.hasNext()) {
            final Node node = iterator.nextNode();
            if(fieldType == null || fieldType.equals(JcrUtils.getStringProperty(node,"hipposysedit:type",null))) {
                fields.add(node);
            }
        }
        return fields;
    }

    private List<ImageVariantRestful> getVariantsForImageSetNamespaceNode(final Node imageSetNode) throws RepositoryException {
        final List<ImageVariantRestful> imageSets = new ArrayList<>();
        final List<Node> nodes = fetchFieldsFromNamespaceNode(imageSetNode, "hippogallery:image");
        for(final Node node : nodes) {
            final String documentType = JcrUtils.getStringProperty(node, "hipposysedit:path", null);
            imageSets.add(new ImageVariantRestful(HippoNodeUtils.getPrefixFromType(documentType),HippoNodeUtils.getNameFromType(documentType)));
        }
        return imageSets;
    }


    private List<Node> fetchVariantTranslations(final Session session) throws RepositoryException {
        final List<Node> variantTranslations = new ArrayList<>();
        final PluginContext pluginContext = getPluginContext();
        final List<Node> nodes = fetchImageSetNamespaceNodes(session, listImageSetTypes(pluginContext));
        System.out.println("Image set nodes: " + nodes.size());
        for(final Node imageSetNSNode : nodes) {
            System.out.println("Image set node: " + imageSetNSNode.getPath());
            variantTranslations.addAll(TranslationUtils.getTranslationsFromNode(imageSetNSNode));
        }
        return variantTranslations;
    }

    private List<Node> fetchImageSetNamespaceNodes(final Session session, final List<String> imageSets) throws RepositoryException {
        final List<Node> nodes = new ArrayList<>();
        for(final String imageSet : imageSets) {
            System.out.println("Fetch Image set NS node for: " + imageSet);
            nodes.add(fetchImageSetNamespaceNode(session, imageSet));
        }
        return nodes;
    }

    private Node fetchImageSetNamespaceNode(final Session session, final String imageSet) throws RepositoryException {
        return session.getNode(getPathToNamespaceNode(imageSet));
    }

    private String getPathToNamespaceNode(final String documentType) {
        return "/hippo:namespaces/" + HippoNodeUtils.getPrefixFromType(documentType) + '/' + HippoNodeUtils.getNameFromType(documentType);
    }

    private List<String> listImageSetTypes(final PluginContext pluginContext) {
        try {
            return CndUtils.getNodeTypesOfType(pluginContext, HippoGalleryNodeType.IMAGE_SET, true);
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve node types", e);
        }
        return Collections.emptyList();

    }

    private PluginContext getPluginContext() {
        return new DashboardPluginContext(GlobalUtils.createSession(), null);
    }


}
