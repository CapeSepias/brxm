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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditUserPanel extends BreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditUserPanel.class);

    private final Form form;
    private final IModel model;

    public EditUserPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel, final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form", new CompoundPropertyModel(model));
        form.setOutputMarkupId(true);
        add(form);

        FormComponent fc;

        fc = new TextField("firstName");
        form.add(fc);

        fc = new TextField("lastName");
        form.add(fc);

        fc = new TextField("email");
        fc.add(EmailAddressValidator.getInstance());
        fc.setRequired(false);
        form.add(fc);


        fc = new CheckBox("active");
        form.add(fc);

        
        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("save-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                User user = (User) model.getObject();
                String username = user.getUsername();
                try {
                    user.save();
                    log.info("User '" + username + "' saved by "
                            + ((UserSession) Session.get()).getCredentials().getStringValue("username"));
                    UserDataProvider.countMinusOne();
                    Session.get().info(getString("user-saved", model));
                    // one up
                    List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                    breadCrumbModel.setActive(l.get(l.size() -2));
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-save-failed", model));
                    log.error("Unable to save user '" + username + "' : ", e);
                }
            }
        });

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() -2));
            }
        }.setDefaultFormProcessing(false));
    }

    public String getTitle() {
        return getString("user-edit-title", model);
    }
}
