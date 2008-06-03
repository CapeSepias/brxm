/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.plugin.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.sa.Home;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IServiceReference;
import org.hippoecm.frontend.sa.plugin.IServiceTracker;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements IClusterable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    public static final String SERVICES = "services.";

    private static class RefCount implements IClusterable {
        private static final long serialVersionUID = 1L;

        IClusterable service;
        int count;

        RefCount(IClusterable service) {
            this.service = service;
            count = 1;
        }

        void addRef() {
            count++;
        }

        boolean release() {
            return (--count == 0);
        }
    }

    private Home page;
    private PluginFactory factory;
    private Map<String, List<IClusterable>> services;
    private Map<String, List<IServiceTracker>> listeners;
    private Map<Integer, RefCount> referenced;
    private int nextReferenceId;

    public PluginManager(Home page) {
        this.page = page;
        factory = new PluginFactory();
        services = new HashMap<String, List<IClusterable>>();
        listeners = new HashMap<String, List<IServiceTracker>>();
        referenced = new HashMap<Integer, RefCount>();
        nextReferenceId = 0;
    }

    public PluginContext start(IPluginConfig config, String controlId) {
        final PluginContext context = new PluginContext(page, controlId, config);
        IPlugin plugin = factory.createPlugin(context, config);
        context.connect(plugin);
        return context;
    }

    public <T extends IClusterable> T getService(String name, Class<T> clazz) {
        List<IClusterable> list = services.get(name);
        if (list != null && list.size() > 0) {
            for (IClusterable service : list) {
                if (clazz.isInstance(service)) {
                    return (T) service;
                }
            }
        }
        return null;
    }

    public <T extends IClusterable> List<T> getServices(String name, Class<T> clazz) {
        List<IClusterable> list = services.get(name);
        List<T> result = new ArrayList<T>(list.size());
        if (list != null && list.size() > 0) {
            for (IClusterable service : list) {
                if (clazz.isInstance(service)) {
                    result.add((T) service);
                }
            }
        }
        return result;
    }

    public <T extends IClusterable> IServiceReference<T> getReference(T service) {
        Map.Entry<Integer, RefCount> entry = internalGetReference(service);
        if (entry == null) {
            log.warn("Referenced service was not registered");
            return null;
        }
        return new ServiceReference<T>(page, SERVICES + entry.getKey());
    }

    public void registerService(IClusterable service, String name) {
        if (name == null) {
            log.error("service name is null");
            return;
        } else {
            log.info("registering " + service + " as " + name);
        }

        Map.Entry<Integer, RefCount> entry = internalGetReference(service);
        if (entry == null) {
            Integer id = new Integer(nextReferenceId++);
            referenced.put(id, new RefCount(service));

            String serviceId = SERVICES + id;
            internalRegisterService(service, serviceId);
        } else {
            entry.getValue().addRef();
        }

        internalRegisterService(service, name);
    }

    public void unregisterService(IClusterable service, String name) {
        if (name == null) {
            log.error("service name is null");
            return;
        } else {
            log.info("unregistering " + service + " from " + name);
        }

        List<IClusterable> list = services.get(name);
        if (list != null) {
            internalUnregisterService(service, name);

            Map.Entry<Integer, RefCount> entry = internalGetReference(service);
            if (entry.getValue().release()) {
                Integer id = entry.getKey();
                String serviceId = SERVICES + id;
                internalUnregisterService(service, serviceId);
                referenced.remove(id);
            }
        } else {
            log.error("unregistering a service that wasn't registered.");
        }
    }

    public void registerTracker(IServiceTracker listener, String name) {
        if (name == null) {
            log.error("listener name is null");
            return;
        } else {
            log.info("registering listener " + listener + " for " + name);
        }

        List<IServiceTracker> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<IServiceTracker>();
            listeners.put(name, list);
        }
        list.add(listener);

        List<IClusterable> notify = services.get(name);
        if (notify != null) {
            Iterator<IClusterable> iter = notify.iterator();
            while (iter.hasNext()) {
                IClusterable service = iter.next();
                listener.addService(service, name);
            }
        }
    }

    public void unregisterTracker(IServiceTracker listener, String name) {
        if (name == null) {
            log.error("listener name is null");
            return;
        } else {
            log.info("unregistering listener " + listener + " for " + name);
        }

        List<IServiceTracker> list = listeners.get(name);
        if (list != null) {
            if (list.contains(listener)) {
                list.remove(listener);
            }
            if (list.isEmpty()) {
                listeners.remove(name);
            }
        } else {
            log.error("unregistering a listener that wasn't registered.");
        }
    }

    <T extends IClusterable> T getService(ServiceReference<T> ref) {
        List<IClusterable> list = services.get(ref.getServiceId());
        if(list.size() > 0) {
            return (T) list.get(0);
        }
        return null;
    }
    
    private void internalRegisterService(IClusterable service, String name) {
        List<IClusterable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<IClusterable>();
            services.put(name, list);
        }
        list.add(service);

        List<IServiceTracker> notify = listeners.get(name);
        if (notify != null) {
            Iterator<IServiceTracker> iter = notify.iterator();
            while (iter.hasNext()) {
                IServiceTracker tracker = iter.next();
                tracker.addService(service, name);
            }
        }
    }

    private void internalUnregisterService(IClusterable service, String name) {
        List<IServiceTracker> notify = listeners.get(name);
        if (notify != null) {
            Iterator<IServiceTracker> iter = notify.iterator();
            while (iter.hasNext()) {
                IServiceTracker tracker = iter.next();
                tracker.removeService(service, name);
            }
        }

        List<IClusterable> list = services.get(name);
        list.remove(service);
        if (list.isEmpty()) {
            services.remove(name);
        }
    }

    private <T extends IClusterable> Map.Entry<Integer, RefCount> internalGetReference(T service) {
        for (Map.Entry<Integer, RefCount> entry : referenced.entrySet()) {
            if (entry.getValue().service == service) {
                return entry;
            }
        }
        return null;
    }
}
