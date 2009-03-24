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
package org.hippoecm.hst.configuration.sitemenu;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * Implementations should return an unmodifiable map for {@link #getSiteMenuConfiguration()} because clients should not
 * be able to modify the configuration
 *
 */
public interface HstSiteMenusConfiguration extends Serializable{
    
    /**
     * Return the {@link HstSite} this <code>HstSiteMap</code> belongs to. 
     * @return the site this <code>HstSiteMap</code> belongs to
     */
    HstSite getSite();
    
    /**
     * Returns the map containing all <code>HstSiteMenuConfiguration</code>'s and an empty map if there are no <code>HstSiteMenuConfiguration</code>'s.
     * <p/>
     * Note: implementation should better return an unmodifiable map to make sure clients cannot modify the map 
     * @return map containing all <code>HstSiteMenuConfiguration</code>'s and an empty map if there are no <code>HstSiteMenuConfiguration</code>'s 
     */
    Map<String, HstSiteMenuConfiguration> getSiteMenuConfigurations();
    
    /**
     * 
     * @param name the name of the {@link SiteMenuConfiguration}
     * @return the {@link SiteMenuConfiguration} with this name and <code>null</code> if does not exist 
     */
    HstSiteMenuConfiguration getSiteMenuConfiguration(String name);
    
    /**
     * Returns all the matching <code>HstSiteMenuItemConfiguration</code>'s for hstSiteMapItemId. Multiple <code>HstSiteMenuItemConfiguration</code>'s 
     * can link to the same {@link HstSiteMapItem}, and it is up to the implementation how to handle this. If no <code>HstSiteMenuItemConfiguration</code>
     * at all matches, a empty list must be returned
     * @param hstSiteMapItemId the id of the {@link HstSiteMapItem} returned by {@link HstSiteMapItem#getId()} 
     * @return All matching <code>HstSiteMenuItemConfiguration</code>'s for the hstSiteMapItemId and an empty list if none matches. 
     * Preferrably return an unmodifiable list such that clients cannot modify configuration
     */
    List<HstSiteMenuItemConfiguration> getItemsBySiteMapItemId(String hstSiteMapItemId);
}
