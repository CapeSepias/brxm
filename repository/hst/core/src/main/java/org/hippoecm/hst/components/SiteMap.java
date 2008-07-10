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
package org.hippoecm.hst.components;

import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.jcr.JCRConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Site map component that scans for nodes in a certain repository location and
 * builds a site map item structure, with each item having separate document and
 * folder lists.
 *
 * Per location, a site map object is kept in session.
 */
public class SiteMap {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger logger = LoggerFactory.getLogger(SiteMap.class);

    private final SiteMapItem baseItem;

    /**
     * Get site map object lazily from session.
     *
     * @param session the HTTP session
     * @param context the current context object
     * @param location path in the repository from where to generate a site map,
     *          relative to the given context
     * @param excludedDocumentNames optional names of documents that are not
     *      included in the site map
     * @param documentLabelProperties optional properties for documents to set
     *      as label for site map items
     */
    public static SiteMap getSiteMap(final HttpSession session, final Context context, final String location,
            final String[] excludedDocumentNames, final String[] documentLabelProperties) {

        // location starting with /: relative to the base location, else 
        // relative to the complete location
        String loc;
        if (location.startsWith("/")) {
            loc = context.getBaseLocation() + location;
        }
        else {
            loc = context.getLocation();
            loc = loc.endsWith("/") ? loc + location : loc + "/" + location;
        }

        
        SiteMap siteMap = (SiteMap) session.getAttribute(SiteMap.class.getName() + "." + location);

        if (siteMap == null) {
            siteMap = new SiteMap(session, context, loc, excludedDocumentNames, documentLabelProperties);
            session.setAttribute(SiteMap.class.getName() + "." + location, siteMap);
        }

        return siteMap;
    }

    /**
     * Constructor.
     */
    public SiteMap(final HttpSession session, final Context context, final String location,
                final String[] excludedDocumentNames, final String[] documentLabelProperties) {
        super();

        this.baseItem = createBaseItem(session, context, location, excludedDocumentNames, documentLabelProperties);
    }

    @SuppressWarnings("unchecked")
    public List<SiteMapItem> getDocuments() {
        return (baseItem != null) ? baseItem.getDocuments() : Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    public List<SiteMapItem> getFolders() {
        return (baseItem != null) ? baseItem.getFolders() : Collections.EMPTY_LIST;
    }

    private SiteMapItem createBaseItem(final HttpSession session, final Context context, final String location,
            final String[] excludedDocumentNames, final String[] documentLabelProperties) {

        Session jcrSession = JCRConnector.getJCRSession(session);

        if (jcrSession == null) {
            throw new IllegalStateException("No JCR session to repository");
        }

        String path = location;
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        try {
            if (!jcrSession.getRootNode().hasNode(path)) {
                logger.error("Cannot find node by location " + location);
                return null;
            }

            // set base item level at -1 so the first level of items to be
            // gotten is 0, similar to the Menu component
            return new SiteMapItem(session, context, 
                        jcrSession.getRootNode().getNode(path), -1/*level*/,
                        excludedDocumentNames, documentLabelProperties);
        }
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }
}
