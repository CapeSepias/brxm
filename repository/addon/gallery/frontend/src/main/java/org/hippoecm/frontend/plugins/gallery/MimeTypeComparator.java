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
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;
import org.hippoecm.repository.api.HippoNodeType;

public class MimeTypeComparator extends NodeComparator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(JcrNodeModel nodeModel1, JcrNodeModel nodeModel2) {
        try {
            String mimeType1 = "";
            Node n1 = getCanonicalNode(nodeModel1);
            if (n1.isNodeType(HippoNodeType.NT_HANDLE) && n1.hasNode(n1.getName())) {
                Node imageSet = n1.getNode(n1.getName());
                Item primItem = imageSet.getPrimaryItem();
                if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                    mimeType1 = ((Node) primItem).getProperty("jcr:mimeType").getString();
                }
            }

            String mimeType2 = "";
            Node n2 = getCanonicalNode(nodeModel2);
            if (n2.isNodeType(HippoNodeType.NT_HANDLE) && n2.hasNode(n2.getName())) {
                Node imageSet = n2.getNode(n2.getName());
                Item primItem = imageSet.getPrimaryItem();
                if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                    mimeType2 = ((Node) primItem).getProperty("jcr:mimeType").getString();
                }
            }
            return String.CASE_INSENSITIVE_ORDER.compare(mimeType1, mimeType2);
        } catch (RepositoryException e) {
            return 0;
        }
    }
}