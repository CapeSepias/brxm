/*
 * Copyright 2007-2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;

/**
 * Filter that creates a context available for expression language.  
 */
public class ContextFilter implements Filter {

    // private static final Logger logger = LoggerFactory.getLogger(ContextFilter.class);

    public static final String ATTRIBUTE_NAME = ContextFilter.class.getName() + ".ATTRIBUTE_NAME";
    public static final String REPOSITORY_BASE_LOCATION = ContextFilter.class.getName() + ".REPOSITORY_BASE_LOCATION";

    private String attributeName = "context";
    String repositoryBaseLocation = "/";

    private String[] ignoreExtensions = new String[] { "ico", "gif", "jpg", "jpeg", "svg", "png", "css", "js" };
    private final List<String> ignoreExtensionsList = new ArrayList<String>();

    /**
     * Constructor
     */
    public ContextFilter() {
        super();
    }

    // from interface
    public void init(FilterConfig filterConfig) throws ServletException {

        // get attributeName
        String param = filterConfig.getInitParameter("attributeName");
        if (param != null && !param.trim().equals("")) {
            this.attributeName = param.trim();
        }

        // get repositoryBaseLocation
        param = filterConfig.getInitParameter("repositoryBaseLocation");
        if (param != null && !param.trim().equals("")) {
            this.repositoryBaseLocation = param;
        }

        // get ignoreExtensions
        param = filterConfig.getInitParameter("ignoreExtensions");
        if (param != null && !param.trim().equals("")) {
            this.ignoreExtensions = param.split(",");
        }

        // convert extensions to list for easy 'contains' access
        for (int i = 0; i < ignoreExtensions.length; i++) {
            String extension = ignoreExtensions[i].trim();
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            ignoreExtensionsList.add(extension);
        }
    }

    // from interface
    public void destroy() {
    }

    // from interface
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        storeSessionAttributes(req.getSession());

        // check extensions
        String servletPath = req.getServletPath();

        if (servletPath.lastIndexOf(".") >= 0) {
            // images may be retrieved with the BinariesServlet        
            String extension = servletPath.substring(servletPath.lastIndexOf("."));

            if (ignoreExtensionsList.contains(extension)) {
                filterChain.doFilter(req, res);
                return;
            }
        }

        // create context
        Session jcrSession = JCRConnector.getJCRSession(req.getSession());
        Context context = createContext(jcrSession, req);

        req.setAttribute(attributeName, context);

        if (callFilterChain(context, req, res)) {
            filterChain.doFilter(req, res);
        }
    }

    /**
     * A hook for subclasses to forward or redirect and not calling doFilter. 
     */
    boolean callFilterChain(final Context context, final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        return true;
    }

    /**
     * Create a (initial) context object. 
     */
    Context createContext(Session jcrSession, HttpServletRequest request) {
        return new Context(jcrSession, request.getContextPath(), this.repositoryBaseLocation);
    }

    /*
     * Save some data in session for use by tags or BinariesServlet;
     * don't do it lazily as the user might switch from 'live' to 'preview' in one session
     */   
    void storeSessionAttributes(HttpSession session) {
        session.setAttribute(ATTRIBUTE_NAME, this.attributeName);
        session.setAttribute(REPOSITORY_BASE_LOCATION, this.repositoryBaseLocation);
    }
}
