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
package org.hippoecm.frontend.editor.viewer;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.impl.TemplateEngine;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeStore;
import org.hippoecm.frontend.types.JcrTypeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewerPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ViewerPlugin.class);

    private ModelService modelService;
    private IClusterControl cluster;
    private String engineId;
    private ITypeStore typeStore;
    private TemplateEngine engine;

    public ViewerPlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        typeStore = new JcrTypeStore();
        engine = new TemplateEngine(context, typeStore);
        context.registerService(engine, ITemplateEngine.class.getName());
        engineId = context.getReference(engine).getServiceId();

        addExtensionPoint("template");

        modelChanged();
    }

    @Override
    public void onDetach() {
        typeStore.detach();
        super.onDetach();
    }

    @Override
    public void onModelChanged() {
        if (cluster != null) {
            modelService.destroy();
            modelService = null;
            cluster.stop();
            cluster = null;
        }
        createTemplate();
        redraw();
    }

    protected void createTemplate() {
        IModel model = getModel();
        if (model != null && model.getObject() != null) {
            ITypeDescriptor type = engine.getType(model);
            IPluginContext context = getPluginContext();

            if (type != null) {
                IClusterConfig template = engine.getTemplate(type, "view");
                if (template != null) {
                    IPluginConfig parameters = new JavaPluginConfig();
                    parameters.put(RenderService.WICKET_ID, getPluginConfig().getString("template"));
                    parameters.put(ITemplateEngine.ENGINE, engineId);

                    cluster = context.newCluster(template, parameters);
                    String modelId = cluster.getClusterConfig().getString(RenderService.MODEL_ID);
                    if (modelId != null) {
                        modelService = new ModelService(modelId, model);
                        modelService.init(context);
                        cluster.start();
                    } else {
                        log.warn("No model specified in template for type " + type.getName());
                    }
                }
            }
        }
    }

}
