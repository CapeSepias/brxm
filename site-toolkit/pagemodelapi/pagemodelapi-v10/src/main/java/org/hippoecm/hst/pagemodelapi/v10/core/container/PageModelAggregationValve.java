/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10.core.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.AggregationValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemenu.CommonMenu;
import org.hippoecm.hst.pagemodelapi.common.content.beans.PageModelObjectMapperFactory;
import org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson.LinkModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ComponentWindowModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.IdentifiableLinkableMetadataBaseModel;
import org.hippoecm.hst.util.ParametersInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

/**
 * Page model aggregation valve, to write a JSON model from the aggregated data for a page request.
 */
public class PageModelAggregationValve extends AggregationValve {

    private static Logger log = LoggerFactory.getLogger(PageModelAggregationValve.class);

    /**
     * Page or component parameter metadata name.
     */
    private static final String PARAMETERS_METADATA = "params";

    /**
     * Page or component parametersInfo metadata name.
     */
    private static final String PARAMETERS_INFO_METADATA = "paramsInfo";

    /**
     * Page title metadata name.
     */
    private static final String PAGE_TITLE_METADATA = "pageTitle";

    /**
     * Page definition ID (from the configuration) metadata name.
     */
    private static final String PAGE_DEFINITION_ID_METADATA = "definitionId";

    /**
     * Preview mode meta-data name.
     */
    private static final String PAGE_PREVIEW_METADATA = "preview";

    /**
     * Maximum content reference level request parameter name.
     */
    private static final String MAX_CONTENT_REFERENCE_LEVEL_PARAM_NAME = "_maxreflevel";

    /**
     * Page Model Object Jackson ObjectMapper instance for JSON (de)serialization.
     */
    private final ObjectMapper pageModelObjectMapper;

    private final ObjectMapper simpleObjectMapper = new ObjectMapper();
    /**
     * Custom metadata decorators.
     */
    private final List<MetadataDecorator> metadataDecorators = new ArrayList<>();

    private int defaultMaxContentReferenceLevel;

    private String apiDocPath;

    private boolean prettyPrint;

    public PageModelAggregationValve(final PageModelObjectMapperFactory factory, final Map<Class<?>, Class<?>> extraMixins,
                                     final JsonPointerFactory jsonPointerFactory) {
        pageModelObjectMapper = factory.createPageModelObjectMapper().registerModule(new SimpleModule().setSerializerModifier(
                new PageModelSerializerModifier(metadataDecorators, jsonPointerFactory)
        ));
        HstBeansObjectMapperDecorator.decorate(pageModelObjectMapper, extraMixins);

    }

    public void setDefaultMaxContentReferenceLevel(int defaultMaxContentReferenceLevel) {
        this.defaultMaxContentReferenceLevel = defaultMaxContentReferenceLevel;
    }

    public void setPrettyPrint(final boolean prettyPrint) {

        this.prettyPrint = prettyPrint;
    }

    /**
     * <p>
     *     Add a custom {@link MetadataDecorator} instance, which is invoked to give a chance to customize
     *     the {@link MetadataContributable} instances in the page model aggregation result output.
     * </p>
     * <p>
     *     Downstream projects like enterprise modules can inject extra meta data decorators
     * </p>
     * @param metadataDecorator custom {@link MetadataDecorator} instance
     */
    @SuppressWarnings("unused")
    public void addMetadataDecorator(MetadataDecorator metadataDecorator) {
        metadataDecorators.add(metadataDecorator);
    }

