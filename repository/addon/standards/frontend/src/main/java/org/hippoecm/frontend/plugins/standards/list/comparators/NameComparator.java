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
package org.hippoecm.frontend.plugins.standards.list.comparators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;

public class NameComparator extends NodeComparator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        int result;
        try {
            Node n1 = o1.getNode();
            Node n2 = o2.getNode();
            if (n1 == null) {
                if (n2 == null) {
                    result = 0;
                }
                result = 1;
            } else if (o2 == null) {
                result = -1;
            }
            String name1 = n1.getName();
            String name2 = n2.getName();
            if((result = String.CASE_INSENSITIVE_ORDER.compare(name1, name2)) == 0) {
                result = n1.getIndex() - n2.getIndex();
            }
        } catch (RepositoryException e) {
            result = 0;
        }
        return result;
    }

}
