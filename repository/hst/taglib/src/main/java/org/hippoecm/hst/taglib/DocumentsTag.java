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
package org.hippoecm.hst.taglib;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.Context;
import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.URLPathTranslator;
import org.hippoecm.hst.util.PropertyFormatter;

/**
 * Documents Tag showing documents from a certain location in the repository.
 */
public class DocumentsTag extends SimpleTagSupport {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final String PROPERTY_TYPE_LINK = "link";
    protected static final String PROPERTY_TYPE_DATE = "date";
    protected static final String PROPERTY_TYPE_STRING = "string";

    private static final String DEFAULT_CONTEXT_NAME = "context";
    private static final Integer DEFAULT_MAX_DOCUMENTS = new Integer(5);
    private static final String[] DEFAULT_DOCUMENT_EXCLUDE_NAMES = new String[] {"index"};

    private final String KEY_CONTEXT_NAME = getConfigurationKeyPrefix() + ".context.name";
    private final String KEY_LOCATION = getConfigurationKeyPrefix() + ".location";
    private final String KEY_MAX_DOCUMENTS = getConfigurationKeyPrefix() + ".max.documents";
    private final String KEY_DOCUMENT_PROPERTIES = getConfigurationKeyPrefix() + ".document.properties";
    private final String KEY_DOCUMENT_PROPERTY_TYPES = getConfigurationKeyPrefix() + ".document.property.types";
    private final String KEY_DOCUMENT_EXCLUDE_NAMES = getConfigurationKeyPrefix() + ".document.exclude.names";
    private final String KEY_DOCUMENT_VIEWFILE = getConfigurationKeyPrefix() + ".document.viewfile";

    private static final String CSS_CLASS_LIST = "hst-list";
    private static final String CSS_CLASS_DOCUMENT = "hst-document";
    private static final String CSS_CLASS_DOCUMENT_DATE = "hst-document-date";
    private static final String CSS_CLASS_DOCUMENT_LINK = "hst-document-link";
    private static final String CSS_CLASS_DOCUMENT_TEXT = "hst-document-text";

    private String contextName = null;
    private String id = null;
    private String location = null;
    private Integer maxDocuments = null;
    private String[] documentProperties = null;
    private String[] documentPropertyTypes = null;
    private String[] documentExcludeNames = null;
    private String documentViewFile = null;

    /** Setter for the tag attribute 'context'. */
    public void setContext(String contextName) {
        this.contextName = contextName;
    }

    /** Setter for the tag attribute 'id'. */
    public void setId(String id) {
        this.id = id;
    }
    
    /** Setter for the tag attribute 'location'. */
    public void setLocation(String location) {
        this.location = location;
    }

    /** Setter for the tag attribute 'maxDocuments'. */
    public void setMaxDocuments(Integer maxDocuments) {
        this.maxDocuments = maxDocuments;
    }

    // javadoc from super
    public void doTag() throws JspException {

            PageContext pageContext = (PageContext) this.getJspContext();
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

            String contextName = getContextName();

            Context context = (Context) request.getAttribute(contextName);

            if (context == null) {
                throw new JspException("No context found in request by attribute name '" + contextName + "'.");
            }

            try {
                pageContext.getOut().flush();

                String location = getLocation();

                Context documentsContext;
                if ("".equals(location)) {
                    documentsContext = context;
                }
                else {
                    Object obj = context.get(location);
                    if (!(obj instanceof Context)) {
                        throw new JspException("Object gotten from context " + context.getLocation()
                                + " and location " + location + " is not of type Context, it is "
                                + ((obj == null) ? "null" : (obj.getClass().getName() + ", " + obj)));
                    }
                    documentsContext = (Context) obj;
                }

                String docViewFile = getDocumentViewFile();
                if (docViewFile == null) {
                    doOutputDefault(documentsContext, pageContext);
                }
                else {
                    doOutputByIncludes(documentsContext, pageContext, docViewFile);
                }
            }
            catch (ServletException ex) {
                throw new JspException(ex);
            } catch (IOException ex) {
                throw new JspException(ex);
            } finally {
                // always reset the context as the include type output may have overwritten it
                request.setAttribute(contextName, context);
            }
        }

    /** What prefix to use to find configuration. */
    protected String getConfigurationKeyPrefix() {

        String fullClassName = this.getClass().getName();

        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        return className.toLowerCase();
    }

    /** Get the default id. */
    protected String getDefaultId() {
        return "documentslist";
    }

    /** Get a default location where to find documents in the repository. */
    protected String getDefaultLocation() {
        return "/documents";
    }

