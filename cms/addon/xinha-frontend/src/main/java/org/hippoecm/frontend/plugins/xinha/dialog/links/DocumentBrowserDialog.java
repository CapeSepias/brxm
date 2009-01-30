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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.services.links.InternalXinhaLink;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLink;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentBrowserDialog extends AbstractBrowserDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentBrowserDialog.class);

    public DocumentBrowserDialog(IPluginContext context, IPluginConfig config, IModel model) {
        super(context, config, model);

        InternalXinhaLink link = (InternalXinhaLink) getModelObject();

        add(new TextFieldWidget("title", new PropertyModel(link, XinhaLink.TITLE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        add(new BooleanFieldWidget("popup", new PropertyModel(link, "target")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        checkState();
    }

    @Override
    protected JcrNodeModel findNewModel(IModel model) {
        if (model == null) {
            return null;
        }
        JcrNodeModel nodeModel = (JcrNodeModel) model;
        Node node = nodeModel.getNode();
        if (node != null) {
            NodeType type;
            try {
                type = node.getPrimaryNodeType();
                if (type.getName().equals(HippoNodeType.NT_HANDLE)) {
                    return nodeModel;
                }
            } catch (RepositoryException e) {
                log.error("Failed to find primaryNodetType", e);
            }
        }
        return null;
    }

    @Override
    protected void onOk() {
        InternalXinhaLink link = (InternalXinhaLink) getModelObject();
        if (link.isValid()) {
            link.save();
        } else {
            error("Please select a document");
        }
    }

}
