/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow.export;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugins.standardworkflow.types.IFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndSerializer implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CndSerializer.class);

    private JcrSessionModel jcrSession;
    private HashMap<String, String> namespaces;
    private LinkedHashSet<ITypeDescriptor> types;
    private ITypeStore currentConfig;
    private ITypeStore draftConfig;

    public CndSerializer(JcrSessionModel sessionModel, String namespace) {
        jcrSession = sessionModel;
        namespaces = new HashMap<String, String>();
        types = new LinkedHashSet<ITypeDescriptor>();

        currentConfig = new JcrTypeStore();
        draftConfig = new JcrTypeStore(namespace);

        List<ITypeDescriptor> list = draftConfig.getTypes(namespace);
        for (ITypeDescriptor descriptor : list) {
            if (descriptor.isNode()) {
                String type = descriptor.getType();
                if (type.indexOf(':') > 0) {
                    String prefix = type.substring(0, type.indexOf(':'));
                    if (namespace.equals(prefix)) {
                        addType(descriptor);
                    }
                }
            }
        }
    }

    public String getOutput() {
        StringBuffer output = new StringBuffer();
        for (Map.Entry<String, String> entry : getNamespaces().entrySet()) {
            output.append("<" + entry.getKey() + "='" + entry.getValue() + "'>\n");
        }
        output.append("\n");

        sortTypes();

        for (ITypeDescriptor descriptor : types) {
            renderType(output, descriptor);
        }
        return output.toString();
    }

    public void addNamespace(String prefix) {
        if (!namespaces.containsKey(prefix)) {
            try {
                namespaces.put(prefix, jcrSession.getSession().getNamespaceURI(prefix));
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public void versionNamespace(String prefix) {
        if (namespaces.containsKey(prefix)) {
            String namespace = namespaces.get(prefix);
            String last = namespace;
            int pos = namespace.lastIndexOf('/');
            try {
                for (String registered : jcrSession.getSession().getNamespacePrefixes()) {
                    String uri = jcrSession.getSession().getNamespaceURI(registered);
                    if (uri.startsWith(namespace.substring(0, pos + 1))) {
                        if (isLater(uri, last)) {
                            last = uri;
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
                return;
            }

            int minorPos = last.lastIndexOf('.');
            if (minorPos > pos) {
                int minor = Integer.parseInt(last.substring(minorPos + 1));
                namespace = last.substring(0, minorPos + 1) + new Integer(minor + 1).toString();
                namespaces.put(prefix, namespace);
            } else {
                log.warn("namespace for " + prefix + " does not conform to versionable format");
            }
        } else {
            log.warn("namespace for " + prefix + " was not found");
        }
    }

    private static boolean isLater(String one, String two) {
        int pos = one.lastIndexOf('/');
        String[] oneVersions = one.substring(pos + 1).split("\\.");
        String[] twoVersions = two.substring(pos + 1).split("\\.");
        for (int i = 0; i < oneVersions.length; i++) {
            if (i < twoVersions.length) {
                int oneVersion = Integer.parseInt(oneVersions[i]);
                int twoVersion = Integer.parseInt(twoVersions[i]);
                if (oneVersion > twoVersion) {
                    return true;
                } else if(oneVersion < twoVersion) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void addType(ITypeDescriptor typeDescriptor) {
        String type = typeDescriptor.getType();
        if (type.indexOf(':') > 0) {
            if (!types.contains(typeDescriptor)) {
                for (String superType : typeDescriptor.getSuperTypes()) {
                    addNamespace(superType.substring(0, superType.indexOf(':')));
                }

                for (IFieldDescriptor field : typeDescriptor.getFields().values()) {
                    String subType = field.getType();
                    ITypeDescriptor sub = getTypeDescriptor(subType);
                    if (sub.isNode()) {
                        addNamespace(subType.substring(0, subType.indexOf(':')));

                        List<String> superTypes = sub.getSuperTypes();
                        for (String superType : superTypes) {
                            addNamespace(superType.substring(0, superType.indexOf(':')));
                        }
                    } else if (field.getPath().indexOf(':') > 0) {
                        addNamespace(field.getPath().substring(0, field.getPath().indexOf(':')));
                    }
                }
                types.add(typeDescriptor);
                addNamespace(type.substring(0, type.indexOf(':')));
            }
        }
    }

    private void renderField(StringBuffer output, IFieldDescriptor field) {
        String subType = field.getType();
        ITypeDescriptor sub = getTypeDescriptor(subType);
        if (sub.isNode()) {
            output.append("+");
        } else {
            output.append("-");
        }

        if (field.getPath() != null) {
            output.append(" " + field.getPath());
        } else {
            output.append(" *");
        }

        String type = sub.getType();
        if (type.indexOf(':') > 0) {
            addNamespace(type.substring(0, type.indexOf(':')));
        } else {
            type = type.toLowerCase();
        }
        output.append(" (" + type + ")");
        if (field.isMultiple()) {
            output.append(" multiple");
        }
        if (field.isMandatory()) {
            output.append(" mandatory");
        }
        if (field.isPrimary()) {
            output.append(" primary");
        }
        output.append("\n");
    }

    private void renderType(StringBuffer output, ITypeDescriptor typeDescriptor) {
        String type = typeDescriptor.getType();
        output.append("[" + type + "]");

        List<String> superFields = new LinkedList<String>();
        Iterator<String> superTypes = typeDescriptor.getSuperTypes().iterator();
        boolean first = true;
        while (superTypes.hasNext()) {
            String superType = superTypes.next();
            ITypeDescriptor superDescriptor = getTypeDescriptor(superType);
            if (superDescriptor != null) {
                Iterator<IFieldDescriptor> fields = superDescriptor.getFields().values().iterator();
                while (fields.hasNext()) {
                    IFieldDescriptor field = fields.next();
                    if (!superFields.contains(field.getPath())) {
                        superFields.add(field.getPath());
                    }
                }
            }
            if (first) {
                first = false;
                output.append(" > " + superType);
            } else {
                output.append(", " + superType);
            }
        }

        for (IFieldDescriptor field : typeDescriptor.getFields().values()) {
            if (field.isOrdered()) {
                output.append(" orderable");
                break;
            }
        }

        output.append("\n");
        for (IFieldDescriptor field : typeDescriptor.getFields().values()) {
            if (!superFields.contains(field.getPath())) {
                renderField(output, field);
            }
        }
        output.append("\n");
    }

    private ITypeDescriptor getTypeDescriptor(String subType) {
        ITypeDescriptor sub = draftConfig.getTypeDescriptor(subType);
        if (sub == null) {
            // FIXME: check subType prefix, subType could have been removed
            sub = currentConfig.getTypeDescriptor(subType);
        }
        return sub;
    }

    private void sortTypes() {
        types = new SortContext(types).sort();
    }

    class SortContext {
        HashSet<ITypeDescriptor> visited;
        LinkedHashSet<ITypeDescriptor> result;
        LinkedHashSet<ITypeDescriptor> set;

        SortContext(LinkedHashSet set) {
            this.set = set;
            visited = new HashSet<ITypeDescriptor>();
            result = new LinkedHashSet<ITypeDescriptor>();
        }

        void visit(ITypeDescriptor descriptor) {
            if (visited.contains(descriptor) || !types.contains(descriptor)) {
                return;
            }

            visited.add(descriptor);
            for (String superType : descriptor.getSuperTypes()) {
                ITypeDescriptor type = getTypeDescriptor(superType);
                visit(type);
            }
            for (IFieldDescriptor field : descriptor.getFields().values()) {
                ITypeDescriptor type = getTypeDescriptor(field.getType());
                visit(type);
            }
            result.add(descriptor);
        }

        LinkedHashSet<ITypeDescriptor> sort() {
            for (ITypeDescriptor type : set) {
                visit(type);
            }
            return result;
        }
    }
}
