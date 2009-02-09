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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.repository.api.HippoNodeType;

public class MimeTypeAttributeModifier extends AbstractNodeAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    static class MimeTypeAttributeModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel nodeModel;

        public MimeTypeAttributeModel(JcrNodeModel model) {
            this.nodeModel = model;
        }

        @Override
        public void detach() {
            super.detach();
            nodeModel.detach();
        }
        
        protected Object load() {
            Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    String cssClass;
                    if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                        Node imageSet = node.getNode(node.getName());
                        try {
                            Item primItem = imageSet.getPrimaryItem();
                            if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                String mimeType = ((Node) primItem).getProperty("jcr:mimeType").getString();
                                if (mimeType.startsWith("application")) {
                                    cssClass = mimeType;
                                    cssClass = StringUtils.replace(cssClass, "/", "-");
                                    cssClass = StringUtils.replace(cssClass, ".", "-");
                                    if (cssClass.contains("opendocument")) {
                                        cssClass = "application-opendocument";
                                    }
                                } else {
                                    cssClass = StringUtils.substringBefore(mimeType, "/");
                                }
                                return "mimetype-" + cssClass + "-16";
                            } else {
                                Gallery.log.warn("primary item of image set must be of type "
                                        + HippoNodeType.NT_RESOURCE);
                            }
                        } catch (ItemNotFoundException e) {
                            Gallery.log.warn("ImageSet must have a primary item. " + node.getPath()
                                    + " probably not of correct image set type");
                        }
                    } else if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
                        return "folder-16";
                    } else {
                        Gallery.log.warn("Node " + node.getPath() + " is not a handle or a folder");
                    }
                } catch (RepositoryException ex) {
                    Gallery.log.error("Unable to determine mime type of document", ex);
                }
            }
            return null;
        }
    }

    @Override
    public AttributeModifier getCellAttributeModifier(Node node) {
        return new CssClassAppender(new MimeTypeAttributeModel(new JcrNodeModel(node)));
    }

    @Override
    public AttributeModifier getColumnAttributeModifier(Node node) {
        return new CssClassAppender(new Model("icon-16"));
    }
}