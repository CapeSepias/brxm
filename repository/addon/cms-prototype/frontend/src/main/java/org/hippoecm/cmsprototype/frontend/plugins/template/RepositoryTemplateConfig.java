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
package org.hippoecm.cmsprototype.frontend.plugins.template;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

public class RepositoryTemplateConfig implements TemplateConfig {

    private static final long serialVersionUID = 1L;

    public RepositoryTemplateConfig() {
    }

    public TemplateDescriptor getTemplate(String name) {
        try {
            Node node = lookupConfigNode(name);
            if (node == null) {
                return null;
            }
            List<FieldDescriptor> children = new ArrayList<FieldDescriptor>();
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.isNodeType("hippo:field")) {
                    String path = child.getProperty("hippo:path").getString();

                    String template = null;
                    if (child.hasProperty("hippo:template")) {
                        template = child.getProperty("hippo:template").getString();
                    }

                    String renderer = null;
                    if (child.hasProperty("hippo:renderer")) {
                        renderer = child.getProperty("hippo:renderer").getString();
                    }
                    children.add(new FieldDescriptor(child.getName(), path, template, renderer));
                }
            }
            return new TemplateDescriptor(name, children);
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Privates

    private Node lookupConfigNode(String template) throws RepositoryException {
        UserSession session = (UserSession) Session.get();

        String path = HippoNodeType.CONFIGURATION_PATH + "/hippo:frontend/hippo:cms-prototype/hippo:templates/" + template;
        if (session.getRootNode().hasNode(path)) {
            return session.getRootNode().getNode(path);
        }
        return null;
    }

}
