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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class FolderWorkflowImpl implements FolderWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private Session userSession;
    private Session rootSession;
    private Node subject;

    private final String ATTIC_PATH = "/content/attic"; // FIXME

    public FolderWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.subject = subject;
        this.userSession = userSession;
        this.rootSession = rootSession;
    }

    public Map<String, Set<String>> list() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String, Set<String>> types = new TreeMap<String, Set<String>>();
        try {
            QueryManager qmgr = rootSession.getWorkspace().getQueryManager();
            Vector<Node> foldertypes = new Vector<Node>();
            Value[] foldertypeRefs = null;
            if (subject.hasProperty("hippostd:foldertype")) {
                try {
                    foldertypeRefs = subject.getProperty("hippostd:foldertype").getValues();
                    if (foldertypeRefs.length > 0) {
                        for (int i = 0; i < foldertypeRefs.length; i++) {
                            foldertypes.add(rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNode(foldertypeRefs[i].getString()));
                        }
                    } else {
                        foldertypeRefs = null;
                    }
                } catch (PathNotFoundException ex) {
                    foldertypeRefs = null;
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                } catch (ValueFormatException ex) {
                    foldertypeRefs = null;
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
            if (foldertypeRefs == null) {
                try {
                    for (NodeIterator iter = rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNodes(); iter.hasNext();) {
                        foldertypes.add(iter.nextNode());
                    }
                } catch (PathNotFoundException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }

            for (Node foldertype : foldertypes) {
                try {
                    Set<String> prototypes = new TreeSet<String>();
                    if (foldertype.isNodeType("nt:query")) {
                        Query query = qmgr.getQuery(foldertype);
                        QueryResult rs = query.execute();
                        for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
                            Node typeNode = iter.nextNode();
                            if (typeNode.isNodeType("hippo:templatetype")) {
                                if (typeNode.hasNode("hippo:prototype")) {
                                    String documentType = typeNode.getParent().getName() + ":" + typeNode.getName();
                                    if (!documentType.startsWith("hippo:") && !documentType.startsWith("reporting:")) {
                                        prototypes.add(documentType);
                                    }
                                }
                            } else {
                                prototypes.add(typeNode.getName());
                            }
                        }
                    }
                    types.put(foldertype.getName(), prototypes);
                } catch (InvalidQueryException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return types;
    }

    public String add(String category, String template, String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String,String> arguments = new TreeMap<String,String>();
        arguments.put("name", name);
        return add(category, template, arguments);
    }

    public String add(String category, String template, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String name = ISO9075Helper.encodeLocalName(arguments.get("name"));
        QueryManager qmgr = rootSession.getWorkspace().getQueryManager();
        Node foldertype = rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNode(category);
        Query query = qmgr.getQuery(foldertype);
        QueryResult rs = query.execute();
        Node result = null;
        Node target = userSession.getRootNode();
        if(!subject.getPath().substring(1).equals(""))
            target = target.getNode(subject.getPath().substring(1));
        Map<String, String[]> renames = new TreeMap<String, String[]>();
        for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
            Node prototypeNode = iter.nextNode();
            if (prototypeNode.isNodeType("hippo:templatetype")) {
                if (prototypeNode.hasNode("hippo:prototype")) {
                    String documentType = prototypeNode.getParent().getName() + ":" + prototypeNode.getName();
                    Node prototype = prototypeNode.getNode("hippo:prototype");
                    if (!documentType.startsWith("hippo:") && !documentType.startsWith("reporting:")) {
                        if (documentType.equals(template)) {
                            renames.put("./_name", new String[] { name });
                            renames.put("./_node/_name", new String[] { name });
                            result = copy(prototype, target, renames, ".");
                            for (NodeIterator variantIter = result.getNodes(); variantIter.hasNext();) {
                                Node variant = variantIter.nextNode();
                                if (variant.hasProperty("hippostd:state"))
                                    variant.setProperty("hippostd:state", "unpublished");
                            }
                            break;
                        }

                    }
                }
            } else if (prototypeNode.getName().equals(template)) {
                if(foldertype.hasProperty("hippostd:modify")) {
                    Value[] values = foldertype.getProperty("hippostd:modify").getValues();
                    String currentTime = null;
                    for(int i=0; i+1<values.length; i+=2) {
                        String newValue = values[i+1].getString();
                        if(newValue.equals("$name")) {
                            newValue = name;
                        } else if(newValue.equals("$now")) {
                            if(currentTime == null) {
                                currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new java.util.Date());
                            }
                            newValue = currentTime;
                        } else if(newValue.startsWith("$")) {
                            newValue = arguments.get(newValue.substring(1));
                        }
                        if(renames.containsKey(values[i].getString())) {
                            String[] oldValues = renames.get(values[i].getString());
                            String[] newValues = new String[oldValues.length + 1];
                            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                            newValues[oldValues.length] = newValue;
                            renames.put(values[i].getString(), newValues);
                        } else {
                            renames.put(values[i].getString(), new String[] { newValue });
                        }
                    }
                }
                result = copy(prototypeNode, target, renames, ".");
                break;
            }
        }
        if(result != null) {
            userSession.save();
            rootSession.save();
            return result.getPath();
        } else {
            return null;
        }
    }

    public void archive(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if(name.startsWith("/"))
            name  = name.substring(1);
        name = ISO9075Helper.encodeLocalName(name);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            if (subject.getPath().equals(ATTIC_PATH)) {
                offspring.remove();
                folder.save();
            } else {
                userSession.getWorkspace().move(folder.getPath() + "/" + offspring.getName(),
                                                ATTIC_PATH + "/" + offspring.getName());
            }
        }
    }

    public void delete(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if(name.startsWith("/"))
            name  = name.substring(1);
        name = ISO9075Helper.encodeLocalName(name);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            offspring.remove();
            folder.save();
        }
    }

    Node copy(Node source, Node target, Map<String, String[]> renames, String path) throws RepositoryException {
        String[] values;
        String name = source.getName();
        if (renames.containsKey(path+"/_name")) {
            values = renames.get(path+"/_name");
            if (values.length > 0) {
                name = values[0];
            }
        }

        String type = source.getPrimaryNodeType().getName();
        if (renames.containsKey(path+"/jcr:primaryType")) {
            values = renames.get(path+"/jcr:primaryType");
            if (values.length > 0) {
                type = values[0];
                if (type.equals("")) {
                    type = null;
                }
            } else {
                type = null;
            }
        }

        if (type != null) {
            target = target.addNode(name, type);
        } else {
            target = target.addNode(name);
        }
        if(source.hasProperty("jcr:mixinTypes")) {
            Value[] mixins = source.getProperty("jcr:mixinTypes").getValues();
            for(int i=0; i<mixins.length; i++) {
                if (!mixins[i].getString().equals("hippo:remodel")) {
                    target.addMixin(mixins[i].getString());
                }
            }
        }
        if (renames.containsKey(path+"/jcr:mixinTypes")) {
            values = renames.get(path+"/jcr:mixinTypes");
            for (int i = 0; i <
                    values.length; i++) {
                if (!target.isNodeType(values[i]) && !values[i].equals("hippo:remodel")) {
                    target.addMixin(values[i]);
                }
            }
        }

        NamespaceRegistry nsreg = source.getSession().getWorkspace().getNamespaceRegistry();
        for (NodeIterator iter = source.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            String childPath;
            if(renames.containsKey(path + "/_node/_name") && !renames.containsKey(path + "/"+ child.getName()))
                childPath = path + "/_node";
            else
                childPath = path + "/" + child.getName();

            if (source.isNodeType("hippo:handle")) {
                if (source.getParent() != null && source.getParent().isNodeType("hippo:templatetype")) {
                    // FIXME: this is tightly coupled to remodeling
                    if (child.isNodeType("hippo:remodel")) {
                        String uri = child.getProperty("hippo:uri").getString();
                        String prefix = source.getParent().getParent().getName();
                        if (nsreg.getURI(prefix).equals(uri)) {
                            copy(child, target, renames, childPath);
                        }
                    }
                } else {
                    copy(child, target, renames, childPath);
                }
            } else {
                copy(child, target, renames, childPath);
            }
        }

        for (PropertyIterator iter = source.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if (!prop.getDefinition().isProtected()
                    && !prop.getDefinition().getDeclaringNodeType().isNodeType("hippo:remodel")) {
                if (prop.getDefinition().isMultiple()) {
                    target.setProperty(prop.getName(), prop.getValues());
                } else {
                    target.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        return target;
    }
}
