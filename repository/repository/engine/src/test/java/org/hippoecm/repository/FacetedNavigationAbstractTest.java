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
package org.hippoecm.repository;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;

public abstract class FacetedNavigationAbstractTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static class Document {
        int docid;
        int x, y, z;
        public Document(int docid) {
            this.docid = docid;
            x = y = z = 0;
        }
    }

    private static String alphabet = "abcde"; // abcdefghijklmnopqrstuvwxyz
    private static int hierDepth = 1;
    private static int saveInterval = 250;
    private final static int defaultNumDocs = 20;
    protected int numDocs = -1;

    private String[] nodeNames;
    protected boolean verbose = false;
    private Map<Integer,Document> documents;

    public FacetedNavigationAbstractTest() {
        nodeNames = new String[alphabet.length()];
        for (int i=0; i<alphabet.length(); i++) {
            nodeNames[i] = alphabet.substring(i,i+1);
        }
        numDocs = defaultNumDocs;
    }

    private void createStructure(Node node, int level) throws ItemExistsException, PathNotFoundException, VersionException,
                                                              ConstraintViolationException, LockException, RepositoryException {
        for (int i=0; i<alphabet.length(); i++) {
            if(verbose)
                System.out.println(("          ".substring(0,level))+nodeNames[i]);
            Node child = node.addNode(nodeNames[i],"hippo:testdocument");
            child.addMixin("hippo:harddocument");
            if (level-1 > 0) {
                createStructure(child, level-1);
            }
        }
        if (level-1 == 0) {
            node.getSession().save();
        }
    }

    protected Map<Integer,Document> fill() throws RepositoryException {
        Node node = session.getRootNode().getNode("test");

        if (!node.hasNode("documents")) {
            node.addNode("documents", "nt:unstructured").addMixin("mix:referenceable");
        }
        if (!node.hasNode("navigation")) {
            node.addNode("navigation");
        }

        node = node.getNode("documents");
        createStructure(node, hierDepth);
        session.save();
        Map<Integer,Document> documents = new HashMap<Integer,Document>();
        for (int docid=0; docid<numDocs; docid++) {
            Random rnd = new Random(docid);
            Document document = new Document(docid);
            Node child = node;
            for (int depth=0; depth<hierDepth; depth++)
                child = child.getNode(nodeNames[rnd.nextInt(alphabet.length())]);
            child = child.addNode(Integer.toString(docid),"hippo:testdocument");
            child.addMixin("hippo:harddocument");
            child.setProperty("docid",Integer.toString(docid));
            if ((document.x = rnd.nextInt(3)) > 0) {
                child.setProperty("x","x"+document.x);
            }
            if ((document.y = rnd.nextInt(3)) > 0) {
                child.setProperty("y","y"+document.y);
            }
            if ((document.z = rnd.nextInt(3)) > 0) {
                child.setProperty("z","z"+document.z);
            }
            if ((docid+1) % saveInterval == 0) {
                session.save();
            }
            documents.put(new Integer(docid), document);
        }
        session.save();
        return documents;
    }

    protected void traverse(Node node) throws RepositoryException {
        if(verbose) {
            if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                System.out.println(node.getPath() + "\t" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
            }
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!child.getPath().equals("/jcr:system"))
                traverse(child);
        }
    }

    protected void check(String facetPath, int x, int y, int z)
        throws RepositoryException {
        int realCount = -1;
        Node node = session.getRootNode();
        if(facetPath.startsWith("/"))
            facetPath = facetPath.substring(1); // skip the initial slash
        String[] pathElements = facetPath.split("/");
        try {
            for(int i=0; i<pathElements.length; i++) {
                node = node.getNode(pathElements[i]);
            }
            if(verbose)
                System.out.println(facetPath + "\t" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
            Node nodeResultSet = node.getNode(HippoNodeType.HIPPO_RESULTSET);
            NodeIterator iter = nodeResultSet.getNodes();
            realCount = 0;
            while(iter.hasNext()) {
                Node child = iter.nextNode();
                ++realCount;
                if(verbose) {
                    System.out.print("\t" + child.getProperty("docid").getString());
                    System.out.print("\t" + (child.hasProperty("x") ? child.getProperty("x").getString().substring(1) : "0"));
                    System.out.print("\t" + (child.hasProperty("y") ? child.getProperty("y").getString().substring(1) : "0"));
                    System.out.print("\t" + (child.hasProperty("z") ? child.getProperty("z").getString().substring(1) : "0"));
                    System.out.println();
                }
            }
            if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                long obtainedCount = (int) node.getProperty(HippoNodeType.HIPPO_COUNT).getLong();
                assertEquals("counted and indicated mismatch on "+facetPath, realCount, obtainedCount);
            }
        } catch(PathNotFoundException ex) {
            System.err.println("PathNotFoundException: "+ex.getMessage());
            ex.printStackTrace(System.err);
            realCount = 0;
            if(verbose)
                System.out.println(facetPath + "\tno results");
        }
        int checkedCount = 0;
        if(verbose)
            System.out.println();
        for(Document document : documents.values()) {
            if((x == 0 || x == document.x) && (y == 0 || y == document.y) && (z == 0 || z == document.z)) {
                if(verbose)
                    System.out.println("\t"+document.docid+"\t"+document.x+"\t"+document.y+"\t"+document.z);
                ++checkedCount;
            }
        }
        if(verbose)
            System.out.println(facetPath + "\t" + realCount + "\t" + checkedCount);
        assertEquals("counted and reference mismatch on "+facetPath, checkedCount, realCount);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        if (!session.getRootNode().hasNode("test")) {
            session.getRootNode().addNode("test");
        }
        session.getRootNode().getNode("test").addNode("navigation");
    }

    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().getNode("test").hasNode("navigation")) {
            session.getRootNode().getNode("test").getNode("navigation").remove();
            session.save();
        }
        session.save();
        session.refresh(false);
        if(session.getRootNode().getNode("test").hasNode("documents")) {
            session.getRootNode().getNode("test").getNode("documents").remove();
            session.save();
        }
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
    }

    protected Node commonStart() throws RepositoryException {
        documents = fill();
        session.save();
        Node node = session.getRootNode().getNode("test/navigation");
        node = node.addNode("xyz", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "xyz");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "x", "y", "z" });
        session.save();
        session.refresh(false);
        return session.getRootNode().getNode("test/navigation").getNode("xyz");
    }

    protected void commonEnd() throws RepositoryException {
        session.refresh(false);
    }

    public boolean getVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
