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
package org.hippoecm.frontend.plugin.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginContext implements IPluginContext, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginContext.class);

    private class PluginControl implements IPluginControl, IDetachable {
        private static final long serialVersionUID = 1L;

        boolean running = true;
        String controlId;
        PluginContext[] contexts;
        IServiceTracker tracker;

        PluginControl(PluginContext[] contexts) {
            this.contexts = contexts;
            this.tracker = new ServiceTracker(IClusterable.class) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onServiceAdded(IClusterable service, String name) {
                    manager.registerService(service, PluginContext.this.controlId);
                }

                @Override
                protected void onRemoveService(IClusterable service, String name) {
                    manager.unregisterService(service, PluginContext.this.controlId);
                }
            };

            manager.registerService(this, "clusters");
            controlId = manager.getReference(this).getServiceId();

            manager.registerTracker(tracker, controlId);
        }

        public void stopPlugin() {
            if (running) {
                for (PluginContext context : contexts) {
                    if (context != null) {
                        context.stop();
                    }
                }
                children.remove(this);

                manager.unregisterTracker(tracker, controlId);

                manager.unregisterService(this, "clusters");

                running = false;
            }
        }

        public void detach() {
            for (PluginContext context : contexts) {
                context.detach();
            }
        }
    }

    private String controlId;
    private IPluginConfig config;
    private Map<String, List<IClusterable>> services;
    private Map<String, List<IServiceTracker>> listeners;
    private List<PluginControl> children;
    private boolean initializing = true;
    private boolean running = false;
    private PluginManager manager;

    public PluginContext(PluginManager manager, String controlId, IPluginConfig config) {
        this.controlId = controlId;
        this.manager = manager;

        this.services = new HashMap<String, List<IClusterable>>();
        this.listeners = new HashMap<String, List<IServiceTracker>>();
        this.children = new LinkedList<PluginControl>();
    }

    public IPluginControl start(IClusterConfig cluster) {
        log.debug("cluster {} starting cluster", this.controlId);

        final PluginContext[] contexts = new PluginContext[cluster.getPlugins().size()];
        PluginControl control = new PluginControl(contexts);

        int i = 0;
        for (IPluginConfig config : cluster.getPlugins()) {
            contexts[i++] = manager.start(config, control.controlId);
        }

        log.debug("cluster {} started cluster {}", this.controlId, control.controlId);

        children.add(control);
        return control;
    }

    public <T extends IClusterable> T getService(String name, Class<T> clazz) {
        return manager.getService(name, clazz);
    }

    public <T extends IClusterable> List<T> getServices(String name, Class<T> clazz) {
        return manager.getServices(name, clazz);
    }

    public <T extends IClusterable> IServiceReference<T> getReference(T service) {
        return manager.getReference(service);
    }

    public void registerService(IClusterable service, String name) {
        log.debug("registering {}, name {}", controlId, name);
        List<IClusterable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<IClusterable>();
            services.put(name, list);
        }
        list.add(service);
        manager.registerService(service, name);
        manager.registerService(service, controlId);
    }

    public void unregisterService(IClusterable service, String name) {
        log.debug("unregistering {}, name {}", controlId, name);
        List<IClusterable> list = services.get(name);
        if (list != null) {
            list.remove(service);
            manager.unregisterService(service, controlId);
            manager.unregisterService(service, name);
            if (list.size() == 0) {
                services.remove(list);
            }
        }
    }

    public void registerTracker(IServiceTracker listener, String name) {
        if (name == null) {
            log.error("listener name is null");
        }
        List<IServiceTracker> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<IServiceTracker>();
            listeners.put(name, list);
        }
        list.add(listener);
        if (!initializing) {
            manager.registerTracker(listener, name);
        }
    }

    public void unregisterTracker(IServiceTracker listener, String name) {
        List<IServiceTracker> list = listeners.get(name);
        if (list != null) {
            list.remove(listener);
            if (!initializing) {
                manager.unregisterTracker(listener, name);
            }
        }
    }

    // DO NOT CALL THIS API
    public void connect(IPlugin plugin) {
        if (initializing) {
            for (Map.Entry<String, List<IServiceTracker>> entry : listeners.entrySet()) {
                List<IServiceTracker> list = entry.getValue();
                for (IServiceTracker listener : list) {
                    manager.registerTracker(listener, entry.getKey());
                }
            }
            initializing = false;
        } else {
            log.warn("context was already initialized");
        }
    }

    void stop() {
        if (!initializing) {
            for (Map.Entry<String, List<IServiceTracker>> entry : listeners.entrySet()) {
                for (IServiceTracker service : entry.getValue()) {
                    manager.unregisterTracker(service, entry.getKey());
                }
            }
        }
        listeners = new HashMap<String, List<IServiceTracker>>();

        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                log.debug("unregistering {}, name {}", controlId, entry.getKey());
                manager.unregisterService(service, controlId);
                manager.unregisterService(service, entry.getKey());
            }
        }
        services = new HashMap<String, List<IClusterable>>();

        PluginControl[] controls = children.toArray(new PluginControl[children.size()]);
        for (PluginControl control : controls) {
            control.stopPlugin();
        }
    }

    public void detach() {
        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                // FIXME: ugly!
                // should all services be IDetachable?
                if ((service instanceof IDetachable) && !(service instanceof IRenderService)) {
                    ((IDetachable) service).detach();
                }
            }
        }

        for (PluginControl control : children) {
            control.detach();
        }

        if (config != null) {
            config.detach();
        }
    }

}
