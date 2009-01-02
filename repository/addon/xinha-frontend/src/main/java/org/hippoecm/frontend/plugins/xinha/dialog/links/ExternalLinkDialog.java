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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.IPersistedMap;
import org.hippoecm.frontend.plugins.xinha.services.links.ExternalXinhaLink;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLink;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class ExternalLinkDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public ExternalLinkDialog(IPluginContext context, IPluginConfig config, IModel model) {
        super(model);

        add(new TextFieldWidget("href", new PropertyModel(getLink(), XinhaLink.HREF)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                update(target);
            }
        });

        add(new TextFieldWidget("title", new PropertyModel(getLink(), XinhaLink.TITLE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                update(target);
            }
        });

        add(new BooleanFieldWidget("popup", new PropertyModel(getLink(), "target")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                update(target);
            }
        });

        final Button remove = new AjaxButton("button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                onRemove();
                closeDialog();
            }

            @Override
            public boolean isVisible() {
                return hasRemoveButton();
            }
        };
        remove.add(new Label("label", "Remove"));
        addButton(remove);
    }

    protected boolean hasRemoveButton() {
        return getLink().isExisting();
    }

    protected void onRemove() {
    }

    public IModel getTitle() {
        return new StringResourceModel("dialog-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=400,height=200");
    }

    private void update(AjaxRequestTarget target) {
        IPersistedMap link = (IPersistedMap) getModelObject();
        ok.setEnabled(link.isValid() && link.hasChanged());
        if (target != null) {
            target.addComponent(ok);
        }
    }

    private ExternalXinhaLink getLink() {
        return (ExternalXinhaLink) getModel();
    }

}
