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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.Session;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.FolderTreeNode;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: LinkPickerPlugin.java 12039 2008-06-13 09:27:05Z bvanhalderen $";

    private static final long serialVersionUID = 1L;

    private JcrPropertyValueModel valueModel;

    private List<String> nodetypes = new ArrayList<String>();
    private DialogLink link;

    static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    public LinkPickerPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();
        valueModel = (JcrPropertyValueModel) getModel();

        if (config.getStringArray("nodetypes") != null) {
            String[] nodeTypes = config.getStringArray("nodetypes");
            nodetypes.addAll(Arrays.asList(nodeTypes));
        }
        if (nodetypes.size() == 0) {
            log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
        }

        DocumentListFilter filter = new DocumentListFilter(config);
        final AbstractTreeNode rootNode = new FolderTreeNode(new JcrNodeModel(config.getString("path", "/")), filter);

        Model linkText = new Model(getValue());
        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new LinkPickerDialog(LinkPickerPlugin.this, context, service, valueModel, nodetypes, rootNode);
            }
        };
        add(link = new DialogLink("value", linkText, dialogFactory, dialogService));

        context.registerService(this, IJcrService.class.getName());
        setOutputMarkupId(true);
    }

    @Override
    public void onModelChanged() {
        link.setModelObject(getValue());
        redraw();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        if (valueModel.getJcrPropertymodel().getItemModel().hasAncestor(nodeModel.getItemModel())) {
            valueModel.detach();
            modelChanged();
        }
    }

    private String getValue() {
        String docbaseUUID = (String) valueModel.getObject();
        if (docbaseUUID == null || docbaseUUID.equals("")) {
            return "[...]";
        }
        try {
            return ((UserSession) Session.get()).getJcrSession().getNodeByUUID(docbaseUUID).getPath();
        } catch (ValueFormatException e) {
            log.error("invalid docbase" + e.getMessage());
        } catch (PathNotFoundException e) {
            log.error("invalid docbase" + e.getMessage());
        } catch (RepositoryException e) {
            log.error("invalid docbase" + e.getMessage());
        }
        return "[...]";
    }

}
