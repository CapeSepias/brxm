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
package org.hippoecm.frontend.model;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.spi.Event;
import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrQueryModel extends LoadableDetachableModel implements IDataProvider, IObservable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrQueryModel.class);

    public static final int UNKNOWN_SIZE = -1;
    
    private String statement;
    private String language;
    private IObservationContext context;
    private JcrEventListener listener;

    public JcrQueryModel(String statement, String language) {
        this.statement = statement;
        this.language = language;
    }

    protected Object load() {
        try {
            QueryManager qmgr = ((UserSession) Session.get()).getJcrSession().getWorkspace().getQueryManager();
            Query query = qmgr.createQuery(statement, language);
            return query.execute();
        } catch (RepositoryException ex) {
            log.error("could not execute query", ex);
        }
        return null;
    }

    public Iterator<Node> iterator(int first, int count) {
        QueryResult result = (QueryResult) getObject();
        if (result != null) {
            try {
                final NodeIterator nodeIter = result.getNodes();
                if (nodeIter != null) {
                    nodeIter.skip(first);
                }
                return new Iterator<Node>() {

                    public boolean hasNext() {
                        return nodeIter.hasNext();
                    }

                    public Node next() {
                        return nodeIter.nextNode();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("removing a node from a query result is not supported");
                    }
                };
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return new ArrayList<Node>(0).iterator();
    }

    public IModel model(Object object) {
        return new JcrNodeModel((Node) object);
    }

    public int size() {
        QueryResult result = (QueryResult) getObject();
        if (result != null) {
            try {
                long size = result.getNodes().getSize();
                if (size > Integer.MAX_VALUE) {
                    log.error("Invalid number of results (" + size + ")");
                    return 0;
                }
                if (size < 0) {
                    // FIXME: getSize() is unsupported
                    // log.warn("Unknown number of results");
                    return UNKNOWN_SIZE;
                }
                return (int) size;
            } catch (RepositoryException ex) {
                log.error("could not determine size of resultset", ex);
            }
        }
        return 0;
    }

    public void setObservationContext(IObservationContext context) {
        this.context = context;
    }

    public void startObservation() {
        listener = new JcrEventListener(context, Event.NODE_ADDED | Event.NODE_REMOVED, "/", true, null, null);
        listener.start();
    }

    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

}
