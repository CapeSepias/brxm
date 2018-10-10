/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDERING_HOST;
import static org.joor.Reflect.on;
import static org.onehippo.cms7.services.cmscontext.CmsSessionContext.SESSION_KEY;

public class AbstractPageComposerTest {

    private static final String CONTEXT_PATH = "/site";

    protected SpringComponentManager componentManager;
    protected HstManager hstManager;
    protected HstURLFactory hstURLFactory;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstEventsCollector hstEventsCollector;
    protected HippoSession session;
    protected Object hstModelMutex;
    protected PreviewDecorator previewDecorator;
    protected HippoWebappContext webappContext = new HippoWebappContext(SITE, new MockServletContext() {
        public String getContextPath() {
            return CONTEXT_PATH;
        }
    });

    protected MountResource mountResource;


    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() throws Exception {
        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());

        HippoWebappContextRegistry.get().register(webappContext);
        componentManager.setServletContext(webappContext.getServletContext());

        final List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());

        mountResource =  componentManager.getComponent("org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource",
                "org.hippoecm.hst.pagecomposer");

        final HstModelProvider hstModelProvider = componentManager.getComponent(HstModelProvider.class.getName());
        hstModelProvider.setContextPath(CONTEXT_PATH);

        final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final HstModel hstModel = hstModelRegistry.registerHstModel(CONTEXT_PATH, componentManager, false);

        hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        siteMapMatcher = hstModel.getHstSiteMapMatcher();
        hstURLFactory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());

        hstEventsCollector = on(hstModel)
                .field("invalidationMonitor")
                .field("hstEventsDispatcher")
                .field("hstEventsCollector").get();
                //HstServices.getComponentManager().getComponent("hstEventsCollector");

        hstModelMutex = HstServices.getComponentManager().getComponent("hstModelMutex");
        previewDecorator = HstServices.getComponentManager().getComponent(PreviewDecorator.class.getName());

        session = (HippoSession)createSession();

        createHstConfigBackup(session);

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.registerHstModel(CONTEXT_PATH, componentManager, true);


    }

    @After
    public void tearDown() throws Exception {

        // to avoid jr problems with current session with shared depth kind of issues, use a refresh
        session.refresh(false);
        restoreHstConfigBackup(session);

        session.logout();

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.unregisterHstModel("/cms");

        this.componentManager.stop();
        this.componentManager.close();
        HippoWebappContextRegistry.get().unregister(webappContext);
        HstServices.setComponentManager(null);
        ModifiableRequestContextProvider.clear();

    }

    protected String[] getConfigurations() {
        String classXmlFileName = AbstractPageComposerTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractPageComposerTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }


    protected Session createLiveUserSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("liveuser", "liveuserpass".toCharArray()));
    }

    protected Configuration getContainerConfiguration() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        // FIXME: for now set hst.configuration.rootPath, otherwise hst model building fails.
        //        assume the hst.configuration.rootPath property should be set to something else in tests later.
        configuration.setProperty("hst.configuration.rootPath", "/hst:hst");
        return configuration;
    }

    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final String hostAndPort,
                                                                                        final String pathInfo) throws Exception {
        return getRequestContextWithResolvedSiteMapItemAndContainerURL(null, hostAndPort, pathInfo, null);
    }

    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final String scheme,
                                                                                        final String hostAndPort,
                                                                                        final String pathInfo,
                                                                                        final String queryString) throws Exception {
        HstRequestContextComponent rcc = HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrl(scheme, hostAndPort, pathInfo, requestContext, queryString);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl, requestContext);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        requestContext.matchingFinished();
        HstURLFactory hstURLFactory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);

        final HstModel hstModel = modelRegistry.getHstModel("/cms");

        //new CXFJaxrsHstConfigService.HstModelSnapshot((PlatformHstModel) hstModel);
        final HstModel liveHstModelSnapshot =
                on(CXFJaxrsHstConfigService.class.getName() + "$HstModelSnapshot")
                        .create((PlatformHstModel) hstModel).get();

        //new CXFJaxrsHstConfigService.HstModelSnapshot((PlatformHstModel) liveHstModelSnapshot, previewDecorator);
        final HstModel previewHstModelSnapshot =
                on(CXFJaxrsHstConfigService.class.getName() + "$HstModelSnapshot")
                        .create(liveHstModelSnapshot, previewDecorator).get();

        requestContext.setAttribute(PageComposerContextService.LIVE_EDITING_HST_MODEL_ATTR, liveHstModelSnapshot);
        requestContext.setAttribute(PageComposerContextService.PREVIEW_EDITING_HST_MODEL_ATTR, previewHstModelSnapshot);

        requestContext.getServletRequest().setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        ModifiableRequestContextProvider.set(requestContext);

        return requestContext;
    }

    protected HstContainerURL createContainerUrl(final String scheme,
                                                 final String hostAndPort,
                                                 final String pathInfo,
                                                 final HstMutableRequestContext requestContext,
                                                 final String queryString) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();

        requestContext.setServletRequest(mockRequest);
        requestContext.setServletResponse(response);
        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            mockRequest.setLocalPort(port);
            mockRequest.setServerPort(port);
        }
        if (scheme == null) {
            mockRequest.setScheme("http");
        } else {
            mockRequest.setScheme(scheme);
        }
        mockRequest.setServerName(host);
        mockRequest.addHeader("Host", hostAndPort);
        mockRequest.setPathInfo(pathInfo);
        mockRequest.setContextPath("/cms");
        mockRequest.setRequestURI("/cms" + pathInfo);

        if (queryString != null) {
            mockRequest.setQueryString(queryString);
        }
        GenericHttpServletRequestWrapper containerRequest = new HstContainerRequestImpl(mockRequest, hstManager.getPathSuffixDelimiter());

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                containerRequest.getContextPath(), HstRequestUtils.getRequestPath(containerRequest));

        if (mount.getMatchingIgnoredPrefix() != null) {
            containerRequest.setServletPath("/" + mount.getMatchingIgnoredPrefix() + mount.getResolvedMountPath());
        } else {
            containerRequest.setServletPath(mount.getResolvedMountPath());
        }

        final String mountId = mount.getMount().getIdentifier();
        requestContext.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, mountId);
        // TODO HSTTWO-4374 this does not work any more, use CmsSessionContext instead?
