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
package org.hippoecm.repository.updater;

import org.hippoecm.repository.ext.UpdaterItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.Utilities;

public class UpdaterTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final String[] content = {
        "/test", "nt:unstructured",
        "/test/docs", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/docs/d", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/docs/d/d", "hippo:document",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/docs/doc", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/docs/doc/doc", "hippo:testdocument",
        "jcr:mixinTypes", "hippo:harddocument",
        "hippo:x", "test"
    };

    @Override
    public void setUp() throws Exception {
        super.setUp(true);
        build(session, content);
        session.save();
    }

    @Test
    public void test() throws RepositoryException {
        Utilities.dump(session.getRootNode().getNode("test"));
        UpdaterEngine updater = new UpdaterEngine(session, new Modules());
        updater.update(new UpdaterItemVisitor.Default() {
                @Override
                public void leaving(Node visit, int level) throws RepositoryException {
                    if(visit.hasProperty("hippo:x")) {
                    visit.getProperty("hippo:x").remove();
                }
                if (visit.getPath().equals("/test/docs/d/d")) {
                    ((UpdaterNode) visit).setPrimaryNodeType("hippo:testdocument");
                    visit.setProperty("hippo:y", "bla");
                }
            }
        });
        updater.commit();
        Utilities.dump(session.getRootNode().getNode("test"));
        session.save();
    }
}
