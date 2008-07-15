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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.ISO9075Helper;
import org.junit.Test;

public class TrivialServerTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Node root;

    public void setUp() throws Exception {
        super.setUp();
        root = session.getRootNode();
        if(root.hasNode("test"))
            root = root.getNode("test");
        else
            root = root.addNode("test");
    }

    @Test public void testTrivialNodeOperations()  {
        try {
            root.addNode("x");
        } catch (RepositoryException e) {
            fail("Failed to add node: " + e.getMessage());
            e.printStackTrace();
        }

        // transient
        try {
            assertNotNull(root.getNode("x"));
        } catch (RepositoryException e) {
            fail("Failed to find node: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("Failed to save node: " + e.getMessage());
            e.printStackTrace();
        }

        // after persist
        try {
            assertNotNull(root.getNode("x"));
        } catch (RepositoryException e) {
            fail("Failed to find node: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            root.getNode("x").remove();
        } catch (RepositoryException e) {
            fail("Failed to delete node: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("Failed to save node deletion: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            root.getNode("x");
            fail("Deleted node found.");
        } catch (PathNotFoundException e) {
            // ok
        } catch (RepositoryException e) {
            fail("Failed to not find node: " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Test public void testEncodedNode() throws RepositoryException {
        String name = "2..,!@#$%^&*()_-[]{}|\\:;'\".,/?testnode";
        Node encodedNode = root.addNode(ISO9075Helper.encodeLocalName(name));
        assertNotNull(encodedNode);
        assertEquals(ISO9075Helper.encodeLocalName(name),encodedNode.getName());
        session.save();
        Node encodedNode2 = root.getNode(ISO9075Helper.encodeLocalName(name));

        // assertEquals(encodedNode, encodedNode2);    -- this test is WRONG [BvH], instead:
        assertTrue(encodedNode.isSame(encodedNode2));

        assertEquals(ISO9075Helper.encodeLocalName(name),encodedNode.getName());
        encodedNode2.remove();
    }

}
