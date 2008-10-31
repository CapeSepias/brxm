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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 */
public class QueryManagerDecorator extends AbstractDecorator implements QueryManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected QueryManager manager;

    protected QueryManagerDecorator(DecoratorFactory factory, SessionDecorator session, QueryManager manager) {
        super(factory, session);
        this.manager = manager;
    }

    @Override
    protected void repair(Session upstreamSession) throws RepositoryException {
        manager = session.getWorkspace().getQueryManager();
    }

    /**
     * @inheritDoc
     */
    public Query createQuery(String statement, String language) throws InvalidQueryException, RepositoryException {
        check();
        return factory.getQueryDecorator(session, manager.createQuery(statement, language));
    }

    /**
     * @inheritDoc
     */
    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException {
        check();
        Query query = manager.getQuery(NodeDecorator.unwrap(node));
        return factory.getQueryDecorator(session, query, node);
    }

    /**
     * @inheritDoc
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        check();
        return manager.getSupportedQueryLanguages();
    }
}