    /** Get a default list of document properties. */
    protected String[] getDefaultDocumentProperties() {
        return new String[] {"defaultcontent:title",
                             "defaultcontent:date",
                             "defaultcontent:introduction"};
    }

    /** Get a default list of document properties types. */
    protected String[] getDefaultDocumentPropertyTypes() {
        return new String[] {PROPERTY_TYPE_STRING,
                             PROPERTY_TYPE_DATE,
                             PROPERTY_TYPE_STRING};
    }

    private String getContextName() {

        // lazy, or set by setter
        if (this.contextName == null) {

            // second by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            this.contextName = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_CONTEXT_NAME, false/*not required*/);

            // third by default
            if (this.contextName == null) {
                this.contextName = DEFAULT_CONTEXT_NAME;
            }
        }

        return this.contextName;
    }

    private String getId() {

        // lazy, or set by setter (first)
        if (this.id == null) {

            // second by default
            if (this.id == null) {
                this.id = getDefaultId();
            }
        }

        return this.id;
    }

    private String getLocation() {

        // lazy, or set by setter
        if (this.location == null) {

            // second by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            this.location = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_LOCATION, false/*not required*/);

            // third by default
            if (this.location == null) {
                this.location = getDefaultLocation();
            }
        }

        return this.location;
    }

    private Integer getMaxDocuments() {

        // lazy, or set by setter
        if (this.maxDocuments == null) {

            // second by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String maxDocsString = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_MAX_DOCUMENTS, false/*not required*/);
            if (maxDocsString != null) {
                this.maxDocuments = new Integer(maxDocsString);
            }

            // third by default
            if (this.maxDocuments == null) {
                this.maxDocuments = DEFAULT_MAX_DOCUMENTS;
            }
        }

        return this.maxDocuments;
    }

    private String[] getDocumentProperties() {

        // lazy (no setter)
        if (this.documentProperties == null) {

            // first by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String propertiesString = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_DOCUMENT_PROPERTIES, false/*not required*/);
            if (propertiesString != null) {
                this.documentProperties = propertiesString.split(",");
            }

            // second by default
            if (this.documentProperties == null) {
                this.documentProperties = getDefaultDocumentProperties();
            }
        }

        return this.documentProperties;
    }

    private String[] getDocumentPropertyTypes() {

        // lazy (no setter)
        if (this.documentPropertyTypes == null) {

            // first by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String propertyTypesString = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_DOCUMENT_PROPERTY_TYPES, false/*not required*/);
            if (propertyTypesString != null) {
                this.documentPropertyTypes = propertyTypesString.split(",");
            }

            // second by default
            if (this.documentPropertyTypes == null) {
                this.documentPropertyTypes = getDefaultDocumentPropertyTypes();
            }
        }

        return this.documentPropertyTypes;
    }

    private String[] getDocumentExcludeNames() {

        // lazy (no setter)
        if (this.documentExcludeNames == null) {

            // first by configuration
            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();
            String excludeNamesString = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_DOCUMENT_EXCLUDE_NAMES, false/*not required*/);
            if (excludeNamesString != null) {
                this.documentExcludeNames = excludeNamesString.split(",");
            }

            // second by default
            if (this.documentExcludeNames == null) {
                this.documentExcludeNames = DEFAULT_DOCUMENT_EXCLUDE_NAMES;
            }
        }

