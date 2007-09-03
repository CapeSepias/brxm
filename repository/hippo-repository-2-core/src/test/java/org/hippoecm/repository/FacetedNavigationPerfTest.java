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
package org.hippoecm.repository;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hippoecm.repository.testutils.history.HistoryWriter;

public class FacetedNavigationPerfTest extends FacetedNavigationAbstractTest {
    private static HistoryWriter historyWriter;

    public static Test suite() {
        TestSuite suite = new TestSuite(FacetedNavigationPerfTest.class);
        historyWriter = new HistoryWriter(suite);
        return historyWriter;
    }

    public FacetedNavigationPerfTest() throws RepositoryException {
        super();
    }

    public void testPerformance() throws RepositoryException, IOException {
        int[] numberOfNodesInTests = new int[] { 500 };
        for (int i = 0; i < numberOfNodesInTests.length; i++) {
            numDocs = numberOfNodesInTests[i];
            Node node = commonStart();
            long count, tBefore, tAfter;
            tBefore = System.currentTimeMillis();
            count = node.getNode("x1").getNode("y2").getNode("z2").getNode("hippo:resultset")
                    .getProperty("hippo:count").getLong();
            tAfter = System.currentTimeMillis();

            historyWriter.write("FacetedNavigationPerfTest" + numDocs, Long.toString(tAfter - tBefore), "ms");
        }
        commonEnd();
    }
}
