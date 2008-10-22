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
package org.hippoecm.repository.api;

import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

public interface HippoQuery extends Query {
    final static String SVN_ID = "$Id$";

    public static final String HIPPOQL = "HIPPOQL";

    public Session getSession() throws RepositoryException;
    
    public Node storeAsNode(String absPath, String type) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

    public String[] getArguments() throws RepositoryException;

    public int getArgumentCount() throws RepositoryException;

    public QueryResult execute(Map<String,String> arguments) throws RepositoryException;

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException;

    public void setLimit(long limit) throws RepositoryException;

    public void setOffset(long offset) throws RepositoryException;

    /* FIXME
     * The following methods are part of JCR2, and should be supported
     * as well within the QueryManager class:
     *
     * public PreparedQuery QueryManager.createPreparedQuery(String statement, String language) throws InvalidQueryException, RepositoryException;
     */
}
