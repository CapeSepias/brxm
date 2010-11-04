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
package org.hippoecm.frontend.plugins.cms.browse.list;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeRenderer;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableBehavior;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableSettings;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;

public class DocumentListingPlugin extends ExpandCollapseListingPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentListingPlugin.class);

    private static final String DOCUMENT_LISTING_CSS = "DocumentListingPlugin.css";

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(BrowserStyle.getStyleSheet());

        setClassName("hippo-list-documents");

    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        container.getHeaderResponse().renderCSSReference(new ResourceReference(DocumentListingPlugin.class, DOCUMENT_LISTING_CSS));
    }

    protected List<ListColumn<Node>> getCollapsedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        //Type Icon
        ListColumn<Node> column = new ListColumn<Node>(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        column.setCssClass("doclisting-icon");
        columns.add(column);

        //Name
        column = new ListColumn<Node>(new StringResourceModel("doclisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        column.setCssClass("doclisting-name");
        columns.add(column);

        return columns;
    }

    protected List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getCollapsedColumns();

        //Type
        ListColumn<Node> column = new ListColumn<Node>(new StringResourceModel("doclisting-type", this, null), "type");
        column.setComparator(new TypeComparator());
        column.setRenderer(new TypeRenderer());
        column.setCssClass("doclisting-type");
        columns.add(column);

        return columns;
    }

    @Override
    protected WidgetBehavior getBehavior() {
        DataTableSettings settings = new DataTableSettings();
        settings.setAutoWidthClassName("doclisting-name");
        return new DataTableBehavior(settings);
    }
}
