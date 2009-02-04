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
package org.hippoecm.frontend.service.render;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRenderService extends Panel implements IObserver, IRenderService, IStringResourceProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractRenderService.class);

    public static final String WICKET_ID = "wicket.id";
    public static final String MODEL_ID = "wicket.model";
    public static final String VARIANT_ID = "wicket.variant";
    public static final String SKIN_ID = "wicket.skin";
    public static final String CSS_ID = "wicket.css";
    public static final String EXTENSIONS_ID = "wicket.extensions";
    public static final String FEEDBACK = "wicket.feedback";
    public static final String BEHAVIOR = "wicket.behavior";

    private boolean redraw;
    private String wicketServiceId;
    private String wicketId;
    private String cssClasses;

    private final IPluginContext context;
    private final IPluginConfig config;
    private final IModelReference modelReference;
    protected LinkedHashMap<String, ExtensionPoint> children;
    private IRenderService parent;

    public AbstractRenderService(IPluginContext context, IPluginConfig properties) {
        super("id", getPluginModel(context, properties));
        this.context = context;
        this.config = properties;

        setOutputMarkupId(true);
        redraw = false;

        wicketId = "service.render";

        this.children = new LinkedHashMap<String, ExtensionPoint>();

        if (properties.getString(WICKET_ID) != null) {
            this.wicketServiceId = properties.getString(WICKET_ID);
        } else {
            log.warn("No service id ({}) defined for {}", WICKET_ID, getClass().getName());
        }

        if (properties.getString(MODEL_ID) != null) {
            modelReference = context.getService(properties.getString(MODEL_ID), IModelReference.class);
            if (modelReference != null) {
                context.registerService(new IObserver() {

                    public IObservable getObservable() {
                        return modelReference;
                    }

                    public void onEvent(IEvent event) {
                        if (event instanceof IModelReference.IModelChangeEvent) {
                            IModelReference.IModelChangeEvent mce = (IModelReference.IModelChangeEvent) event;
                            updateModel(mce.getNewModel());
                        }
                    }

                }, IObserver.class.getName());
            }
        } else {
            modelReference = null;
        }
        if (getModel() instanceof IObservable) {
            context.registerService(this, IObserver.class.getName());
        }

        String[] extensions = config.getStringArray(EXTENSIONS_ID);
        if (extensions != null) {
            for (String extension : extensions) {
                addExtensionPoint(extension);
            }
        }

        StringBuffer sb;

        cssClasses = null;
        String[] classes = config.getStringArray(CSS_ID);
        if (classes != null) {
            sb = null;
            for (String cssClass : classes) {
                if (sb == null) {
                    sb = new StringBuffer(cssClass);
                } else {
                    sb.append(" ").append(cssClass);
                }
            }
            if (sb != null) {
                cssClasses = sb.toString();
            }
        }

        String[] skins = config.getStringArray(SKIN_ID);
        if (skins != null) {
            for (String skin : skins) {
                HeaderContributor cssContributor = HeaderContributorHelper.forCss(skin);
                add(cssContributor);
                context.registerService(cssContributor, IDialogService.class.getName());
            }
        }

        if (config.getString(FEEDBACK) != null) {
            context.registerService(new ContainerFeedbackMessageFilter(this), config.getString(FEEDBACK));
        } else {
            log.debug("No feedback id {} defined to register message filter", FEEDBACK);
        }

        if (config.getStringArray(BEHAVIOR) != null) {
            ServiceTracker<IBehaviorService> tracker = new ServiceTracker<IBehaviorService>(IBehaviorService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(IBehaviorService behavior, String name) {
                    String path = behavior.getComponentPath();
                    if (path != null) {
                        Component component = get(path);
                        if (component != null) {
                            component.add(behavior);
                        } else {
                            log.warn("No component found under {}", path);
                        }
                    } else {
                        add(behavior);
                    }
                }

                @Override
                public void onRemoveService(IBehaviorService behavior, String name) {
                    String path = behavior.getComponentPath();
                    if (path != null) {
                        Component component = get(behavior.getComponentPath());
                        if (component != null) {
                            component.remove(behavior);
                        }
                    } else {
                        remove(behavior);
                    }
                }
            };

            for (String name : config.getStringArray(BEHAVIOR)) {
                context.registerTracker(tracker, name);
            }
        }

        context.registerService(this, wicketServiceId);
    }

    // override model change methods

    @Override
    public Component setModel(IModel model) {
        if (modelReference != null) {
            modelReference.setModel(model);
        } else {
            updateModel(model);
        }
        return this;
    }

    public final void updateModel(IModel model) {
        if (getModel() instanceof IObservable) {
            context.unregisterService(this, IObserver.class.getName());
        }
        super.setModel(model);
        if (getModel() instanceof IObservable) {
            context.registerService(this, IObserver.class.getName());
        }
    }

    public IObservable getObservable() {
        return (IObservable) getModel();
    }

    public void onEvent(IEvent event) {
        getModel().detach();
        modelChanged();
    }

    // override methods with configuration data

    @Override
    public String getVariation() {
        if (config.getString(VARIANT_ID) != null) {
            return config.getString(VARIANT_ID);
        }
        // don't inherit variation from Wicket ancestor
        return null;
    }

    // utility routines for subclasses

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

    protected void redraw() {
        redraw = true;
    }

    protected void addExtensionPoint(final String extension) {
        ExtensionPoint extPt = createExtensionPoint(extension);
        children.put(extension, extPt);
        context.registerTracker(extPt, config.getString(extension));
    }

    protected abstract ExtensionPoint createExtensionPoint(String extension);

    protected void removeExtensionPoint(String name) {
        context.unregisterTracker(children.get(name), config.getString(name));
        children.remove(name);
    }

    protected IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    // allow styling

    @Override
    public void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);

        if (cssClasses != null) {
            tag.put("class", cssClasses);
        }
    }

    // implement IRenderService

    public Component getComponent() {
        return this;
    }

    public void render(PluginRequestTarget target) {
        if (redraw) {
            if (target != null) {
                target.addComponent(this);
            }
            redraw = false;
        }
        for (Map.Entry<String, ExtensionPoint> entry : children.entrySet()) {
            for (IRenderService service : entry.getValue().getChildren()) {
                service.render(target);
            }
        }
    }

    public void focus(IRenderService child) {
        IRenderService parent = getParentService();
        if (parent != null) {
            parent.focus(this);
        }
    }

    @Override
    public String getId() {
        return wicketId;
    }

    @Override
    public String getMarkupId(boolean createIfDoesNotExist) {
        return wicketServiceId;
    }

    public void bind(IRenderService parent, String wicketId) {
        this.parent = parent;
        this.wicketId = wicketId;
    }

    public void unbind() {
        this.parent = null;
        wicketId = "service.render.unbound";
    }

    public IRenderService getParentService() {
        return parent;
    }

    // implement IStringResourceProvider
    @SuppressWarnings("unchecked")
    public String getString(Map<String, String> criteria) {
        String[] translators = config.getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            for (String translatorId : translators) {
                ITranslateService translator = (ITranslateService) context.getService(translatorId,
                        ITranslateService.class);
                if (translator != null) {
                    String translation = translator.translate(criteria);
                    if (translation != null) {
                        return translation;
                    }
                }
            }
        }
        return null;
    }

    private static IModel getPluginModel(IPluginContext context, IPluginConfig properties) {
        String modelId = properties.getString(MODEL_ID);
        if (modelId != null) {
            IModelReference service = context.getService(modelId, IModelReference.class);
            if (service != null) {
                return service.getModel();
            }
        }
        return null;
    }

    protected abstract class ExtensionPoint extends ServiceTracker<IRenderService> {
        private static final long serialVersionUID = 1L;

        private final List<IRenderService> list;
        protected String extension;

        ExtensionPoint(String extension) {
            super(IRenderService.class);
            this.extension = extension;
            this.list = new LinkedList<IRenderService>();
        }

        public List<IRenderService> getChildren() {
            return list;
        }

        @Override
        public void onServiceAdded(IRenderService service, String name) {
            list.add(service);
            redraw();
        }

        @Override
        public void onServiceChanged(IRenderService service, String name) {
        }

        @Override
        public void onRemoveService(IRenderService service, String name) {
            list.remove(service);
            redraw();
        }
    }
}
