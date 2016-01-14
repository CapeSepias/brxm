/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.contentrestapi.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

public abstract class AbstractBaseNodeVisitor extends AbstractBaseVisitor {

    protected AbstractBaseNodeVisitor(final VisitorFactory visitorFactory) {
        super(visitorFactory);
    }

    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final ContentType parentContentType = context.getContentTypes().getContentTypeForNode(node.getParent());
        final ContentTypeChild nodeType = parentContentType.getChildren().get(node.getName());

        // skip nodes that either are unknown or for which a property with the same name is also defined
        if (nodeType == null || parentContentType.getProperties().get(node.getName()) != null) {
            return;
        }

        final Map<String, Object> descendantsOutput = new TreeMap<>();
        if (nodeType.isMultiple()) {
            List<Object> siblings = (List<Object>)destination.get(node.getName());
            if (siblings == null) {
                siblings = new ArrayList<>();
                destination.put(node.getName(), siblings);
            }
            siblings.add(descendantsOutput);
        } else {
            if (destination.get(node.getName()) != null) {
                return;
            }
            destination.put(node.getName(), descendantsOutput);
        }

        visitDescendants(context, node, descendantsOutput);
    }

    protected abstract void visitDescendants(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException;
}