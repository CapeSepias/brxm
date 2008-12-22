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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.UserDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditGroupPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditGroupPanel.class);

    private final IModel model;
    private final ListView localList;

    private final AdminDataTable table;

    public EditGroupPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
            final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        this.model = model;
        final Group group = (Group) model.getObject();

        add(new TextAreaWidget("description", new PropertyModel(group, "description")));

        // members
        Label localLabel = new Label("group-members-label", new ResourceModel("group-members"));
        localList = new MembershipsListEditView("group-members", "group-member", group);
        add(localLabel);
        add(localList);

        // All local groups
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new AbstractColumn(new Model(""), "add") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                final User user = (User) model.getObject();
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("group-member-add-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            group.addMembership(user.getDisplayName());
                            info(getString("group-member-added", model));
                            localList.removeAll();
                            target.addComponent(localList);
                        } catch (RepositoryException e) {
                            error(getString("group-member-add-failed", model));
                            log.error("Failed to add member", e);
                        }
                    }
                };
                item.add(action);
            }
        });

        columns.add(new PropertyColumn(new Model("First Name"), "frontend:firstname", "firstName"));
        columns.add(new PropertyColumn(new Model("Last Name"), "frontend:lastname", "lastName"));

        table = new AdminDataTable("table", columns, new UserDataProvider(), 40);
        table.setOutputMarkupId(true);
        add(table);
    }

    /** list view to be nested in the form. */
    private final class MembershipsListEditView extends ListView {
        private static final long serialVersionUID = 1L;
        private String labelId;
        private Group group;

        public MembershipsListEditView(final String id, final String labelId, final Group group) {
            super(id, new PropertyModel(group, "members"));
            this.labelId = labelId;
            this.group = group;
            setReuseItems(false);
            setOutputMarkupId(true);
        }

        protected void populateItem(ListItem item) {
            item.setOutputMarkupId(true);
            final String userName = (String) item.getModelObject();
            item.add(new Label(labelId, userName));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("group-member-remove-action")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        group.removeMembership(userName);
                        info(getString("group-member-removed", null));
                        localList.removeAll();
                        target.addComponent(localList);
                    } catch (RepositoryException e) {
                        error(getString("group-member-remove-failed", null));
                        log.error("Failed to remove memberships", e);
                    }
                }
            });
        }
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("group-edit-title", component, model);
    }

}
