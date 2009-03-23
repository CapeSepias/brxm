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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MemoryTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";


    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_PASS = "password";

    private static final int NUMBER_OF_LOGINS = 2;
    private static final int NUMBER_OF_GCS = 5;
    private static final long GC_DELAY_MS = 2;
    private static final long FINITSH_DELAY_MS = 1;

    public void cleanup() throws RepositoryException  {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }
        session.save();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        cleanup();
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        // create test user
        Node testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     *  run with: mvn -o test -Dtest=MemoryTest -Dmaven.surefire.debug="-agentlib:yjpagent"
     *  and make a memorydump during FINISH_DELAY_MS
     * @throws RepositoryException
     */
    @Test
    public void testManyLogins() throws RepositoryException {
        // setup user session
        Session userSession = null;
        
        for (int i = 0; i < NUMBER_OF_LOGINS; i++) {
            userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
            userSession.logout();
        }
        for (int i = 0; i < NUMBER_OF_GCS; i++) {
            System.gc();
            try {
                Thread.sleep(GC_DELAY_MS);
            } catch(InterruptedException ex) {
            }
        }
        try {
            Thread.sleep(FINITSH_DELAY_MS);
        } catch(InterruptedException ex) {
        }
    }



}