//        mockRequest.getSession().setAttribute(RENDERING_HOST, host);
//        mockRequest.getSession().setAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID, mountId);

        CmsSessionContextMock contextMock =
                new CmsSessionContextMock(new SimpleCredentials("admin", "admin".toCharArray()));
        mockRequest.getSession().setAttribute(SESSION_KEY, contextMock);
        contextMock.getContextPayload().put(CMS_REQUEST_RENDERING_MOUNT_ID, mountId);
        contextMock.getContextPayload().put(RENDERING_HOST, host);

        mockRequest.setAttribute(SESSION_KEY, contextMock);

        return hstURLFactory.getContainerURLProvider().parseURL(mockRequest, response, mount);
    }

    public static class CmsSessionContextMock implements CmsSessionContext {

        private SimpleCredentials credentials;
        Map<String, Serializable> contextPayload;

        public CmsSessionContextMock(Credentials credentials) {
            this.contextPayload = new HashMap<>();
            this.credentials = (SimpleCredentials)credentials;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public Map<String, Serializable> getContextPayload() {
            return contextPayload;
        }

        @Override
        public String getCmsContextServiceId() {
            return null;
        }

        @Override
        public Object get(final String key) {
            return CmsSessionContext.REPOSITORY_CREDENTIALS.equals(key) ? credentials : null;
        }

    }

    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url, final HstMutableRequestContext requestContext) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getContextPath(), url.getRequestPath());
        final Mount decorated = previewDecorator.decorateMountAsPreview(resolvedMount.getMount());
        ((MutableResolvedMount) resolvedMount).setMount(decorated);
        String pathInfo = url.getPathInfo();
        if (StringUtils.isNotEmpty(resolvedMount.getResolvedMountPath())) {
            pathInfo = pathInfo.substring(resolvedMount.getResolvedMountPath().length());
        }
        return resolvedMount.matchSiteMapItem(pathInfo);
    }

    public static void createHstConfigBackup(Session session) throws RepositoryException {
        if (!session.nodeExists("/hst-backup")) {
            JcrUtils.copy(session, "/hst:hst", "/hst-backup");
            session.save();
        }
    }

    public static void restoreHstConfigBackup(Session session) throws RepositoryException {
        if (session.nodeExists("/hst-backup")) {
            if (session.nodeExists("/hst:hst")) {
                session.removeItem("/hst:hst");
            }
            JcrUtils.copy(session, "/hst-backup", "/hst:hst");
            session.removeItem("/hst-backup");
            session.save();
        }
    }


    protected void moveChannelToWorkspace() throws RepositoryException {
        final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        if (!unitTestConfigNode.hasNode("hst:workspace")) {
            unitTestConfigNode.addNode("hst:workspace", "hst:workspace");
        }
        session.move("/hst:hst/hst:configurations/unittestproject/hst:channel",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:channel");
    }

    protected void createWorkspaceWithTestContainer() throws RepositoryException {
        final Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        final Node workspace = unitTestConfigNode.addNode("hst:workspace", "hst:workspace");
        final Node containers = workspace.addNode("hst:containers", "hst:containercomponentfolder");

        final Node containerNode = containers.addNode("testcontainer", "hst:containercomponent");
        containerNode.setProperty("hst:xtype", "HST.vBox");
    }

    protected void movePagesFromCommonToUnitTestProject() throws RepositoryException {
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages", "/hst:hst/hst:configurations/unittestproject/hst:pages");
    }

    protected void addReferencedContainerForHomePage() throws RepositoryException {
        final Node container = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:pages/homepage")
                .addNode("container", "hst:containercomponentreference");
        container.setProperty("hst:referencecomponent", "testcontainer");
    }

    protected String addCatalogItem() throws RepositoryException {
        Node unitTestConfigNode = session.getNode("/hst:hst/hst:configurations/unittestproject");
        final Node catalog = unitTestConfigNode.addNode("hst:catalog", "hst:catalog");
        final Node catalogPackage = catalog.addNode("testpackage", "hst:containeritempackage");
        final Node catalogItem = catalogPackage.addNode("testitem", "hst:containeritemcomponent");
        catalogItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE, "thankyou");
        catalogItem.setProperty("hst:xtype", "HST.Item");
        return catalogItem.getIdentifier();
    }

    protected void registerChannelEventListener(Object listener) {
        ChannelEventListenerRegistry.get().register(listener);
    }

    protected void unregisterChannelEventListener(Object listener) {
        ChannelEventListenerRegistry.get().unregister(listener);
    }

}
