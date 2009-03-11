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
package org.hippoecm.hst.ocm.manager.impl;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ObjectConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.SimpleFieldsHelper;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.hippoecm.hst.ocm.NodeAware;
import org.hippoecm.hst.ocm.SimpleObjectConverter;
import org.hippoecm.hst.ocm.SimpleObjectConverterAware;
import org.hippoecm.repository.api.HippoNodeType;

public class HstObjectConverterImpl extends ObjectConverterImpl implements SimpleObjectConverter {

    protected Mapper mapper;
    protected AtomicTypeConverterProvider converterProvider;
    protected ProxyManager proxyManager;
    protected ObjectCache requestObjectCache;
    protected SimpleFieldsHelper simpleFieldsHelp;
    
    public HstObjectConverterImpl(Mapper mapper, AtomicTypeConverterProvider converterProvider, ProxyManager proxyManager, ObjectCache requestObjectCache) {
        super(mapper, converterProvider, proxyManager, requestObjectCache);
        
        this.mapper = mapper;
        this.converterProvider = converterProvider;
        this.proxyManager = proxyManager;
        this.requestObjectCache = requestObjectCache;
        this.simpleFieldsHelp = new SimpleFieldsHelper(converterProvider);
    }

    @Override
    public void insert(Session session, Object object) {
        super.insert(session, object);
    }
    
    @Override
    public void insert(Session session, Node parentNode, String nodeName, Object object) {
        super.insert(session, parentNode, nodeName, object);
    }
    
    @Override
    public void update(Session session, Object object) {
        super.update(session, object);
    }
    
    @Override
    public void update(Session session, String uuId, Object object) {
        super.update(session, uuId, object);
    }
    
    @Override
    public void update(Session session, Node objectNode, Object object) {
        super.update(session, objectNode, object);
    }
    
    @Override
    public void update(Session session, Node parentNode, String nodeName, Object object) {
        super.update(session, parentNode, nodeName, object);
    }
    
    @Override
    public Object getObject(Session session, String path) {
        Object object = null;
        Node node = null;
        
        try {
            node = (Node) session.getItem(path);
            
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                object = getObject(session, path + "/" + node.getName());
            } else {
                object = super.getObject(session, path);
            }            
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
        
        if (object instanceof NodeAware) {
            ((NodeAware) object).setNode(node);
        }
        
        if (object instanceof SimpleObjectConverterAware) {
            ((SimpleObjectConverterAware) object).setSimpleObjectConverter(this);
        }

        return object;
    }
    
    @Override
    public Object getObject(Session session, Class clazz, String path) {
        Object object = null;
        Node node = null;
        
        try {
            node = (Node) session.getItem(path);
            
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                object = getObject(session, clazz, path + "/" + node.getName());
            } else {
                object = super.getObject(session, clazz, path);
            }
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
        
        if (object instanceof NodeAware) {
            ((NodeAware) object).setNode(node);
        }
        
        if (object instanceof SimpleObjectConverterAware) {
            ((SimpleObjectConverterAware) object).setSimpleObjectConverter(this);
        }

        return object;
    }
    
    @Override
    public void retrieveAllMappedAttributes(Session session, Object object) {
        super.retrieveAllMappedAttributes(session, object);
    }
    
    @Override
    public void retrieveMappedAttribute(Session session, Object object, String attributeName) {
        super.retrieveMappedAttribute(session, object, attributeName);
    }
    
    @Override
    public String getPath(Session session, Object object) {
        return super.getPath(session, object);
    }
    
}
