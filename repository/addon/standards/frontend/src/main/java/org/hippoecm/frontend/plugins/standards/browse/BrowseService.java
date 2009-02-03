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
package org.hippoecm.frontend.plugins.standards.browse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseService implements IBrowseService<IModel>, IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BrowseService.class);

    private class DocumentModelService extends ModelReference<JcrNodeModel> {
        private static final long serialVersionUID = 1L;

        DocumentModelService(IPluginConfig config, JcrNodeModel document) {
            super(config.getString("model.document"), document);
        }

        public void updateModel(JcrNodeModel model) {
            super.setModel(model);
        }

        @Override
        public void setModel(JcrNodeModel model) {
            selectDocument(model);
        }
    }

    private JcrNodeModel folder;
    private ModelReference<JcrNodeModel> folderService;
    private DocumentModelService documentService;

    public BrowseService(final IPluginContext context, final IPluginConfig config, JcrNodeModel document) {

        document = findDocument(document);

        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));

        if (config.getString("model.document") != null) {
            documentService = new DocumentModelService(config, document);
            documentService.init(context);
        } else {
            log.error("no document model service (model.document) specified");
        }

        if (config.getString("model.folder") != null) {
            folderService = new ModelReference<JcrNodeModel>(config.getString("model.folder"), folder);
            folderService.init(context);
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return folderService;
                }

                public void onEvent(IEvent event) {
                    selectFolder(folderService.getModel());
                }

            }, IObserver.class.getName());
        } else {
            log.error("no folder model service (model.folder) specified");
        }
    }

    public void selectFolder(IModel model) {
        if (model != null && (model instanceof JcrNodeModel) && !model.equals(folder)) {
            folder = (JcrNodeModel) model;

            documentService.updateModel(new JcrNodeModel((Node) null));
        }
    }

    public void selectDocument(JcrNodeModel model) {
        browse(model);
    }

    public void setFolderModel(JcrNodeModel nodeModel) {
        folderService.setModel(nodeModel);
    }

    public void updateDocumentModel(JcrNodeModel nodeModel) {
        documentService.updateModel(nodeModel);
    }

    public void browse(IModel model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel document = findDocument((JcrNodeModel) model);
            if (folder != null) {
                if (document != null) {
                    updateDocumentModel(document);
                }
                setFolderModel(folder);
            } else {
                log.warn("No folder found for model {}", model);
            }
        } else {
            log.warn("Model {} is not an JcrNodeModel", model);
        }
    }

    public void detach() {
        folderService.detach();
        documentService.detach();
    }

    private JcrNodeModel findDocument(JcrNodeModel document) {
        if (isFolder(document)) {
            folder = document;
            document = null;
        } else {
            folder = document.getParentModel();
            while (!isFolder(folder)) {
                document = folder;
                folder = folder.getParentModel();
            }
        }
        return document;
    }

    private boolean isFolder(JcrNodeModel nodeModel) {
        if (nodeModel != null) {
            try {
                Node node = nodeModel.getNode();
                if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")
                        || node.isNodeType("hippo:namespace")) {
                    return true;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return false;
        }
        return true;
    }

}
