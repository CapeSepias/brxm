/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstConfigurationAugmenter;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.HstLinkProcessor;
import org.hippoecm.hst.core.linking.HstLinkProcessorChain;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.RewriteContextResolver;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.channel.ChannelManagerImpl;
import org.hippoecm.hst.platform.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.platform.linking.CompositeHstLinkCreatorImpl;
import org.hippoecm.hst.platform.linking.DefaultHstLinkCreator;
import org.hippoecm.hst.platform.linking.DefaultRewriteContextResolver;
import org.hippoecm.hst.platform.linking.containers.DefaultResourceContainer;
import org.hippoecm.hst.platform.linking.containers.HippoGalleryAssetSet;
import org.hippoecm.hst.platform.linking.containers.HippoGalleryImageSetContainer;
import org.hippoecm.hst.platform.linking.resolvers.HippoResourceLocationResolver;
import org.hippoecm.hst.platform.matching.BasicHstSiteMapMatcher;
import org.hippoecm.hst.platform.services.channel.ContentBasedChannelFilter;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class HstModelImpl implements PlatformHstModel {

    private static final Logger log = LoggerFactory.getLogger(HstModelImpl.class);

    private Session session;
    private final String contextPath;
    private final ContainerConfiguration websiteContainerConfiguration;
    private ClassLoader platformClassloader = this.getClass().getClassLoader();
    private final ComponentManager websiteComponentManager;
    private final HstNodeLoadingCache hstNodeLoadingCache;
    private final HstConfigurationLoadingCache hstConfigurationLoadingCache;
    private final BasicHstSiteMapMatcher hstSiteMapMatcher;
    private final HstLinkCreator hstLinkCreator;
    private final ChannelManager channelManager;

    private volatile VirtualHosts virtualHosts;
    private BiPredicate<Session, Channel> channelFilter;

    private InvalidationMonitor invalidationMonitor;

    public HstModelImpl(final Session session,
                        final String contextPath,
                        final ComponentManager websiteComponentManager,
                        final HstNodeLoadingCache hstNodeLoadingCache,
                        final HstConfigurationLoadingCache hstConfigurationLoadingCache) throws RepositoryException {
        this.session = session;
        this.contextPath = contextPath;
        this.websiteComponentManager = websiteComponentManager;
        websiteContainerConfiguration = websiteComponentManager.getContainerConfiguration();
        this.hstNodeLoadingCache = hstNodeLoadingCache;
        this.hstConfigurationLoadingCache = hstConfigurationLoadingCache;

        hstSiteMapMatcher = new BasicHstSiteMapMatcher();
        configureSiteMapMatcher();

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);

        final DefaultHstLinkCreator defaultHstLinkCreator = new DefaultHstLinkCreator();
        configureHstLinkCreator(defaultHstLinkCreator, websiteComponentManager);

        this.hstLinkCreator = new CompositeHstLinkCreatorImpl(modelRegistry, defaultHstLinkCreator);

        channelFilter = configureChannelFilter(new ContentBasedChannelFilter());

        invalidationMonitor = new InvalidationMonitor(session, hstNodeLoadingCache, hstConfigurationLoadingCache, this);

        final String contentRoot = websiteContainerConfiguration.getString("channel.manager.contentRoot", ChannelManagerImpl.DEFAULT_CONTENT_ROOT);
        channelManager = new ChannelManagerImpl(this, invalidationMonitor.getEventPathsInvalidator(), hstNodeLoadingCache, contentRoot);

        // TODO add the channelManagerEventListeners??
    }

    public void destroy() throws RepositoryException {
        invalidationMonitor.destroy();
        session.logout();
    }

    public synchronized void invalidate() {
        virtualHosts = null;
    }

    @Override
    public VirtualHosts getVirtualHosts() {
        VirtualHosts vhosts = virtualHosts;
        if (vhosts != null) {
            return vhosts;
        }

        synchronized (this) {
            vhosts = virtualHosts;
            if (vhosts != null) {
                return vhosts;
            }

            // dispatch all events
            invalidationMonitor.dispatchHstEvents();

            // make sure that the Thread class loader during model loading is the platform classloader
            final ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
            try {
                if (platformClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(platformClassloader);
                }
                final VirtualHostsService virtualHosts = new VirtualHostsService(contextPath, websiteContainerConfiguration, hstNodeLoadingCache, hstConfigurationLoadingCache);
                augment(virtualHosts);

                this.virtualHosts = virtualHosts;
                return this.virtualHosts;
            } catch (RuntimeException e) {
                log.error("Exception loading model", e);
                throw e;
            } catch (Exception e) {
                log.error("Exception loading model", e);
                throw new RuntimeException(e);
            } finally {
                if (platformClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }
            }
        }
    }

    @Override
    public boolean isHstConfigurationNodesLoaded() {
        return hstNodeLoadingCache.isHstNodesLoaded();
    }

    private void augment(final VirtualHostsService virtualHosts) throws ContainerException {
        List<HstConfigurationAugmenter> configurationAugmenters = websiteComponentManager.getComponent(HstManager.class).getHstConfigurationAugmenters();
        for (HstConfigurationAugmenter configurationAugmenter : configurationAugmenters) {
            configurationAugmenter.augment(virtualHosts);
        }
    }

    @Override
    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return hstSiteMapMatcher;
    }

    @Override
    public HstLinkCreator getHstLinkCreator() {
        return hstLinkCreator;
    }

    // internal api!
    @Override
    public BiPredicate<Session, Channel> getChannelFilter() {
        return channelFilter;
    }

    // internal api!
    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    // internal api!
    @Override
    public String getConfigurationRootPath() {
        return hstNodeLoadingCache.getRootPath();
    }

    // internal api!
    @Override
    public EventPathsInvalidator getEventPathsInvalidator() {
        return invalidationMonitor.getEventPathsInvalidator();
    }

    private void configureSiteMapMatcher() {
        hstSiteMapMatcher.setLinkProcessor(getHstLinkProcessor(websiteComponentManager));
    }

    private HstLinkProcessor getHstLinkProcessor(final ComponentManager websiteComponentManager) {
        HstLinkProcessor customLinkProcessor = websiteComponentManager.getComponent(HstLinkProcessor.class.getName());
        if (customLinkProcessor != null) {
            return customLinkProcessor;
        }
        return new HstLinkProcessorChain();
    }

    private RewriteContextResolver getRewriteContextResolver(final ComponentManager websiteComponentManager) {
        final RewriteContextResolver customRewriteContextResolver = websiteComponentManager.getComponent(RewriteContextResolver.class.getName());
        if (customRewriteContextResolver != null) {
            return customRewriteContextResolver;
        }
        return new DefaultRewriteContextResolver();
    }

    private void configureHstLinkCreator(final DefaultHstLinkCreator hstLinkCreator, final ComponentManager websiteComponentManager) {
        final List<String> binaryLocations = websiteComponentManager.getComponent(HstLinkCreator.class.getName() + ".binaryLocations");
        hstLinkCreator.setBinaryLocations(binaryLocations.toArray(new String[binaryLocations.size()]));

        hstLinkCreator.setPageNotFoundPath(websiteComponentManager.getContainerConfiguration()
                .getString("linkrewriting.failed.path", DefaultHstLinkCreator.DEFAULT_PAGE_NOT_FOUND_PATH));

        hstLinkCreator.setRewriteContextResolver(getRewriteContextResolver(websiteComponentManager));

        hstLinkCreator.setLinkProcessor(getHstLinkProcessor(websiteComponentManager));

        final List<ResourceContainer> immutableResourceContainers = getImmutableResoureceContainers(websiteComponentManager);

        final List<LocationResolver> immutableLocationResolvers =
                getImmutableResoureceResolvers(websiteComponentManager, immutableResourceContainers, binaryLocations.toArray(new String[binaryLocations.size()]));

        hstLinkCreator.setLocationResolvers(immutableLocationResolvers);
    }

    private List<LocationResolver> getImmutableResoureceResolvers(final ComponentManager websiteComponentManager,
                                                                  final List<ResourceContainer> resourceContainers,
                                                                  final String[] binaryLocations) {

        List<LocationResolver> locationsResolvers = new ArrayList<>();
        // the spring config id is customResourceResolvers instead of customLocationResolvers
        List<LocationResolver> customLocationResolvers = websiteComponentManager.getComponent("customResourceResolvers");

        locationsResolvers.addAll(customLocationResolvers);

        final HippoResourceLocationResolver hippoResourceLocationResolver = new HippoResourceLocationResolver();
        hippoResourceLocationResolver.setBinaryLocations(binaryLocations);
        hippoResourceLocationResolver.setResourceContainers(resourceContainers);

        locationsResolvers.add(hippoResourceLocationResolver);

        return locationsResolvers;
    }

    private List<ResourceContainer> getImmutableResoureceContainers(final ComponentManager websiteComponentManager) {

        List<ResourceContainer> resourceContainers = new ArrayList<>();
        // first add the custom resourceContainers, after that the fallback built in resource containers
        List<ResourceContainer> customResourceContainers = websiteComponentManager.getComponent("customResourceContainers");
        resourceContainers.addAll(customResourceContainers);

        final HippoGalleryImageSetContainer hippoGalleryImageSetContainer = new HippoGalleryImageSetContainer();
        hippoGalleryImageSetContainer.setPrimaryItem("hippogallery:original");
        hippoGalleryImageSetContainer.setMappings(ImmutableMap.of("hippogallery:thumbnail", "thumbnail"));

        resourceContainers.add(hippoGalleryImageSetContainer);

        final HippoGalleryAssetSet hippoGalleryAssetSet = new HippoGalleryAssetSet();
        hippoGalleryAssetSet.setPrimaryItem("hippogallery:asset");
        hippoGalleryAssetSet.setMappings(ImmutableMap.of());

        resourceContainers.add(hippoGalleryAssetSet);
        resourceContainers.add(new DefaultResourceContainer());
        return ImmutableList.copyOf(resourceContainers);
    }

    private BiPredicate<Session, Channel> configureChannelFilter(final BiPredicate<Session, Channel> builtinFilter) {

        BiPredicate<Session, Channel> compositeFilter = builtinFilter;

        List<BiPredicate<Session, Channel>> customChannelFilters = websiteComponentManager.getComponent("customChannelFilters");

        for (BiPredicate<Session, Channel> customChannelFilter : customChannelFilters) {
            compositeFilter = customChannelFilter.and(customChannelFilter);
        }

        return compositeFilter;
    }
}
