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

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.ServiceTracker;

public class ModelService<T extends IModel> extends ServiceTracker<IModelListener> implements IModelService<T> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private List<IModelListener> listeners;
    private String id;
    private T model;

    public ModelService(String serviceId, T model) {
        super(IModelListener.class);

        this.id = serviceId;
        this.model = model;
        this.listeners = new LinkedList<IModelListener>();
    }

    public void init(IPluginContext context) {
        this.context = context;
        context.registerService(this, id);
        context.registerTracker(this, id);
    }

    public void destroy() {
        context.unregisterTracker(this, id);
        context.unregisterService(this, id);
    }

    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        if (model != this.model) {
            this.model = model;
            for (Object listener : listeners.toArray()) {
                ((IModelListener) listener).updateModel(model);
            }
        }
    }

    @Override
    protected void onServiceAdded(IModelListener service, String name) {
        listeners.add(service);
    }

    @Override
    protected void onRemoveService(IModelListener service, String name) {
        listeners.remove(service);
    }
    
    public void detach() {
        if(model != null) {
           model.detach();
        }
    }

}
