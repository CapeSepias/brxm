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
package org.hippoecm.repository.query.lucene;

import java.util.Map;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetsQuery {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FacetsQuery.class);

    /**
     * The lucene query
     */
    private BooleanQuery query;

    public FacetsQuery(Map<String, String> facetsQuery, NamespaceMappings nsMappings,
                       ServicingIndexingConfiguration indexingConfig) {
        this.query = new BooleanQuery(true);

        if (facetsQuery != null) {
            for (Map.Entry<String, String> entry : facetsQuery.entrySet()) {
                Name nodeName;
                String internalName = "";
                try {
                    nodeName = NameFactoryImpl.getInstance().create(entry.getKey());
                    if (indexingConfig.isFacet(nodeName)) {
                        internalName = ServicingNameFormat.getInternalFacetName(nodeName, nsMappings);
                        /*
                         * TODO HREPTWO-652 : when lucene 2.3.x or higher is used, replace wildcardquery
                         * below with FixedScoreTermQuery without wildcard, and use payload to get the type
                         */
                        Query wq = new WildcardQuery(new Term(internalName, entry.getValue() + "?"));
                        //Query q = new FixedScoreTermQuery(new Term(internalName, entry.getValue() + "?"));
                        this.query.add(wq, Occur.MUST);
                    } else {
                        log.warn("Property " + nodeName.getNamespaceURI() + ":" + nodeName.getLocalName() + " not allowed for facetted search. " + "Add the property to the indexing configuration to be defined as FACET");
                    }
                } catch (IllegalNameException e) {
                    log.error(e.toString());
                }
            }
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }
}
