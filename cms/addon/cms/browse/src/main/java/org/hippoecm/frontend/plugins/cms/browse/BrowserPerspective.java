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

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;

public class BrowserPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public BrowserPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        addExtensionPoint("breadcrumbPlugin");
        addExtensionPoint("browserPlugin");
        addExtensionPoint("listPlugin");
        addExtensionPoint("workflowsPlugin");
        addExtensionPoint("versionPlugin");
        addExtensionPoint("versionTogglePlugin");
    }

    @Override
    public void view(IModel model) {
        super.view(model);
        focus(null);
    }
}
