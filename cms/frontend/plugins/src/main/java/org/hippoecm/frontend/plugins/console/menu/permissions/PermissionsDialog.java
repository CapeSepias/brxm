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
package org.hippoecm.frontend.plugins.console.menu.permissions;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    /**
     * prededfined action constants in checkPermission
     */
    public static final String READ_ACTION = "read";
    public static final String REMOVE_ACTION = "remove";
    public static final String ADD_NODE_ACTION = "add_node";
    public static final String SET_PROPERTY_ACTION = "set_property";

    public static final String[] JCR_ACTIONS = new String[] { READ_ACTION, REMOVE_ACTION, ADD_NODE_ACTION,
            SET_PROPERTY_ACTION };


    static final Logger log = LoggerFactory.getLogger(PermissionsDialog.class);

    public PermissionsDialog(MenuPlugin plugin, IPluginContext context) {
        final JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();
        setModel(nodeModel);

        final Label usernameLabel = new Label("username", "Unknown");
        final Label membershipsLabel = new Label("memberships", "None");
        final Label allActionsLabel = new Label("all-actions", "None");
        final Label allRolesLabel = new Label("all-roles", "None");
        final Label actionsLabel = new Label("actions", "None");
        final Label rolesLabel = new Label("roles", "None");
        add(usernameLabel);
        add(membershipsLabel);
        add(allActionsLabel);
        add(allRolesLabel);
        add(actionsLabel);
        add(rolesLabel);
        try {
            Node subject = nodeModel.getNode();

            // FIXME: hardcoded workflowuser
            Session privSession = subject.getSession()
                    .impersonate(new SimpleCredentials("workflowuser", new char[] {}));

            String userID = subject.getSession().getUserID();
            String[] allRoles = getRoles(privSession);
            String[] memberships = getMemberships(privSession, userID);
            String[] actions = getAllowedActions(subject, JCR_ACTIONS);
            String[] roles = getAllowedActions(subject, allRoles);

            usernameLabel.setModel(new Model(userID));
            membershipsLabel.setModel(new Model(StringUtils.join(memberships, ", ")));
            allActionsLabel.setModel(new Model(StringUtils.join(JCR_ACTIONS, ", ")));
            allRolesLabel.setModel(new Model(StringUtils.join(allRoles, ", ")));
            actionsLabel.setModel(new Model(StringUtils.join(actions, ", ")));
            rolesLabel.setModel(new Model(StringUtils.join(roles, ", ")));

        } catch (RepositoryException ex) {
            actionsLabel.setModel(new Model(ex.getClass().getName() + ": " + ex.getMessage()));
        }
        cancel.setVisible(false);
    }

    private boolean hasPermission(Node node, String actions) throws RepositoryException {
        try {
            node.getSession().checkPermission(node.getPath(), actions);
            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    private String[] getAllowedActions(Node node, String[] actions) throws RepositoryException {
        final List<String> list = new ArrayList<String>();
        for (String action : actions) {
            if (hasPermission(node, action)) {
                list.add(action);
            }
        }
        Collections.sort(list);
        return list.toArray(new String[list.size()]);
    }

    private String[] getRoles(Session session) throws RepositoryException {
        final String queryString = "//element(*, hippo:role)";
        final String queryType = "xpath";
        final List<String> list = new ArrayList<String>();
        try {
            Query query = session.getWorkspace().getQueryManager().createQuery(queryString, queryType);
            NodeIterator nodeIter = query.execute().getNodes();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.nextNode();
                // FIXME query should not return the hippo:prototype node, or the
                // prototype node should not exists at all
                if (node != null && !"hippo:prototype".equals(node.getName())) {
                    list.add(node.getName());
                }
            }
        } catch (RepositoryException e) {
            log.error("Error executing query[" + queryString + "]", e);
        }
        Collections.sort(list);
        return list.toArray(new String[list.size()]);
    }

    private String[] getMemberships(Session session, String username) throws RepositoryException {
        final String queryString = "//element(*, hippo:group)[jcr:contains(@hippo:members, '" + username
                + "')]";
        final String queryType = "xpath";
        final List<String> list = new ArrayList<String>();
        try {
            Query query = session.getWorkspace().getQueryManager().createQuery(queryString, queryType);
            NodeIterator nodeIter = query.execute().getNodes();
            log.debug("Number of memberships found with query '{}' : {}", queryString, nodeIter.getSize());
            while (nodeIter.hasNext()) {
                Node node = nodeIter.nextNode();
                if (node != null) {
                    list.add(node.getName());
                }
            }
        } catch (RepositoryException e) {
            log.error("Error executing query[" + queryString + "]", e);
        }
        Collections.sort(list);
        return list.toArray(new String[list.size()]);
    }

    @Override
    public void ok() {
    }

    @Override
    public void cancel() {
    }

    public IModel getTitle() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        String path;
        try {
            path = nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
            log.warn("Unable to get path for : " + nodeModel);
        }
        return new Model("Permissions for " + path);
    }
}
