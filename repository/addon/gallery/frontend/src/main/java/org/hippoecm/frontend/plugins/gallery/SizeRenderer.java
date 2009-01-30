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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.repository.api.HippoNodeType;

public class SizeRenderer extends AbstractNodeRenderer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    public static final long ONE_KB = 1024;
    public static final long ONE_MB = ONE_KB * ONE_KB;
    public static final long ONE_GB = ONE_KB * ONE_MB;

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        String size;
        if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
            Node imageSet = node.getNode(node.getName());
            try {
                Item primItem = imageSet.getPrimaryItem();
                if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                    long length = ((Node) primItem).getProperty("jcr:data").getLength();
                    if (length / ONE_GB > 0) {
                        size = String.valueOf(length / ONE_GB) + " GB";
                    } else if (length / ONE_MB > 0) {
                        size = String.valueOf(length / ONE_MB) + " MB";
                    } else if (length / ONE_KB > 0) {
                        size = String.valueOf(length / ONE_KB) + " KB";
                    } else {
                        size = String.valueOf(length) + " bytes";
                    }
                } else {
                    Gallery.log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                    return new Label(id);
                }
            } catch (ItemNotFoundException e) {
                Gallery.log.warn("ImageSet must have a primary item. " + node.getPath()
                        + " probably not of correct image set type");
                return new Label(id);
            }
        } else {
            return new Label(id);
        }
        return new Label(id, size);
    }

}
