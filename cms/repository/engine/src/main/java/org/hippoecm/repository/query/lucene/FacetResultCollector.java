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
package org.hippoecm.repository.query.lucene;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.HitCollector;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;

public class FacetResultCollector extends HitCollector {
    private IndexReader reader;
    private String internalName;
    private int numhits;
    private Set<String> hits;
    private Map<String,Count> facetMap;
    private FieldSelector fieldSelector;
    private int offset;
    private int limit;

    public FacetResultCollector(IndexReader reader, String facet,  Map<String,Map<String,Count>> resultset, HitsRequested hitsRequested, NamespaceMappings nsMappings) {
        this.reader = reader;
        if(facet != null){
             try {
              this.internalName = ServicingNameFormat.getInternalFacetName(facet, nsMappings);
             } catch(Exception ex) {
                  System.err.println(ex.getMessage());
                  ex.printStackTrace(System.err);
             }
        }
        this.numhits = 0;

        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add(ServicingFieldNames.HIPPO_PATH);
        this.fieldSelector = new SetBasedFieldSelector(fieldNames, new HashSet());
        if(hitsRequested.isResultRequested()) {
            this.hits = new HashSet<String>();
            this.offset = hitsRequested.getOffset();
            this.limit = hitsRequested.getLimit();
        } else {
            this.hits = null;
        }

        if(facet != null && resultset.get(facet)!= null) {
             facetMap = resultset.get(facet);
        }
    }
    public final void collect(final int docid, final float score) {
        try {
            if(hits != null) {
                if(offset == 0 && hits.size() < limit ) {
                    Document d = reader.document(docid,fieldSelector);
                    Field f = d.getField(ServicingFieldNames.HIPPO_PATH);
                    if(f!=null){
                        hits.add(f.stringValue());
                    }
                } else if (offset > 0){
                    // decrement offset untill it is 0. Then start gathering results above
                    offset--;
                }
            }
             if(facetMap != null){
                 final TermFreqVector tfv = reader.getTermFreqVector(docid, internalName);
                 if(tfv != null) {
                     for(int i=0; i<tfv.getTermFrequencies().length; i++) {
                         Count count = facetMap.get(tfv.getTerms()[i]);
                         if(count == null) {
                             facetMap.put(tfv.getTerms()[i], new Count(1));
                         } else {
                             count.count += 1;
                          }
                     }
                 }
             } else {
                 /*
                  * only without facetMap the numHits are correct directly. With a non-null
                  * facet map, a seperate query is needed to get the correct count
                  */
                 ++numhits;
             }
        } catch(Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public Set<String> getHits() {
        return hits;
    }

    public int getNumhits() {
        return numhits;
    }

    public void setNumhits(int numhits) {
        this.numhits = numhits;
}


}