    /**
     * Set API Document (e.g, /swagger.json) Path.
     * @param apiDocPath API Document (e.g, /swagger.json) Path
     */
    public void setApiDocPath(String apiDocPath) {
        this.apiDocPath = apiDocPath;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides <code>AggregationValve#processWindowsRender()</code> to create an {@link AggregatedPageModel}
     * from the current page request and write it as JSON output.
     */
    @Override
    protected void processWindowsRender(final HstContainerConfig requestContainerConfig,
            final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {
        final AggregatedPageModel aggregatedPageModel = (AggregatedPageModel) createAggregatedPageModel(
                sortedComponentWindows, requestMap, responseMap);
        setCurrentAggregatedPageModel(aggregatedPageModel);
    }

    /**
     * Create an aggregated page model to write.
     * <p>
     * Note: if this method is overriden, it is supposed to override {@link #writeAggregatedOutput(ValveContext, HstComponentWindow)}
     * method as well because the method is reponsible for serializaing the returned object from this method.
     * @param sortedComponentWindows sorted component window array which was sorted by the parent {@link AggregationValve}
     * @param requestMap HST Request map for each {@link HstComponentWindow} instance
     * @param responseMap HST Response map for each {@link HstComponentWindow} instance
     * @return an aggregated page model to write
     * @throws ContainerException if container exception occurs
     */
    protected Object createAggregatedPageModel(final HstComponentWindow[] sortedComponentWindows,
                                               final Map<HstComponentWindow, HstRequest> requestMap,
                                               final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {
        final HstRequestContext requestContext = RequestContextProvider.get();

        // root component (page component) is the first item in the sortedComponentWindows.
        final HstComponentWindow rootWindow = sortedComponentWindows[0];

        final AggregatedPageModel aggregatedPageModel = new AggregatedPageModel(null);
        decorateAggregatedPageModel(requestContext, aggregatedPageModel);

        final ComponentWindowModel pageWindowModel = new ComponentWindowModel(rootWindow);

        final String definitionId = rootWindow.getComponentInfo().getId();
        if (StringUtils.isNotEmpty(definitionId)) {
            pageWindowModel.putMetadata(PAGE_DEFINITION_ID_METADATA, definitionId);
        }

        final String pageTitle = requestContext.getResolvedSiteMapItem().getPageTitle();
        if (StringUtils.isNotEmpty(pageTitle)) {
            pageWindowModel.putMetadata(PAGE_TITLE_METADATA, pageTitle);
        }

        aggregatedPageModel.setPageWindowModel(pageWindowModel);
        addPreviewFlagToPageModel(aggregatedPageModel, requestContext);

        // include api version to _meta section
        aggregatedPageModel.putMetadata("version",
                requestContext.getServletRequest().getAttribute(ContainerConstants.PAGE_MODEL_API_VERSION));

        addLinksToPageModel(aggregatedPageModel);

        final int sortedComponentWindowsLen = sortedComponentWindows.length;

        for (int i = 0; i < sortedComponentWindowsLen; i++) {
            final HstComponentWindow window = sortedComponentWindows[i];
            final HstRequest hstRequest = requestMap.get(window);
            final HstResponse hstResponse = responseMap.get(window);

            final ComponentWindowModel currentComponentWindowModel = aggregatedPageModel.getModel(window.getReferenceNamespace())
                    .orElseThrow(() -> new ContainerException(
                            String.format("Expected window for '%s' to be present", window.getReferenceName())));

            addComponentRenderingURLLink(hstResponse, currentComponentWindowModel);
            addParametersInfoMetadata(window, hstRequest, currentComponentWindowModel);
            decorateComponentWindowMetadata(hstRequest, hstResponse, currentComponentWindowModel);

            for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                currentComponentWindowModel.putModel(entry.getKey(), entry.getValue());
            }
        }

        return aggregatedPageModel;
    }

    /**
     * <code>HstRequestContext</code> specific {@link AggregatedPageModel} attribute name.
     */
    private static final String AGGREGATED_PAGE_MODEL_ATTR = PageModelAggregationValve.class.getName()
            + ".aggregatedPageModel";

    /**
     * Return the current {@link AggregatedPageModel} object.
     * @return the current {@link AggregatedPageModel} object
     */
    public static AggregatedPageModel getCurrentAggregatedPageModel() {
        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext != null) {
            return (AggregatedPageModel) requestContext.getAttribute(AGGREGATED_PAGE_MODEL_ATTR);
        }

        return null;
    }

    /**
     * Set the current {@link AggregatedPageModel} object.
     * @param aggregatedPageModel the current {@link AggregatedPageModel} object
     */
    public static void setCurrentAggregatedPageModel(AggregatedPageModel aggregatedPageModel) {
        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not available.");
        }

        requestContext.setAttribute(AGGREGATED_PAGE_MODEL_ATTR, aggregatedPageModel);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides <code>AggregationValve#processWindowsRender()</code> to create an {@link AggregatedPageModel}
     * from the current page request and write it as JSON output.
     */
    @Override
    protected void writeAggregatedOutput(final ValveContext context, final HstComponentWindow rootRenderingWindow)
            throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();

        final HttpServletResponse response = requestContext.getServletResponse();

        // TODO include logic for final int maxRefLevel = NumberUtils.toInt(
        // TODO  requestContext.getServletRequest().getParameter(MAX_CONTENT_REFERENCE_LEVEL_PARAM_NAME),
        // TODO         defaultMaxContentReferenceLevel);

        try {

            PageModelSerializer.initContext();

            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader(ContainerConstants.PAGE_MODEL_API_VERSION,
                    (String)requestContext.getServletRequest().getAttribute(ContainerConstants.PAGE_MODEL_API_VERSION));


            if (prettyPrint) {
                pageModelObjectMapper.writerWithDefaultPrettyPrinter().writeValue(response.getWriter(), getCurrentAggregatedPageModel());
            } else {
                pageModelObjectMapper.writeValue(response.getWriter(), getCurrentAggregatedPageModel());
            }


        } catch (JsonGenerationException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (IOException e) {
            log.warn("Failed to write aggregated page model in json.", e);
        } finally {
            PageModelSerializer.closeContext();
        }
    }

    @Override
    protected boolean isAggregationApiDocumentRequest(final ValveContext context) {
        if (StringUtils.isNotBlank(apiDocPath)) {
            final HstRequestContext requestContext = context.getRequestContext();
            final HstContainerURL baseURL = requestContext.getBaseURL();
            return apiDocPath.equals(baseURL.getPathInfo());
        }

        return false;
    }

    /**
     * Adding componentRendering URL link to the linkable model.
     * @param hstResponse HstResponse
     * @param linkableModel linkable model
     */
    private void addComponentRenderingURLLink(HstResponse hstResponse,
            IdentifiableLinkableMetadataBaseModel linkableModel) {
        HstURL compRenderURL = hstResponse.createComponentRenderingURL();
        linkableModel.putLink(ContainerConstants.LINK_NAME_COMPONENT_RENDERING, new LinkModel(compRenderURL.toString()));
    }

    /**
     * Add <code>params</code> metadata to the {@code model} from the {@code window}.
     * @param window HST Component Window instance
     * @param model the {@link MetadataContributable} model instance where the parameter map should be contributed to
     */
    private void addParametersInfoMetadata(HstComponentWindow window, HstRequest hstRequest, MetadataContributable model) {
        final ComponentConfiguration compConfig = (window.getComponent() != null)
                ? window.getComponent().getComponentConfiguration()
                : null;

        if (compConfig == null) {
            return;
        }

        final Object paramsInfo = ParametersInfoUtils.createParametersInfo(window.getComponent(), compConfig, hstRequest);

        if (paramsInfo != null) {
            try {
                final JsonNode paramsInfoNode = simpleObjectMapper.valueToTree(paramsInfo);
                model.putMetadata(PARAMETERS_INFO_METADATA, paramsInfoNode);
            } catch (Exception e) {
                log.warn("Failed to convert ParametersInfo instance ({}) to ObjectNode.", paramsInfo, e);
            }
        }

        final ResolvedSiteMapItem resolvedSiteMapItem = RequestContextProvider.get().getResolvedSiteMapItem();
        final ObjectNode paramsNode = simpleObjectMapper.getNodeFactory().objectNode();

        for (String paramName : compConfig.getParameterNames()) {
            final String paramValue = compConfig.getParameter(paramName, resolvedSiteMapItem);

            if (paramValue != null) {
                paramsNode.put(paramName, paramValue);
            }
        }

        model.putMetadata(PARAMETERS_METADATA, paramsNode);
    }

    private void addPreviewFlagToPageModel(final AggregatedPageModel aggregatedPageModel, final HstRequestContext requestContext) {
        if (requestContext.isChannelManagerPreviewRequest()) {
            aggregatedPageModel.putMetadata(PAGE_PREVIEW_METADATA, true);
        }
    }

    /**
     * Add links to the page model.
     * @param pageModel the aggregated page model instance
     */
    private void addLinksToPageModel(IdentifiableLinkableMetadataBaseModel pageModel) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        final HstSiteMapItem siteMapItem = requestContext.getResolvedSiteMapItem().getHstSiteMapItem();

        final Mount selfMount = requestContext.getResolvedMount().getMount();
        final HstLink selfLink = linkCreator.create(siteMapItem, selfMount);
        pageModel.putLink(ContainerConstants.LINK_NAME_SELF, new LinkModel(selfLink.toUrlForm(requestContext, true)));

        final Mount siteMount = selfMount.getParent();
        if (siteMount != null) {
            final HstLink siteLink = linkCreator.create(siteMapItem, siteMount);
            pageModel.putLink(ContainerConstants.LINK_NAME_SITE, new LinkModel(siteLink.toUrlForm(requestContext, true)));
        } else {
            log.warn("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                    "case for '{}'. Cannot add site links", selfMount);
        }
    }


    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the aggregated page model.
     * @param requestContext the HST requestContext object
     * @param aggregatedPageModel MetadataContributable model
     */
    private void decorateAggregatedPageModel(final HstRequestContext requestContext,
                                             final AggregatedPageModel aggregatedPageModel) {
        if (CollectionUtils.isEmpty(metadataDecorators)) {
            return;
        }

        for (MetadataDecorator decorator : metadataDecorators) {
            decorator.decorateAggregatedPageModelMetadata(requestContext, aggregatedPageModel);
        }
    }

    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the component window.
     * @param hstRequest HstRequest object
     * @param hstResponse HstResponse object
     * @param model MetadataContributable model
     */
    private void decorateComponentWindowMetadata(final HstRequest hstRequest, final HstResponse hstResponse,
            MetadataContributable model) {
        if (CollectionUtils.isEmpty(metadataDecorators)) {
            return;
        }

        for (MetadataDecorator decorator : metadataDecorators) {
            decorator.decorateComponentWindowMetadata(hstRequest, hstResponse, model);
        }
    }

    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the component window.
     * @param menuWrapperModel a wrapper model for a {@link CommonMenu}
     */
//    private void decorateCommonMenuMetadata(final DecoratedPageModelEntityWrapper<CommonMenu> menuWrapperModel) {
//        if (CollectionUtils.isEmpty(metadataDecorators)) {
//            return;
//        }
//
//        final HstRequestContext requestContext = RequestContextProvider.get();
//
//        for (MetadataDecorator decorator : metadataDecorators) {
//            decorator.decorateCommonMenuMetadata(requestContext, menuWrapperModel.getData(), menuWrapperModel);
//        }
//    }
//
//    /**
//     * @param menuWrapperModel a wrapper model for a {@link HippoDocumentBean}
//     */
//    private void decorateDocumentMetadata(final DecoratedPageModelEntityWrapper<HippoDocumentBean> menuWrapperModel) {
//        if (CollectionUtils.isEmpty(metadataDecorators)) {
//            return;
//        }
//
//        final HstRequestContext requestContext = RequestContextProvider.get();
//
//        for (MetadataDecorator decorator : metadataDecorators) {
//            decorator.decorateContentMetadata(requestContext, menuWrapperModel.getData(), menuWrapperModel);
//        }
//    }
}
