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
package org.hippoecm.frontend.plugins.cms.browse;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.UnitExpandCollapseBehavior;
import org.hippoecm.frontend.plugins.yui.layout.UnitSettings;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;

import javax.jcr.Node;
import java.util.Iterator;

public class BrowserPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final WireframeSettings settings;
    private UnitExpandCollapseBehavior toggler;

    private IModelReference documentModelReference;
    private RenderService editorBrowseService;

    public BrowserPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        addExtensionPoint("top");
        addExtensionPoint("center");
        addExtensionPoint("left");

        context.registerTracker(new ServiceTracker<RenderService>(RenderService.class) {
            @Override
            protected void onServiceAdded(RenderService service, String name) {
                super.onServiceAdded(service, name);
                editorBrowseService = service;
            }

            @Override
            protected void onRemoveService(RenderService service, String name) {
                super.onRemoveService(service, name);
                editorBrowseService = null;
            }
        }, "service.browse.editor");

        context.registerTracker(new ServiceTracker<IModelReference>(IModelReference.class) {

            IObserver observer;

            @Override
            protected void onServiceAdded(final IModelReference service, String name) {
                documentModelReference = service;

                if (observer == null) {
                    context.registerService(observer = new IObserver() {

                        public IObservable getObservable() {
                            return service;
                        }

                        public void onEvent(Iterator events) {
                            if (documentModelReference != null) {
                                JcrNodeModel documentModel = (JcrNodeModel) documentModelReference.getModel();
                                if(documentModel.getItemModel() == null || documentModel.getItemModel().getPath() == null) {
                                    //Prevent calling toggle twice in a single request: we will end up here after
                                    //the onToggle override below sets the documentModel to null to remove the selected
                                    //state from the doclisting
                                    return;
                                }
                            }

                            AjaxRequestTarget target = AjaxRequestTarget.get();
                            if (target != null) {
                                UnitSettings leftSettings = settings.getUnit("left");
                                if (leftSettings != null && leftSettings.isExpanded()) {
                                    toggler.toggle("left", target);
                                }
                            }
                        }
                    }, IObserver.class.getName());
                }
                super.onServiceAdded(service, name);
            }

            @Override
            protected void onRemoveService(IModelReference service, String name) {
                super.onRemoveService(service, name);
                if (observer != null) {
                    context.unregisterService(observer, IObserver.class.getName());
                    observer = null;
                }
                documentModelReference = null;
            }
        }, config.getString("model.document"));


        // register as the IRenderService for the browser service
        String browserId = config.getString("browser.id");
        context.registerService(this, browserId);

        add(new WireframeBehavior(settings = new WireframeSettings(config.getPluginConfig("layout.wireframe"))));
    }

    @Override
    protected ExtensionPoint createExtensionPoint(String extension) {
        return new ExtensionPoint(extension) {
            @Override
            public void onServiceAdded(IRenderService service, String name) {
                super.onServiceAdded(service, name);

                if(extension.equals("left")) {
                    service.getComponent().add(toggler = new UnitExpandCollapseBehavior(settings, "left") {

                        @Override
                        protected void onToggle(boolean expand, AjaxRequestTarget target) {
                            if(expand) {
                                //remove focus from tabs
                                if(editorBrowseService != null) {
                                    editorBrowseService.focus(null);
                                }
                                //Remove selected state from doclisting
                                if (documentModelReference != null) {
                                    documentModelReference.setModel(new JcrNodeModel((Node) null));
                                }

                            }
                        }
                    });
                }
            }
        };
    }

}
