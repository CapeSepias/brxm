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
package org.hippoecm.hst.core.linking;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * HstLinkCreator interface for creating {@link HstLink}'s
 */
public interface HstLinkCreator {


    /**
     * Rewrite a jcr uuid to a HstLink wrt its current ResolvedSiteMapItem. 
     * @param uuid the uuid of the node that must be used to link to
     * @param session jcr session 
     * @param resolvedSiteMapItem
     * @return an <code>HstLink</code> instance or <code>null<code> 
     * @deprecated Use {@link #create(String, Session, HstRequestContext)} instead
     */
    @Deprecated
    HstLink create(String uuid, Session session, ResolvedSiteMapItem resolvedSiteMapItem);
    

    /**
     * Rewrite a jcr uuid to a HstLink wrt its current ResolvedSiteMapItem. 
     * @param uuid the uuid of the node that must be used to link to
     * @param session jcr session 
     * @param requestContext
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String uuid, Session session, HstRequestContext requestContext);
    
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem
     * @param node
     * @param resolvedSiteMapItem
     * @return the HstLink for this jcr Node or <code>null</code>
     * @deprecated Use {@link #create(Node, HstRequestContext)} instead
     */
    @Deprecated
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem
     * @param node
     * @param requestContext
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, HstRequestContext requestContext);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem and preferredItem. When <code>preferredItem</code> is not <code>null</code>, the link is tried to be rewritten to 
     * one of the descendants (including itself) of the preferred {@link HstSiteMapItem}. When <code>preferredItem</code> is <code>null</code>, a link is created against the entire sitemap item tree. When there cannot be created an HstLink to a descendant HstSiteMapItem 
     * or self, then:
     * 
     * <ol>
     *  <li>when <code>fallback = true</code>, a fallback to {@link #create(Node, ResolvedSiteMapItem)} is done</li>
     *  <li>when <code>fallback = false</code>, dependent on the implementation some error HstLink or <code>null</code> can be returned</li>
     * </ol>
     * <p>
     * This method returns an {@link HstLink} that takes the current URL into account, but does compute the link with respect to the physical (canonical) location
     * of the jcr Node. <b>If</b> you need a {@link HstLink} within the context of the possible virtual jcr Node (for example in case of in context showing documents in faceted navigation), use
     * {@link #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean, boolean)} with <code>navigationStateful = true</code>
     * </p>
     * @see #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean, boolean) 
     * @param node the jcr node
     * @param resolvedSiteMapItem the current resolved sitemap item
     * @param preferredItem if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
     * @return the HstLink for this jcr Node or <code>null</code>
     * @deprecated use {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean)} instead
     */
    @Deprecated
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem, boolean fallback);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current HstRequestContext and preferredItem. When <code>preferredItem</code> is not <code>null</code>, the link is tried to be rewritten to 
     * one of the descendants (including itself) of the preferred {@link HstSiteMapItem}. When <code>preferredItem</code> is <code>null</code>, a link is created against the entire sitemap item tree. When there cannot be created an HstLink to a descendant HstSiteMapItem 
     * or self, then:
     * 
     * <ol>
     *  <li>when <code>fallback = true</code>, a fallback to {@link #create(Node, HstRequestContext)} is done</li>
     *  <li>when <code>fallback = false</code>, dependent on the implementation some error HstLink or <code>null</code> can be returned</li>
     * </ol>
     * <p>
     * This method returns an {@link HstLink} that takes the current URL into account, but does compute the link with respect to the physical (canonical) location
     * of the jcr Node. <b>If</b> you need a {@link HstLink} within the context of the possible virtual jcr Node (for example in case of in context showing documents in faceted navigation), use
     * {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean, boolean)} with <code>navigationStateful = true</code>
     * </p>
     * @see #create(Node, HstRequestContext, HstSiteMapItem, boolean, boolean) 
     * @param node the jcr node
     * @param requestContext the current requestContext
     * @param preferredItem if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
      * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem, boolean fallback);
    
    /**
     * <p>
     * This method creates the same {@link HstLink} as {@link #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean)} when <code>navigationStateful = false</code>. When <code>navigationStateful = true</code>, 
     * the link that is created is with respect to the jcr Node <code>node</code>, even if this node is a virtual location. This is different then {@link #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean)}: that
     * method always first tries to find the canonical location of the jcr Node before it is creating a link for the node. 
     * </p>
     * 
     * <p>
     * <b>Expert:</b> Note there is a difference between context relative with respect to the current URL and with respect to the current jcr Node. <b>Default</b>, links in the HST are
     * created always taking into account the current URL (thus context aware linking) unless you call {@link #createCanonical(Node, ResolvedSiteMapItem)} or {@link #createCanonical(Node, ResolvedSiteMapItem, HstSiteMapItem)}. Also,
     * <b>default</b>, it always (unless there is no) takes the <i>canonical</i> location of the jcr Node. Thus, multiple virtual versions of the same physical Node, result in the same HstLink. Only when having <code>navigationStateful = true</code>, 
     * also the jcr Node is context relative, and thus multiple virtual versions of the same jcr Node can result in multiple links. This is interesting for example in 
     * faceted navigation views, where you want 'in context' documents to be shown.
     * </p>
     * @see #create(Node, ResolvedSiteMapItem, HstSiteMapItem, boolean)
     * @param node the jcr node 
     * @param resolvedSiteMapItem  the current resolved sitemap item
     * @param preferredItem  if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
     * @param navigationStateful value true or false
     * @return  the HstLink for this jcr Node or <code>null</code>
     * @deprecated use {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean, boolean)} instead
     */
    @Deprecated
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem, boolean fallback, boolean navigationStateful);
    
    /**
     * <p>
     * This method creates the same {@link HstLink} as {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean)} when <code>navigationStateful = false</code>. When <code>navigationStateful = true</code>, 
     * the link that is created is with respect to the jcr Node <code>node</code>, even if this node is a virtual location. This is different then {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean)}: that
     * method always first tries to find the canonical location of the jcr Node before it is creating a link for the node. 
     * </p>
     * 
     * <p>
     * <b>Expert:</b> Note there is a difference between context relative with respect to the current URL and with respect to the current jcr Node. <b>Default</b>, links in the HST are
     * created always taking into account the current URL (thus context aware linking) unless you call {@link #createCanonical(Node, HstRequestContext)} or {@link #createCanonical(Node, HstRequestContext, HstSiteMapItem)}. Also,
     * <b>default</b>, it always (unless there is no) takes the <i>canonical</i> location of the jcr Node. Thus, multiple virtual versions of the same physical Node, result in the same HstLink. Only when having <code>navigationStateful = true</code>, 
     * also the jcr Node is context relative, and thus multiple virtual versions of the same jcr Node can result in multiple links. This is interesting for example in 
     * faceted navigation views, where you want 'in context' documents to be shown.
     * </p>
     * @see #create(Node, HstRequestContext, HstSiteMapItem, boolean)
     * @param node the jcr node 
     * @param HstRequestContext  the current requestContext
     * @param preferredItem  if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
     * @param navigationStateful value true or false
     * @return  the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem, boolean fallback, boolean navigationStateful);
    

    /**
     * This creates a canonical HstLink: regardless the context, one and the same jcr Node is garantueed to return the same HstLink. This is
     * useful when showing one and the same content via multiple urls, for example in faceted navigation. Search engines can better index your
     * website when defining a canonical location for duplicate contents: See 
     * <a href="http://googlewebmastercentral.blogspot.com/2009/02/specify-your-canonical.html">specify-your-canonical</a> for more info on this subject.
     * 
     * @param node
     * @param resolvedSiteMapItem
     * @return the HstLink for this jcr Node or <code>null</code>
     * @deprecated use {@link #createCanonical(Node, HstRequestContext)} instead
     */
    @Deprecated
    HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    

    /**
     * This creates a canonical HstLink: regardless the current requestContext, one and the same jcr Node is garantueed to return the same HstLink. This is
     * useful when showing one and the same content via multiple urls, for example in faceted navigation. Search engines can better index your
     * website when defining a canonical location for duplicate contents: See 
     * <a href="http://googlewebmastercentral.blogspot.com/2009/02/specify-your-canonical.html">specify-your-canonical</a> for more info on this subject.
     * 
     * @param node
     * @param HstRequestContext
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, HstRequestContext requestContext);
    
    /**
     * @see {@link #createCanonical(Node, ResolvedSiteMapItem)}.
     * When specifying a preferredItem, we try to create a canonical link wrt this preferredItem. If the link cannot be created for this preferredItem,
     * a fallback to {@link #createCanonical(Node, ResolvedSiteMapItem)} without preferredItem is done.
     * 
     * @param node
     * @param resolvedSiteMapItem
     * @param preferredItem if <code>null</code>, a fallback to {@link #createCanonical(Node, ResolvedSiteMapItem)} is done
     * @return the HstLink for this jcr Node or <code>null</code>
     * @deprecated use {@link #createCanonical(Node, HstRequestContext, HstSiteMapItem)} instead
     */
    @Deprecated
    HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem);
    
    /**
     * @see {@link #createCanonical(Node, ResolvedSiteMapItem)}.
     * When specifying a preferredItem, we try to create a canonical link wrt this preferredItem. If the link cannot be created for this preferredItem,
     * a fallback to {@link #createCanonical(Node, HstRequestContext)} without preferredItem is done.
     * 
     * @param node
     * @param HstRequestContext
     * @param preferredItem if <code>null</code>, a fallback to {@link #createCanonical(Node, HstRequestContext)} is done
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem);
    
    
    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} with respect to the <code>hstSite</code>. Note that this HstLink creation does only take into account the
     * <code>hstSite</code>.
     * This might be a different one then the one of the current request context, reflected in the {@link ResolvedSiteMapItem}, 
     * for example in the method {@link #create(Node, ResolvedSiteMapItem)}. 
     * If the <code>hstSite</code> cannot be used to create a HstLink for the jcr <code>node</code>, because the <code>node</code> belongs
     * to a different (sub)site, <code>null</code> is returned. </p>
     * <p>note: if an link is returned, this is always the canonical link, also see {@link #createCanonical(Node, ResolvedSiteMapItem)}</p>
     * @param node the jcr node for that should be translated into a HstLink
     * @param hstSite the (sub)site for which the hstLink should be created for
     * @return the {@link HstLink} for the jcr <code>node</code> and the <code>hstSite</code> or <code>null</code> when no link for the node can be made in the <code>hstSite</code>
     * @deprecated Use {@link #create(Node, SiteMount))}
     */
    @Deprecated
    HstLink create(Node node, HstSite hstSite);
    
    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} with respect to the <code>siteMount</code>. Note that this HstLink creation does only take into account the
     * <code>siteMount</code> and not the current context.
     * This might be a different one then the one of the current request context, reflected in the {@link ResolvedSiteMapItem}, 
     * for example in the method {@link #create(Node, ResolvedSiteMapItem)}. 
     * If the <code>siteMount</code> cannot be used to create a HstLink for the jcr <code>node</code>, because the <code>node</code> belongs
     * to a different (sub)site, <code>null</code> is returned. </p>
     * <p>note: if an link is returned, this is always the canonical link, also see {@link #createCanonical(Node, ResolvedSiteMapItem)}</p>
     * @param node the jcr node for that should be translated into a HstLink
     * @param siteMount the (sub)site for which the hstLink should be created for
     * @return the {@link HstLink} for the jcr <code>node</code> and the <code>hstSite</code> or <code>null</code> when no link for the node can be made in the <code>siteMount</code>
     */
    HstLink create(Node node, SiteMount siteMount);

    /**
     * 
     * @param bean
     * @param hstRequestContext
     * @return a HstLink for <code>bean</code> and the <code>hstRequestContext</code> or <code>null</code> when no link for the node can be made
     */
    HstLink create(HippoBean bean, HstRequestContext hstRequestContext);
   
    /**
     * Regardless the current context, create a HstLink to the HstSiteMapItem that you use as argument. This is only possible if the sitemap item does not
     * contain any ancestor including itself with a wildcard, because the link is ambiguous in that case. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param toHstSiteMapItem the {@link HstSiteMapItem} to link to
     * @return an <code>HstLink</code> instance or <code>null<code> 
     * @deprecated Use {@link #create(String, SiteMount)} instead
     */
    @Deprecated
    HstLink create(HstSiteMapItem toHstSiteMapItem);

    /**
     * Regardless the current context, create a HstLink for the <code>path</code> and <code>hstSite</code>
     * @param path the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     * @deprecated Use {@link #create(String, SiteMount)} instead
     */
    @Deprecated
    HstLink create(String path, HstSite hstSite);
    

    /**
     * Regardless the current context, create a HstLink for the <code>path</code> and <code>siteMount</code>
     * @param path the path to the sitemap item
     * @param siteMount the SiteMount the path should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, SiteMount siteMount);
    
    /**
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @param containerResource whether it is a static link, for example for css/js
     * @return an <code>HstLink</code> instance or <code>null<code> 
     * @deprecated Use {@link #create(String, SiteMount, boolean)}
     */
    @Deprecated
    HstLink create(String path, HstSite hstSite, boolean containerResource);
    
    /**
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param SiteMount the SiteMount for which the link should be created
     * @param containerResource whether it is a static link, for example for css/js
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, SiteMount siteMount, boolean containerResource);
    
    /**
     * @deprecate Use {@link #create(String, SiteMount)} instead
     */
    @Deprecated
    HstLink create(HstSite hstSite, String toSiteMapItemId);
    
    /**
     * Binaries frequently have a different linkrewriting mechanism. If this method returns <code>true</code> the location is a
     * binary location. 
     * @param path
     * @return <code>true</code> when the path points to a binary location
     */
    boolean isBinaryLocation(String path);
    
    /**
     * @return The prefix that is used for binary locations. The returned binaries prefix is relative to <code>/</code> and 
     * does not include the <code>/</code> itself. If no binaries prefix is configured, <code>""</code> will be returned
     */ 
    String getBinariesPrefix();
    
    /**
     * @return the list of location resolvers, primarily used for resolving custom binary locations 
     */
    List<LocationResolver> getLocationResolvers();
}
