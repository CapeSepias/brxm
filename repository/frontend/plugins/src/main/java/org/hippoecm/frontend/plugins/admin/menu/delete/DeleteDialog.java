/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.menu.delete;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public DeleteDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);

        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        String message;
        try {
            message = "Delete " + nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            message = e.getMessage();
        }
        dialogWindow.setTitle(message);
        add(new Label("message", message));
        if (nodeModel.getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        JcrNodeModel parentModel = nodeModel.getParentModel();

        //The actual JCR remove
        nodeModel.getNode().remove();

        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("flush", parentModel.findRootModel());
            channel.send(request);

            request = channel.createRequest("select", parentModel);
            channel.send(request);
        }
    }

    @Override
    public void cancel() {
    }

}
