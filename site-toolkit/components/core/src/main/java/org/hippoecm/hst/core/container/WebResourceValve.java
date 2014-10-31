/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.webresources.CacheableWebResource;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.WebResourceUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webresources.Binary;
import org.onehippo.cms7.services.webresources.WebResource;
import org.onehippo.cms7.services.webresources.WebResourceBundle;
import org.onehippo.cms7.services.webresources.WebResourceException;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebResourceValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(WebResourceValve.class);
    private static final long ONE_YEAR_SECONDS = TimeUnit.SECONDS.convert(365L, TimeUnit.DAYS);
    private static final long ONE_YEAR_MILLISECONDS = TimeUnit.MILLISECONDS.convert(ONE_YEAR_SECONDS, TimeUnit.SECONDS);

    private HstCache webResourceCache;

    public void setWebResourceCache(final HstCache webResourceCache) {
        this.webResourceCache = webResourceCache;
    }

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final HttpServletResponse response = context.getServletResponse();

        try {
            final WebResource webResource = getWebResource(requestContext);
            setHeaders(response, webResource);
            writeWebResource(response, webResource);
        } catch (WebResourceException e) {
            final HttpServletRequest request = context.getServletRequest();
            if (log.isDebugEnabled()) {
                log.info("Cannot serve binary '{}'", request.getPathInfo(), e);
            } else {
                log.info("Cannot serve binary '{}', cause: '{}'", request.getPathInfo(), e.toString());
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (Exception e) {
            throw new ContainerException(e);
        }

        context.invokeNext();
    }

    private WebResource getWebResource(final HstRequestContext requestContext) throws RepositoryException, ContainerException, IOException, WebResourceException {
        final WebResourcesService service = getWebResourcesService();

        final Session session = requestContext.getSession();
        final String bundleName = WebResourceUtils.getBundleName(requestContext);
        log.debug("Trying to get web resource bundle '{}' with session user '{}'", bundleName, session.getUserID());
        final WebResourceBundle webResourceBundle = service.getJcrWebResourceBundle(session, bundleName);

        final String contentPath = "/" + requestContext.getResolvedSiteMapItem().getRelativeContentPath();
        final String version = getVersion(requestContext, contentPath);
        final String cacheKey = createCacheKey(bundleName, contentPath, version);

        final CacheElement cacheElement = webResourceCache.get(cacheKey);

        if (cacheElement == null) {
            return cacheWebResource(webResourceBundle, contentPath, version, cacheKey);
        } else {
            return (CacheableWebResource) cacheElement.getContent();
        }
    }

    private WebResourcesService getWebResourcesService() throws ContainerException {
        WebResourcesService service = HippoServiceRegistry.getService(WebResourcesService.class);
        if (service == null) {
            log.error("Missing service for '{}'. Cannot serve webresource.", WebResourcesService.class.getName());
            throw new ContainerException("Missing service for '" + WebResourcesService.class.getName() + "'. Cannot serve webresource.");
        }
        return service;
    }

    private String getVersion(final HstRequestContext requestContext, final String contentPath) throws WebResourceException {
        final String version = requestContext.getResolvedSiteMapItem().getParameter("version");
        if (version == null) {
            String msg = String.format("Cannot serve web resource '%s' for mount '%s' because sitemap item" +
                            "'%s' does not contain version param.", contentPath,
                    requestContext.getResolvedMount().getMount(),
                    requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getQualifiedId());
            throw new WebResourceException(msg);
        }
        return version;
    }

    private String createCacheKey(final String bundleName, final String contentPath, final String version) {
        final StringBuilder cacheKeyBuilder = new StringBuilder(bundleName).append('\uFFFF');
        cacheKeyBuilder.append(version).append(contentPath);
        return cacheKeyBuilder.toString();
    }

    private WebResource cacheWebResource(final WebResourceBundle webResourceBundle, final String contentPath, final String version, final String cacheKey) throws IOException {
        try {
            final WebResource webResource = getWebResourceFromBundle(webResourceBundle, contentPath, version);
            final CacheableWebResource cacheableWebResource = new CacheableWebResource(webResource);
            final CacheElement element = webResourceCache.createElement(cacheKey, cacheableWebResource);
            webResourceCache.put(element);
            return cacheableWebResource;
        } catch (Exception e) {
            clearBlockingLock(cacheKey);
            throw e;
        }
    }

    private WebResource getWebResourceFromBundle(final WebResourceBundle webResourceBundle, final String contentPath, final String version) throws IOException {
        if (version.equals(webResourceBundle.getAntiCacheValue())) {
            return webResourceBundle.get(contentPath);
        } else {
            return webResourceBundle.get(contentPath, version);
        }
    }

    /**
     * Blocking EhCache creates a lock during a #get that returns null. Hence if after the get the creation for the web
     * resource fails, we need to clear the lock for the cacheKey
     */
    private void clearBlockingLock(final String cacheKey) {
        log.debug("Clear lock for {}", cacheKey);
        final CacheElement element = webResourceCache.createElement(cacheKey, null);
        webResourceCache.put(element);
    }

    private static void setHeaders(final HttpServletResponse response, final WebResource webResource) throws RepositoryException {
        // no need for ETag since expires 1 year
        response.setHeader("Content-Length", Long.toString(webResource.getBinary().getSize()));
        response.setContentType(webResource.getMimeType());
        // one year ahead max, see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
        response.setDateHeader("Expires", ONE_YEAR_MILLISECONDS + System.currentTimeMillis());
        response.setHeader("Cache-Control", "max-age=" + ONE_YEAR_SECONDS);
    }

    private static void writeWebResource(final HttpServletResponse response, final WebResource webResource) throws IOException {
        final Binary binary = webResource.getBinary();
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(binary.getStream(), outputStream);
            outputStream.flush();
        }
    }

}