        return this.documentExcludeNames;
    }

    private String getDocumentViewFile() {

        // lazy (no setter)
        if (this.documentViewFile == null) {

            HttpServletRequest request = (HttpServletRequest) ((PageContext) this.getJspContext()).getRequest();

            // first try key postfixed by .id
            String docViewFile = HSTConfiguration.get(request.getSession().getServletContext(),
                    KEY_DOCUMENT_VIEWFILE + "." + getId(), false/*not required*/);

            
            // then by configuration
            if (docViewFile == null) {
                docViewFile = HSTConfiguration.get(request.getSession().getServletContext(),
                        KEY_DOCUMENT_VIEWFILE, false/*not required*/);
            }
            
            if (docViewFile != null) {
                this.documentViewFile = docViewFile;
            }
        }

        // may return null, the hardcoded view will be output
        return this.documentViewFile;
    }

    /** Output default HTML by configured or default properties */
    @SuppressWarnings("unchecked")
    private void doOutputDefault(Context context, PageContext pageContext) throws IOException, JspException {

        String[] documentProperties = getDocumentProperties();
        String[] documentPropertyTypes = getDocumentPropertyTypes();

        if (documentProperties.length != documentPropertyTypes.length) {
            throw new JspException("Number of document properties is different from the number " +
                    "of document property types: " + documentProperties + " and " + documentPropertyTypes);
        }

        URLPathTranslator urlPathTranslator = null;
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        PropertyFormatter propertyFormatter = new PropertyFormatter(request);

        StringBuffer buffer = new StringBuffer();

        buffer.append("<div id=\"");
        buffer.append(getId());
        buffer.append("\"  class=\"");
        buffer.append(CSS_CLASS_LIST);
        buffer.append("\">\n");

        // loop subcontexts representing handle subnodes
        Iterator documents = context.entrySet().iterator();

        int counter = 0;
        while (documents.hasNext() && (counter < getMaxDocuments().intValue())) {
           Context documentHandle = (Context) documents.next();

            // get first subnode of the handle
            Iterator handleIterator = documentHandle.entrySet().iterator();
            if (!handleIterator.hasNext()) {
                continue;
            }

            Context documentContext = (Context) handleIterator.next();

            // check the excluded names
            if (inExcludeNames((String) documentContext.get("_name"))) {
                continue;
            }

            buffer.append("  <div class=\"");
            buffer.append(CSS_CLASS_DOCUMENT);
            buffer.append("\">\n");

            for (int i = 0; i < documentProperties.length; i++) {

                String tagName = (i == 0) ? "h3" : "p";

                Object property = documentContext.get(documentProperties[i]);
                String propertyType = documentPropertyTypes[i];

                if (property != null) {
                    if (PROPERTY_TYPE_LINK.equals(propertyType)) {

                        // create lazily within method
                        if (urlPathTranslator == null) {
                            urlPathTranslator = new URLPathTranslator(context);
                        }

                        String translatedURL = urlPathTranslator.documentPathToURL(documentContext.getLocation());

                        buffer.append("    <");
                        buffer.append(tagName);
                        buffer.append(" class=\"");
                        buffer.append(CSS_CLASS_DOCUMENT_LINK);
                        buffer.append("\">");
                        buffer.append("<a href=\"").append(translatedURL).append("\">").append(property).append("</a>");
                        buffer.append("</");
                        buffer.append(tagName);
                        buffer.append(">\n");
                    }
                    else if (PROPERTY_TYPE_DATE.equals(propertyType)) {

                        if (!(property instanceof Date)) {
                            throw new JspException("Property " + property + " is not of type Date, it is "
                                + ((property == null) ? "null" : (property.getClass().getName() + ", " + property)));
                        }

                        buffer.append("    <");
                        buffer.append(tagName);
                        buffer.append(" class=\"");
                        buffer.append(CSS_CLASS_DOCUMENT_DATE);
                        buffer.append("\">");
                        buffer.append(propertyFormatter.format(property));
                        buffer.append("</");
                        buffer.append(tagName);
                        buffer.append(">\n");
                    }
                    else {
                        buffer.append("    <");
                        buffer.append(tagName);
                        buffer.append(" class=\"");
                        buffer.append(CSS_CLASS_DOCUMENT_TEXT);
                        buffer.append("\">");
                        buffer.append(propertyFormatter.format(property));
                        buffer.append("</");
                        buffer.append(tagName);
                        buffer.append(">\n");
                    }
                }
            }

            buffer.append("  </div>\n");

            counter++;
        }

        buffer.append("</div>\n");

        pageContext.getOut().append(buffer);
    }

    /** Output documents by including (jsp) files */
    @SuppressWarnings("unchecked")
    private void doOutputByIncludes(Context context, PageContext pageContext, String documentViewFile)
                                throws JspException, ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        // loop subcontexts representing subnodes
        Iterator documents = context.entrySet().iterator();
        int counter = 0;
        while (documents.hasNext() && (counter < getMaxDocuments().intValue())) {

            Context documentHandle = (Context) documents.next();

            // get first subnode of the handle
            Iterator handleIterator = documentHandle.entrySet().iterator();
            if (!handleIterator.hasNext()) {
                continue;
            }

            Context documentContext = (Context) handleIterator.next();

            // check the excluded names
            if (inExcludeNames((String) documentContext.get("_name"))) {
                continue;
            }

            // set document context in request
            request.setAttribute(getContextName(), documentContext);

            // do include
            RequestDispatcher dispatcher = request.getRequestDispatcher(documentViewFile);
            if (dispatcher == null) {
                throw new JspException("No dispatcher could be obtained for file " + documentViewFile);
            }
            dispatcher.include(request, response);

            counter++;
        }
    }

    private boolean inExcludeNames(String name) {

        String[] excludeNames = getDocumentExcludeNames();

        for (int i = 0; i < excludeNames.length; i++) {
            if (excludeNames[i].equals(name)){
                return true;
            }
        }

        return false;
    }
}
