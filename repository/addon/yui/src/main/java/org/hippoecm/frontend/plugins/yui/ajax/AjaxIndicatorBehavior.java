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
package org.hippoecm.frontend.plugins.yui.ajax;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.extensions.ajax.markup.html.WicketAjaxIndicatorAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;

public class AjaxIndicatorBehavior extends AbstractBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    final private WicketAjaxIndicatorAppender ajaxIndicator;
    
    public AjaxIndicatorBehavior() {
        ajaxIndicator = new WicketAjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getIndicatorUrl() {
                return RequestCycle.get().urlFor(new ResourceReference(AjaxIndicatorBehavior.class, "ajax-loader.gif"));
            }
        };
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        YuiHeaderContributor.forModule(HippoNamespace.NS, "ajaxindicator").renderHead(response);
        TextTemplateHeaderContributor.forJavaScript(AjaxIndicatorBehavior.class,
                "init_ajax_indicator.js", new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    private Map<String, Object> variables;

                    @Override
                    public Object getObject() {
                        if (variables == null) {
                            variables = new MiniMap(1);
                            variables.put("id", ajaxIndicator.getMarkupId());
                        }
                        return variables;
                    }
                }).renderHead(response);


        super.renderHead(response);
    }

    @Override
    public void bind(Component component) {
        component.add(ajaxIndicator);
        super.bind(component);
    }

}
