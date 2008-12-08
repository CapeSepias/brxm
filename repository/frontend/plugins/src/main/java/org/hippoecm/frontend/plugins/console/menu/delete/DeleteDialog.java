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
package org.hippoecm.frontend.plugins.console.menu.delete;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;

public class DeleteDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private MenuPlugin plugin;

    public DeleteDialog(MenuPlugin plugin, IDialogService dialogWindow) {
        super(dialogWindow);
        this.plugin = plugin;
        add(new Label("message", getTitle()));
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel nodeModel = (JcrNodeModel)plugin.getModel();
        JcrNodeModel parentModel = nodeModel.getParentModel();

        //The actual JCR remove
        nodeModel.getNode().remove();

        //set the parent model as current model
        plugin.setModel(parentModel);

        //flush the JCR tree
        plugin.flushNodeModel(parentModel.findRootModel());
    }

    public IModel getTitle() {
        JcrNodeModel nodeModel = (JcrNodeModel)plugin.getModel();
        String title;
        try {
            title = "Delete " + nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            title = e.getMessage();
        }
        return new Model(title);
    }

}
