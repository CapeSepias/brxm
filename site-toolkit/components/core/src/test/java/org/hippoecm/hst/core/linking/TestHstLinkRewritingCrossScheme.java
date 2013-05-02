/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;


import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TestHstLinkRewritingCrossScheme extends AbstractBeanTestCase {

    private HstManager hstManager;
    private HstURLFactory hstURLFactory;
    private ObjectConverter objectConverter;
    private HstLinkCreator linkCreator;
    private HstSiteMapMatcher siteMapMatcher;
    private Repository repository;
    private Credentials credentials;

    private final static String HTTP_SCHEME = "http";
    private final static String HTTPS_SCHEME = "https";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        this.objectConverter = getObjectConverter();
        this.linkCreator = getComponent(HstLinkCreator.class.getName());
        this.repository = getComponent(Repository.class.getName());
        this.credentials = getComponent(Credentials.class.getName() + ".hstconfigreader");

    }

    private class SiteMapItemReference {
        String jcrPath;
        String originalScheme;

        SiteMapItemReference(String jcrPath) {
            this.jcrPath = jcrPath;
        }
    }

    @Test
    public void testHttpRequestToHttpsLinks() throws Exception {
        List<SiteMapItemReference> siteMapItemsToSetHttpsScheme = new ArrayList<SiteMapItemReference>();
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_.html"));
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_any_.html"));
        try {
            makeSiteMapItemsHttps(siteMapItemsToSetHttpsScheme);

            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("http","localhost","/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsArticleBean = obm.getObject("/unittestcontent/documents/unittestproject/News/2009/April/AprilNewsArticle");
            HstLink newsArticleLink = linkCreator.create((HippoBean)newsArticleBean, requestContext);

            assertEquals("news/2009/April/AprilNewsArticle.html", newsArticleLink.getPath());
            final String newsArticlesHttpsURL = newsArticleLink.toUrlForm(requestContext, false);
            final String newsArticlesHttpsURLFullyQualified = newsArticleLink.toUrlForm(requestContext, true);

            assertEquals("scheme for news articles should be https and thus fully qualified links expected when current request is http.",
                    HTTPS_SCHEME +"://localhost/site/news/2009/April/AprilNewsArticle.html", newsArticlesHttpsURL);
            assertTrue("scheme for news articles should be https and thus fully qualified links even when not explicitly set in toUrlForm",
                    newsArticlesHttpsURLFullyQualified.equals(newsArticlesHttpsURL));

        } finally {
            revertSiteMapItemsToValueBefore(siteMapItemsToSetHttpsScheme);
        }
    }


    @Test
    public void testHttpsRequestToHttpsLinks() throws Exception {
        List<SiteMapItemReference> siteMapItemsToSetHttpsScheme = new ArrayList<SiteMapItemReference>();
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_.html"));
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_any_.html"));
        try {
            makeSiteMapItemsHttps(siteMapItemsToSetHttpsScheme);

            // note below https scheme for current request!!
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("https","localhost","/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object newsArticleBean = obm.getObject("/unittestcontent/documents/unittestproject/News/2009/April/AprilNewsArticle");
            HstLink newsArticleLink = linkCreator.create((HippoBean)newsArticleBean, requestContext);

            assertEquals("news/2009/April/AprilNewsArticle.html", newsArticleLink.getPath());
            final String newsArticlesHttpsURL = newsArticleLink.toUrlForm(requestContext, false);
            final String newsArticlesHttpsURLFullyQualified = newsArticleLink.toUrlForm(requestContext, true);

            assertEquals("link for news articles should be not fully qualified https.",
                    "/site/news/2009/April/AprilNewsArticle.html", newsArticlesHttpsURL);

            assertEquals("fully qualified link for news articles should start with https.",
                    HTTPS_SCHEME +"://localhost/site/news/2009/April/AprilNewsArticle.html", newsArticlesHttpsURLFullyQualified);

        } finally {
            revertSiteMapItemsToValueBefore(siteMapItemsToSetHttpsScheme);
        }
    }

    @Test
    public void testHttpsRequestToHttpLinks() throws Exception {
        List<SiteMapItemReference> siteMapItemsToSetHttpsScheme = new ArrayList<SiteMapItemReference>();
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_.html"));
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_any_.html"));
        try {
            makeSiteMapItemsHttps(siteMapItemsToSetHttpsScheme);

            // note below https scheme for current request!!
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("https","localhost","/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homepageBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homepageLink = linkCreator.create((HippoBean)homepageBean, requestContext);

            assertEquals("", homepageLink.getPath());
            final String homepageHttpURL = homepageLink.toUrlForm(requestContext, false);
            final String homepageHttpURLFullyQualified = homepageLink.toUrlForm(requestContext, true);

            assertEquals("link for homepage should be fully qualified http link as current request is https.",
                    HTTP_SCHEME +"://localhost/site", homepageHttpURL);

            assertTrue("scheme for homepage should be http and thus fully qualified links even when not explicitly set in toUrlForm",
                    homepageHttpURLFullyQualified.equals(homepageHttpURL));

        } finally {
            revertSiteMapItemsToValueBefore(siteMapItemsToSetHttpsScheme);
        }
    }

    @Test
    public void testCrossMountHttpToHttpsLinks() throws Exception {
        List<SiteMapItemReference> siteMapItemsToSetHttpsScheme = new ArrayList<SiteMapItemReference>();
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_.html"));
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_any_.html"));
        try {
            makeSiteMapItemsHttps(siteMapItemsToSetHttpsScheme);

            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("http","localhost","/subsite/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);

            {
                Object newsArticleBean = obm.getObject("/unittestcontent/documents/unittestproject/News/2009/April/AprilNewsArticle");
                HstLink newsArticleLink = linkCreator.create(((HippoBean)newsArticleBean).getNode(), requestContext, "root");

               //  /mycontextpath/examplecontextpathonly
                assertEquals("news/2009/April/AprilNewsArticle.html", newsArticleLink.getPath());
                final String newsArticlesHttpsURL = newsArticleLink.toUrlForm(requestContext, false);
                final String newsArticlesHttpsURLFullyQualified = newsArticleLink.toUrlForm(requestContext, true);

                assertEquals("scheme for news articles should be https",
                        HTTPS_SCHEME +"://localhost/site/news/2009/April/AprilNewsArticle.html", newsArticlesHttpsURL);
                assertTrue("scheme for news articles should be https",
                        newsArticlesHttpsURLFullyQualified.equals(newsArticlesHttpsURL));
            }
            {
                Object homepageBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
                HstLink homepageLink = linkCreator.create(((HippoBean)homepageBean).getNode(), requestContext, "root");

                assertEquals("", homepageLink.getPath());
                final String homepageHttpURL = homepageLink.toUrlForm(requestContext, false);
                final String homepageHttpURLFullyQualified = homepageLink.toUrlForm(requestContext, true);

                assertEquals("link for homepage should be fully qualified http because cross mount on different port.",
                        "/site", homepageHttpURL);
                assertEquals("link for homepage should be fully qualified http because cross mount on different port.",
                        HTTP_SCHEME +"://localhost/site", homepageHttpURLFullyQualified);

            }

            // NOW TEST THE SAME AS ABOVE WITH DIFFERENT mount alias (examplecontextpathonly) : That mount can also represent the documents

            {
                Object newsArticleBean = obm.getObject("/unittestcontent/documents/unittestproject/News/2009/April/AprilNewsArticle");
                HstLink newsArticleLink = linkCreator.create(((HippoBean)newsArticleBean).getNode(), requestContext, "examplecontextpathonly");

                //  /mycontextpath/examplecontextpathonly
                assertEquals("news/2009/April/AprilNewsArticle.html", newsArticleLink.getPath());
                final String newsArticlesHttpsURL = newsArticleLink.toUrlForm(requestContext, false);
                final String newsArticlesHttpsURLFullyQualified = newsArticleLink.toUrlForm(requestContext, true);

                assertEquals("scheme for news articles should be https",
                        HTTPS_SCHEME +"://localhost/mycontextpath/examplecontextpathonly/news/2009/April/AprilNewsArticle.html", newsArticlesHttpsURL);
                assertTrue("scheme for news articles should be https",
                        newsArticlesHttpsURLFullyQualified.equals(newsArticlesHttpsURL));
            }
            {
                Object homepageBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
                HstLink homepageLink = linkCreator.create(((HippoBean)homepageBean).getNode(), requestContext, "examplecontextpathonly");

                assertEquals("", homepageLink.getPath());
                final String homepageHttpURL = homepageLink.toUrlForm(requestContext, false);
                final String homepageHttpURLFullyQualified = homepageLink.toUrlForm(requestContext, true);

                assertEquals("link for homepage should be fully qualified http because cross mount on different port.",
                        "/mycontextpath/examplecontextpathonly", homepageHttpURL);
                assertEquals("link for homepage should be fully qualified http because cross mount on different port.",
                        HTTP_SCHEME +"://localhost/mycontextpath/examplecontextpathonly", homepageHttpURLFullyQualified);

            }

        } finally {
            revertSiteMapItemsToValueBefore(siteMapItemsToSetHttpsScheme);
        }
    }


    @Test
    public void testCrossHostHttpToHttpsLinks() throws Exception {
        List<SiteMapItemReference> siteMapItemsToSetHttpsScheme = new ArrayList<SiteMapItemReference>();
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_default_.html"));
        siteMapItemsToSetHttpsScheme.add(new SiteMapItemReference("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news/_any_.html"));
        try {
            makeSiteMapItemsHttps(siteMapItemsToSetHttpsScheme);

            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("http","sub.unit.test","/home");
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);

            Object newsArticleBean = obm.getObject("/unittestcontent/documents/unittestproject/News/2009/April/AprilNewsArticle");
            HstLink newsArticleLink = linkCreator.create(((HippoBean)newsArticleBean).getNode(), requestContext, "root");

            //  /mycontextpath/examplecontextpathonly
            assertEquals("news/2009/April/AprilNewsArticle.html", newsArticleLink.getPath());
            final String newsArticlesHttpsURL = newsArticleLink.toUrlForm(requestContext, false);
            final String newsArticlesHttpsURLFullyQualified = newsArticleLink.toUrlForm(requestContext, true);

            assertEquals("scheme for news articles should be https",
                    HTTPS_SCHEME +"://www.unit.test/site/news/2009/April/AprilNewsArticle.html", newsArticlesHttpsURL);
            assertTrue("scheme for news articles should be https",
                    newsArticlesHttpsURLFullyQualified.equals(newsArticlesHttpsURL));


        } finally {
            revertSiteMapItemsToValueBefore(siteMapItemsToSetHttpsScheme);
        }
    }

    private void makeSiteMapItemsHttps(final List<SiteMapItemReference> siteMapItemsToSetHttpsScheme) throws Exception {
        Session session = null;
        try {
            session = repository.login(credentials);
            for (SiteMapItemReference siteMapItemReference : siteMapItemsToSetHttpsScheme) {
                Node jcrNode = session.getNode(siteMapItemReference.jcrPath);
                if (jcrNode.hasProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME)) {
                    siteMapItemReference.originalScheme = jcrNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME).getString();
                }
                jcrNode.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME, "https");
            }
            session.save();
        } finally {
            if (session != null) {
               session.logout();
            }
        }
    }

    private void revertSiteMapItemsToValueBefore(final List<SiteMapItemReference> siteMapItemsToSetHttpsScheme) throws Exception {
        Session session = null;
        try {
            session = repository.login(credentials);
            for (SiteMapItemReference siteMapItemReference : siteMapItemsToSetHttpsScheme) {
                Node jcrNode = session.getNode(siteMapItemReference.jcrPath);
                if (siteMapItemReference.originalScheme == null) {
                    if (jcrNode.hasProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME)) {
                        jcrNode.getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME).remove();
                    }
                } else {
                    jcrNode.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME, siteMapItemReference.originalScheme);
                }
            }
            session.save();
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    public HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(String scheme, String hostAndPort, String requestURI) throws Exception {
        HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrl(scheme, hostAndPort, requestURI, requestContext);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);
        return requestContext;
    }

    public HstContainerURL createContainerUrl(String scheme, String hostAndPort, String requestURI,
                                              HstMutableRequestContext requestContext) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        requestContext.setServletRequest(request);
        requestContext.setServletResponse(response);
        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            request.setLocalPort(port);
            request.setServerPort(port);
        }
        request.setScheme(scheme);
        request.setServerName(host);
        request.addHeader("Host", hostAndPort);
        request.setContextPath("/site");
        requestURI = "/site" + requestURI;
        request.setRequestURI(requestURI);
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
        return hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
    }

    public ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        return vhosts.matchSiteMapItem(url);
    }


}
