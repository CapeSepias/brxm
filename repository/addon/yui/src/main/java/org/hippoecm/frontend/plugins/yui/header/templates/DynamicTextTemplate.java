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

package org.hippoecm.frontend.plugins.yui.header.templates;

import java.util.Map;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.plugins.yui.javascript.Settings;

public abstract class DynamicTextTemplate implements IHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private TextTemplateHeaderContributor headerContributor;
    private Map<String, Object> variables;

    public DynamicTextTemplate(Class<?> clazz, String filename) {
        this(new PackagedTextTemplate(clazz, filename));
    }

    public DynamicTextTemplate(PackagedTextTemplate template) {
        headerContributor = TextTemplateHeaderContributor.forJavaScript(template, new DynamicReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            Map<String, Object> getVariables() {
                return DynamicTextTemplate.this.getVariables();
            }
        });
    }

    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
    }

    Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new MiniMap(5);
        }
        variables.put("config", getSettings().toScript());
        return variables;
    }
    
    abstract public Settings getSettings();


    private static abstract class DynamicReadOnlyModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;

        @Override
        public Object getObject() {
            return getVariables();
        }

        abstract Map<String, Object> getVariables();
    }
}
