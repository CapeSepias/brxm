/*
 * Copyright 2007 Hippo
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
package org.hippoecm.cmsprototype.frontend.plugins.template;

import javax.jcr.Node;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class Template extends Panel {
    private static final long serialVersionUID = 1L;

    private FieldView fields;

    public Template(String wicketId, IModel model, TemplateDescriptor descriptor, TemplateEngine engine) {
        super(wicketId, model);

        TemplateProvider provider = new TemplateProvider(descriptor, (Node) getModelObject(), engine);
        fields = new FieldView("field", descriptor, provider, engine);
        add(fields);
    }
}
