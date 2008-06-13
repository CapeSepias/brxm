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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.jackrabbit.HippoSharedItemStateManager;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.FacetPropExistsQuery;
import org.hippoecm.repository.query.lucene.FacetResultCollector;
import org.hippoecm.repository.query.lucene.FacetsQuery;
import org.hippoecm.repository.query.lucene.FixedScoreSimilarity;
import org.hippoecm.repository.query.lucene.FixedScoreTermQuery;
import org.hippoecm.repository.query.lucene.ServicingFieldNames;
import org.hippoecm.repository.query.lucene.ServicingIndexingConfiguration;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedNavigationEngineThirdImpl extends ServicingSearchIndex
    implements FacetedNavigationEngine<FacetedNavigationEngineThirdImpl.QueryImpl, FacetedNavigationEngineThirdImpl.ContextImpl> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    class QueryImpl extends FacetedNavigationEngine.Query {
        String xpath;

        public QueryImpl(String xpath) {
            this.xpath = xpath;
        }

        public String toString() {
            return xpath;
        }
    }

    class ResultImpl extends FacetedNavigationEngine.Result {
        int length;
        Iterator<NodeId> iter = null;

        ResultImpl(int length, Set<NodeId> result) {
            this.length = length;
            if (result != null) {
                this.iter = result.iterator();
            }
        }

        public int length() {
            return length;
        }

        public Iterator<NodeId> iterator() {
            return iter;
        }

        public String toString() {
            return getClass().getName() + "[length=" + length + "]";
        }
    }

    class ContextImpl extends FacetedNavigationEngine.Context {
        Session session;
        String userId;
        Subject subject;
        AuthorizationQuery authorizationQuery;
        NodeTypeManager ntMgr;

        ContextImpl(Session session, String userId, Subject subject, NodeTypeManager ntMgr)
                throws RepositoryException {
            this.session = session;
            this.userId = userId;
            this.subject = subject;
            this.ntMgr = ntMgr;
            this.authorizationQuery = new AuthorizationQuery(subject, getNamespaceMappings(),
                    (ServicingIndexingConfiguration) getIndexingConfig(), ntMgr, session);
        }
    }

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngine.class);

    public FacetedNavigationEngineThirdImpl() {
    }

    public ContextImpl prepare(String userId, Subject subject, List<QueryImpl> initialQueries,
            Session session) throws RepositoryException {
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        return new ContextImpl(session, userId, subject, ntMgr);
    }

    public void unprepare(ContextImpl authorization) {
        // deliberate ignore
    }

    public void reload(Map<Name, String[]> facetValues) {
        // deliberate ignore
    }

    public boolean requiresReload() {
        return false;
    }

    public boolean requiresNotify() {
        return false;
    }

    public void notify(String docId, Map<Name, String[]> oldFacets, Map<Name, String[]> newFacets) {
        // deliberate ignore
    }

    public void purge() {
        // deliberate ignore
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
            Map<String, String> facetsQueryMap, QueryImpl openQuery, Map<String, Map<String, Count>> resultset,
            Map<Map<String, String>, Map<String, Map<String, Count>>> futureFacetsQueries, HitsRequested hitsRequested)
            throws UnsupportedOperationException {

        NamespaceMappings nsMappings = getNamespaceMappings();

        /*
         * facetsQuery: get the query for the facets that are asked for
         */
        FacetsQuery facetsQuery = new FacetsQuery(facetsQueryMap, nsMappings,
                (ServicingIndexingConfiguration) getIndexingConfig());

        /*
         * initialQuery: get the query for initialQuery
         */
        org.apache.lucene.search.Query initialLuceneQuery = null;
        if (initialQuery != null && !initialQuery.xpath.equals("")) {
            initialLuceneQuery = new FixedScoreTermQuery(new Term(ServicingFieldNames.HIPPO_PATH, initialQuery.xpath));
        }

        /*
         * authorizationQuery: get the query for the facets the person is allowed to see (which
         * is again a facetsQuery)
         */

        BooleanQuery searchQuery = new BooleanQuery(true);

        if (facetsQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(facetsQuery.getQuery(), Occur.MUST);
        }
        // TODO perhaps create cached user specific filter for authorisation to gain speed
        if (contextImpl.authorizationQuery.getQuery().clauses().size() > 0) {
            searchQuery.add(contextImpl.authorizationQuery.getQuery(), Occur.MUST);
        }

        if (initialLuceneQuery != null) {
            searchQuery.add(initialLuceneQuery, Occur.MUST);
        }

        FacetResultCollector collector = null;
        IndexReader indexReader = null;
        IndexSearcher searcher = null;
        try {
            /*
             * if getIndexReader(true) you will also get version storage index which
             * cannot be used for facet searches, therefore set 'false'
             */
            //indexReader = getIndex().getIndexReader();
            indexReader = getIndexReader(false);
            searcher = new IndexSearcher(indexReader);
            searcher.setSimilarity(new FixedScoreSimilarity());
            // In principle, below, there is always one facet
            if (resultset != null) {
                for (String facet : resultset.keySet()) {
                    /*
                     * Nodes not having this facet, still should be counted if they are a hit
                     * in the query without this facet. Therefor, first get the count query without
                     * FacetPropExistsQuery.
                     */

                    long start1 = System.currentTimeMillis();
                    int numHits = searcher.search(searchQuery).length();
                    if (log.isDebugEnabled()) {
                        log.debug("lucene query no collector took: \t" + (System.currentTimeMillis() - start1)
                                + " ms for #" + numHits + ". Query: " + searchQuery.toString());
                    }
                    /*
                     * facetPropExists: the node must have the property as facet
                     */
                    FacetPropExistsQuery facetPropExists = new FacetPropExistsQuery(facet, nsMappings,
                            (ServicingIndexingConfiguration) getIndexingConfig());
                    searchQuery.add(facetPropExists.getQuery(), Occur.MUST);

                    long start = System.currentTimeMillis();
                    collector = new FacetResultCollector(indexReader, nsMappings.translatePropertyName(NameFactoryImpl
                            .getInstance().create(facet)), (facet != null ? resultset.get(facet) : null),
                            hitsRequested, nsMappings);
                    searcher.search(searchQuery, collector);
                    // set the numHits value
                    collector.setNumhits(numHits);
                    if (log.isDebugEnabled()) {
                        log.debug("lucene query with collector took: \t" + (System.currentTimeMillis() - start)
                                + " ms for #" + collector.getNumhits() + ". Query: " + searchQuery.toString());
                    }

                }
            } else {
                // resultset is null, so search for HippoNodeType.HIPPO_RESULTSET

                long start = System.currentTimeMillis();
                collector = new FacetResultCollector(indexReader, null, null, hitsRequested, nsMappings);
                searcher.search(searchQuery, collector);
                if (log.isDebugEnabled()) {
                    log.debug("lucene query with collector took: \t" + (System.currentTimeMillis() - start)
                            + " ms for #" + collector.getNumhits() + ". Query: " + searchQuery.toString());
                }

            }

        } catch (IllegalNameException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (indexReader != null) {
                try {
                    indexReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return this.new ResultImpl(collector.getNumhits(), collector.getHits());

    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
            Map<String, String> facetsQuery, QueryImpl openQuery, HitsRequested hitsRequested) {
        return view(queryName, initialQuery, authorization, facetsQuery, openQuery, null, null, hitsRequested);
    }

    public QueryImpl parse(String query) {
        return this.new QueryImpl(query);
    }

    @Override
    protected void doInit() throws IOException {
        QueryHandlerContext context = getContext();
        HippoSharedItemStateManager stateMgr = (HippoSharedItemStateManager) context.getItemStateManager();
        stateMgr.repository.setFacetedNavigationEngine(this);
        super.doInit();

    }
}
