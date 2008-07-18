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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.yui.layout.YuiUnitBehavior;
import org.hippoecm.frontend.plugins.yui.layout.YuiWireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.YuiWireframeConfig;

public class TabbedPanel extends AjaxTabbedPanel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private TabsPlugin plugin;

    public TabbedPanel(String wicketId, TabsPlugin plugin, List<TabsPlugin.Tab> tabs) {
        super(wicketId, tabs);
        this.plugin = plugin;
        setOutputMarkupId(true);
        setVersioned(false);
        
        add(new YuiWireframeBehavior("tabbed-panel-layout", true).registerUnitElement("top", "tabbed-panel-layout-top").registerUnitElement("center", "tabbed-panel-layout-center"));
        
        visitChildren(new IVisitor() {
            public Object component(Component component) {
                if(component.getId().equals("tabs-container")) {
                    component.setOutputMarkupId(true);
                    component.add(new YuiUnitBehavior(YuiWireframeConfig.Unit.TOP, "height=30"));
                    return STOP_TRAVERSAL;
                } 
                return CONTINUE_TRAVERSAL;
            }            
        });

    }

    @Override
    protected WebMarkupContainer newLink(String linkId, final int index) {
        assert (linkId.equals("link"));
        WebMarkupContainer container = new WebMarkupContainer("container", new Model(new Integer(index)));
        final TabsPlugin.Tab tabbie = (TabsPlugin.Tab) getTabs().get(index);
        if (tabbie.canClose()) {
            container.add(new AjaxFallbackLink("close") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    plugin.onClose(tabbie, target);
                }
            });
        } else {
            container.add(new Label("close").setVisible(false));
        }
        return container;
    }

    // used by superclass to add title to the container
    @Override
    protected Component newTitle(String titleId, IModel titleModel, final int index) {
        final TabsPlugin.Tab tabbie = (TabsPlugin.Tab) getTabs().get(index);
        return new AjaxFallbackLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onSelect(tabbie, target);
            }
        }.add(new Label(titleId, titleModel));
    }

}
