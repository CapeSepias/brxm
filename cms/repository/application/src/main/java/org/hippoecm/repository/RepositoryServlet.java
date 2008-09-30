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
package org.hippoecm.repository;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Decoder;

import org.apache.commons.lang.StringEscapeUtils;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

import org.hippoecm.repository.decorating.server.ServerServicingAdapterFactory;

public class RepositoryServlet extends HttpServlet {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    /** Parameter name of the repository storage directory */
    public final static String REPOSITORY_DIRECTORY_PARAM = "repository-directory";

    /** Parameter name of the binging address */
    public final static String REPOSITORY_BINDING_PARAM = "repository-address";

    /** Parameter name of the repository config file */
    public final static String REPOSITORY_CONFIG_PARAM = "repository-config";

    /** Default binding address for server */
    public final static String DEFAULT_BINDING_ADDRESS = "rmi://localhost:1099/hipporepository";

    /** System property for overriding the repostiory config file */
    public final static String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public final static String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    HippoRepository repository;
    String bindingAddress;
    String storageLocation;
    String repositoryConfig;

    public RepositoryServlet() {
        storageLocation = null;
    }

    private void parseInitParameters(ServletConfig config) throws ServletException {
        findStorageLocation(config);
        findBindingAddress(config);
        findRepositoryConfig(config);
    }

    /**
     * Try to extract the binding address from the config or use the DEFAULT_BINDING_ADDRESS
     * @param config
     */
    private void findBindingAddress(ServletConfig config) {
        // try to get bind address from the config or servletContext
        bindingAddress = config.getInitParameter(REPOSITORY_BINDING_PARAM);
        if (bindingAddress == null || bindingAddress.equals("")) {
            // fall back to global context setting
            bindingAddress = config.getServletContext().getInitParameter(REPOSITORY_BINDING_PARAM);
        }

        // still got nothing, use default
        if (bindingAddress == null || bindingAddress.equals("")) {
            bindingAddress = DEFAULT_BINDING_ADDRESS;
        }
    }

    /**
     *
     * @param config
     * @throws ServletException
     */
    private void findStorageLocation(ServletConfig config) throws ServletException {
        storageLocation = config.getInitParameter(REPOSITORY_DIRECTORY_PARAM);

        // basic sanity
        if (storageLocation == null) {
            return;
        }
        if ("".equals(storageLocation)) {
            storageLocation = null;
            return;
        }

        // absolute path
        //if (storageLocation.startsWith("/") || storageLocation.startsWith("file:")) {
        //    return;
        //}

        // try to parse the path
        storageLocation = config.getServletContext().getRealPath(storageLocation);
        if (storageLocation == null) {
            throw new ServletException("Cannot determin repository location "
                    + config.getInitParameter(REPOSITORY_DIRECTORY_PARAM));
        }
    }

