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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.IdentityHashMap;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IFocusListener;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CmsEditor implements IEditor {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(CmsEditor.class);

    private IPluginContext context;
    private IClusterControl cluster;
    private IRenderService renderer;
    private ModelReference<IModel> modelService;
    private JcrNodeModel model;
    private EditorManagerPlugin manager;
    private IFocusListener focusListener;

    CmsEditor(final EditorManagerPlugin manager, IPluginContext context, String clusterName, IPluginConfig config,
            IModel model) throws CmsEditorException {
        this.manager = manager;
        this.model = (JcrNodeModel) model;
        this.context = context;

        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(clusterName);
        cluster = context.newCluster(clusterConfig, config);
        IClusterConfig decorated = cluster.getClusterConfig();

        String modelId = decorated.getString(RenderService.MODEL_ID);
        modelService = new ModelReference<IModel>(modelId, model);
        modelService.init(context);

        String editorId = decorated.getString("editor.id");
        context.registerService(this, editorId);

        cluster.start();

        renderer = context.getService(decorated.getString(RenderService.WICKET_ID), IRenderService.class);
        if (renderer == null) {
            cluster.stop();
            modelService.destroy();
            throw new CmsEditorException("No IRenderService found");
        }

        String renderId = context.getReference(renderer).getServiceId();

        // attach self to renderer, so that other plugins can close us
        context.registerService(this, renderId);

        // observe focus events, those need to be synchronized with the active model of the editor manager
        focusListener = new IFocusListener() {
            private static final long serialVersionUID = 1L;

            public void onFocus(IRenderService renderService) {
                if (!manager.active) {
                    manager.active = true;
                    manager.setActiveModel(CmsEditor.this.model);
                    manager.active = false;
                }
            }

        };
        context.registerService(focusListener, renderId);
    }

    public IModel getModel() {
        return model;
    }

    public void close() throws EditorException {
        List<IEditorFilter> filters = context.getServices(context.getReference(this).getServiceId(),
                IEditorFilter.class);
        IdentityHashMap<IEditorFilter, Object> filterContexts = new IdentityHashMap<IEditorFilter, Object>();
        for (IEditorFilter filter : filters) {
            Object filterContext = filter.preClose();
            if (filterContext == null) {
                throw new EditorException("Close operation cancelled by filter");
            }
            filterContexts.put(filter, filterContext);
        }

        String renderId = context.getReference(renderer).getServiceId();
        context.unregisterService(focusListener, renderId);
        context.unregisterService(this, renderId);

        cluster.stop();
        modelService.destroy();
        for (IEditorFilter filter : filters) {
            filter.postClose(filterContexts.get(filter));
        }
        context.unregisterService(this, cluster.getClusterConfig().getString("editor.id"));
        manager.unregister(this);
    }

    void focus() {
        if (renderer != null) {
            renderer.focus(null);
        }
    }

}
