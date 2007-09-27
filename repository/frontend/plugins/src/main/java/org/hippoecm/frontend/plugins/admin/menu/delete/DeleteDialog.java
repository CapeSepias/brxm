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
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrNodeModelState;
import org.hippoecm.repository.api.HippoNode;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public DeleteDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        dialogWindow.setTitle("Delete selected node");
        
        add(new Label("message", new PropertyModel(this, "message")));
        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    public void ok() throws RepositoryException {
        if (model.getNode() != null) {
            HippoNode parentNode = (HippoNode) model.getNode().getParent();
            model.getNode().remove();
            //JcrNodeModel removedModel = new JcrNodeModel(model.getNode());
            model.setNode(parentNode); // node is deleted so use parent now
            //model.getState().mark(JcrNodeModelState.CHILD_REMOVED);
            //model.getState().setRelatedNode(removedModel);
        }
    }

    public void cancel() {
    }

    public String getMessage()  {
        try {
            return "Delete " + model.getNode().getPath();
        } catch (RepositoryException e) {
            return "";
        }
    }

    public void setMessage(String message) {
    }

}