    /**
     * Try to extract the repository config file from the config or use the DEFAULT_REPOSITORY_CONFIG
     * @param config
     */
    private void findRepositoryConfig(ServletConfig config) {
        // try to get repository config file name from the config or servletContext
        repositoryConfig = config.getInitParameter(REPOSITORY_CONFIG_PARAM);
        if (repositoryConfig == null || repositoryConfig.equals("")) {
            // fall back to global context setting
            repositoryConfig = config.getServletContext().getInitParameter(REPOSITORY_CONFIG_PARAM);
        }

        // still got nothing, use default
        if (repositoryConfig == null || repositoryConfig.equals("")) {
            repositoryConfig = DEFAULT_REPOSITORY_CONFIG;
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        parseInitParameters(config);
        System.setProperty(SYSTEM_SERVLETCONFIG_PROPERTY, repositoryConfig);

        try {
            if (storageLocation == null) {
                repository = HippoRepositoryFactory.getHippoRepository();
            } else {
                repository = HippoRepositoryFactory.getHippoRepository(storageLocation);
            }
            HippoRepositoryFactory.setDefaultRepository(repository);
            Remote remote = new ServerServicingAdapterFactory().getRemoteRepository(repository.getRepository());
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");

            /* Start rmiregistry if not already started */
            if (bindingAddress.startsWith("rmi://")) {
                int port = Registry.REGISTRY_PORT;
                try {
                    if (bindingAddress.startsWith("rmi://")) {
                        if (bindingAddress.indexOf('/', 6) >= 0) {
                            if (bindingAddress.indexOf(':', 6) >= 0 &&
                                bindingAddress.indexOf(':', 6) < bindingAddress.indexOf('/', 6)) {
                                port = Integer.parseInt(bindingAddress.substring(bindingAddress.indexOf(':', 6)+1,
                                                                   bindingAddress.indexOf('/',bindingAddress.indexOf(':',6)+1)));
                            }
                        }
                    }
                    Registry registry = LocateRegistry.createRegistry(port);
                    log.info("Started an RMI registry on port " + port);
                } catch (RemoteException ex) {
                    log.info("RMI registry has already been started on port " + port);
                }
            }

            try {
                Context ctx = new InitialContext();
                ctx.rebind(bindingAddress, remote);
                log.info("Server " + config.getServletName() + " available in context on " + bindingAddress);
            } catch (NamingException ex) {
                log.error("Cannot bind to address " + bindingAddress, ex);
                throw new ServletException("NamingException: " + ex.getMessage());
            }

        } catch (RemoteException ex) {
            log.error("Generic remoting exception ", ex);
            throw new ServletException("RemoteException: " + ex.getMessage());
        } catch (RepositoryException ex) {
            log.error("Error while setting up JCR repository: ", ex);
            throw new ServletException("RepositoryException: " + ex.getMessage());
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // if(req.getAuthType() != req.BASIC_AUTH) {
        //     res.setHeader("WWW-Authenticate","Basic realm=\"Repository\"");
        //     res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "");
        //     return;
        // }

        String username = "admin", password = "admin";
        String authhead = req.getHeader("Authorization");
        if (authhead != null) {
            String userpass = new String(new BASE64Decoder().decodeBuffer(authhead.substring(6)));
            username = userpass.substring(0, userpass.indexOf(":"));
            password = userpass.substring(userpass.indexOf(":") + 1);
        } else {
            /* An alternative for this else body part is to use:
             *   username = req.getUserPrincipal().getName(); or req.getRemoteUser()
             *   password = null;  a problem is that we don't have a password then
             * but this only works if we fully configured a security realm for this.
             */
            if (req.getAuthType() != req.BASIC_AUTH) {
                res.setHeader("WWW-Authenticate", "Basic realm=\"Repository\"");
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "");
                return;
            } else {
                username = password = null;
            }
        }

        String path = req.getRequestURI();
        if (path.startsWith(req.getContextPath())) {
            path = path.substring(req.getContextPath().length());
        }
        if (path.startsWith(req.getServletPath())) {
            path = path.substring(req.getServletPath().length());
        }
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();

        Session session = null;
        try {
            if (username == null || username.equals("")) {
                session = repository.login();
            } else {
                session = repository.login(username, (password != null ? password.toCharArray() : null));
            }

            writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
            writer.println("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            writer.println("<head><title>Hippo Repository Console</title>");
            writer.println("<style type=\"text/css\">");
            writer.println(" table.params {font-size:small}");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("  <h2>Hippo Repository Console</h2>");
            writer.println("  <h3>Request parameters</h3>");
            writer
                    .println("    <table style=\"params\" summary=\"request parameters\"><tr><th>name</th><th>value</th></tr>");
            writer.println("    <tr><td>servlet path</td><td>: <code>" + req.getServletPath() + "</code></td></tr>");
            writer.println("    <tr><td>request uri</td><td>: <code>" + req.getRequestURI() + "</code></td></tr>");
            writer.println("    <tr><td>relative path</td><td>: <code>" + path + "</code></td></tr>");
            writer.println("    </table>");

            writer.println("  <h3>Login information</h3>");
            writer.println("    <table style=\"params\" summary=\"login parameters\"><tr>");
            writer.println("    <tr><th>logged in as:</th><td>" + session.getUserID() + "</code></td></tr>");
            writer.println("    </table>");
            writer.println("  <h3>Referenced node</h3>");

            Node node = session.getRootNode();
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            writer.print("Accessing node <code>");

            writer.print("<a href=\"" + req.getContextPath() + req.getServletPath() + "//\">/root</a>");

            path = URLDecoder.decode(path, "UTF-8");

            String pathElt = "";
            String pathEltName = "";
            String currentPath = "";
            StringTokenizer pathElts = new StringTokenizer(path, "/");
            while (pathElts.hasMoreTokens()) {
                pathElt = pathElts.nextToken();
                pathEltName = StringEscapeUtils.escapeHtml(ISO9075Helper.decodeLocalName(pathElt));

                currentPath += "/" + StringEscapeUtils.escapeHtml(pathElt);
                writer.print("<a href=\"" + req.getContextPath() + req.getServletPath() + "/" + currentPath + "/\">/"
                        + pathEltName + "</a>");
            }
            writer.println("</code>");

            if (!"".equals(path)) {
                node = node.getNode(path);
            }
            writer.println("    <ul>");
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                writer.print("    <li type=\"circle\"><a href=\"" + req.getContextPath() + req.getServletPath() + "/" + StringEscapeUtils.escapeHtml(child.getPath()) + "/" + "\">");
                String displayName = StringEscapeUtils.escapeHtml(ISO9075Helper.decodeLocalName(((HippoNode)child).getDisplayName()));
                if (child.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                    writer.print(displayName + " [" + child.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]");
                } else {
                    writer.print(displayName);
                }
                writer.println("</a>");
            }
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                Property prop = iter.nextProperty();
                writer.print("    <li type=\"disc\">");
                writer.print("[name=" + prop.getName() + "] = ");
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    writer.print("[ ");
                    for (int i = 0; i < values.length; i++) {
                        writer.print((i > 0 ? ", " : "") + values[i].getString());
                    }
                    writer.println(" ]");
                } else {
                    writer.println(prop.getString());
                }
            }
            writer.println("    </ul>");


            String queryString = null;
            if ((queryString = req.getParameter("xpath")) != null || (queryString = req.getParameter("sql")) != null) {
                queryString = URLDecoder.decode(queryString, "UTF-8");
                writer.println("  <h3>Query executed</h3>");
                writer.println("  <blockquote>");
                writer.println(queryString);
                writer.println("  </blockquote>");
                writer.println("  <ol>");
                QueryManager qmgr = session.getWorkspace().getQueryManager();
                Query query = qmgr.createQuery(queryString, (req.getParameter("xpath") != null ? Query.XPATH
                        : Query.SQL));
                QueryResult result = query.execute();
                for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                    Node resultNode = iter.nextNode();
                    writer.println("    <li>");
                    if (resultNode != null) {
                        writer.println(resultNode.getPath());
                    }
                }
                writer.println("  </ol><hr><table>");
                result = query.execute();
                String[] columns = result.getColumnNames();
                writer.println("  <tr>");
                for (int i = 0; i < columns.length; i++) {
                    writer.print("    <th>");
                    writer.print(columns[i]);
                    writer.println("</th>");
                }
                writer.println("  </tr>");
                for (RowIterator iter = result.getRows(); iter.hasNext();) {
                    Row resultRow = iter.nextRow();
                    writer.println("    <tr>");
                    if (resultRow != null) {
                        Value[] values = resultRow.getValues();
                        if (values != null) {
                            for (int i = 0; i < values.length; i++) {
                                writer.print("    <td>");
                                writer.print(values[i] != null ? values[i].getString() : "");
                                writer.println("</td>");
                            }
                        }
                    }
                    writer.println("</tr>");
                }
                writer.println("  </ol>");
            }
            if ((queryString = req.getParameter("uuid")) != null) {
                queryString = URLDecoder.decode(queryString, "UTF-8");
                writer.println("  <h3>Get node by UUID</h3>");
                writer.println("  <blockquote>");
                writer.println("UUID = " + queryString);
                writer.println("  </blockquote>");
                writer.println("  <ol>");
                writer.println("    <li>");
                try {
                    Node n = session.getNodeByUUID(queryString);
                    writer.println("Found node: " + n.getPath());
                } catch (ItemNotFoundException e) {
                    writer.println("No node found for uuid " + queryString);
                } catch (RepositoryException e) {
                    writer.println(e.getMessage());
                }
                writer.println("  </li> ");
                writer.println("  </ol><hr>");

            }
            if ((queryString = req.getParameter("deref")) != null) {
                queryString = URLDecoder.decode(queryString, "UTF-8");
                writer.println("  <h3>Getting nodes having a reference to </h3>");
                writer.println("  <blockquote>");
                writer.println("UUID = " + queryString);
                Node n = null;
                try {
                    n = session.getNodeByUUID(queryString);
                    writer.println(" ( " + n.getPath() + " )");
                } catch (RepositoryException e) {
                    writer.println(e.getMessage());
                }
                writer.println("  </blockquote><hr>");
                if (n != null) {
                    PropertyIterator propIt = n.getReferences();
                    if (propIt.hasNext()) {
                        writer.println("  <table>");
                        writer.println("  <tr><th align=left>");
                        writer.println("  Node path");
                        writer.println("  </th><th align=left>" );
                        writer.println("  Property reference name" );
                        writer.println("  </th></tr>");
                        while (propIt.hasNext()) {
                            Property prop = propIt.nextProperty();
                            writer.println("  <tr><td>");
                            writer.println(prop.getParent().getPath());
                            writer.println("    </td><td>");
                            writer.println("<b>"+prop.getName()+"</b>");
                            writer.println("    </td></tr>");
                        }
                        writer.println("  </table>");

                    }else {
                        writer.println("No nodes have a reference to '" +n.getPath() + "'");
                    }
                }

            }
        } catch (LoginException ex) {
            res.setHeader("WWW-Authenticate", "Basic realm=\"Repository\"");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "");
        } catch (RepositoryException ex) {
            writer.println("<p>Error while accessing the repository, exception reads as follows:");
            writer.println("<pre>" + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        writer.println("</body></html>");
    }

    public void destroy() {
        try {
            Context ctx = new InitialContext();
            ctx.unbind(bindingAddress);
        } catch (NamingException ex) {
            log.warn("Cannot unbind from address " + bindingAddress, ex);
        }
        repository.close();
    }
}
