/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.internal.InternalHstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public class RuntimeHstSiteMapItem extends GenericHstSiteMapItemWrapper {

    private final HstSiteMapItem delegatee;
    private final HstSiteMap hstSiteMap;
    private final HstSiteMapItem parentItem;
    private final Map<String, HstSiteMapItem> childrenSiteMapItems = new HashMap<>();
    private final Map<String, InternalHstSiteMapItem> wildCardChildrenSiteMapItems = new HashMap<>();
    private final Map<String, InternalHstSiteMapItem> anyChildrenSiteMapItems = new HashMap<>();
    private final List<HstSiteMapItem> siteMapItems;
    private final List<InternalHstSiteMapItem> wildCardChildSiteMapItems;
    private final List<InternalHstSiteMapItem> anyChildSiteMapItems;
    private final String scheme;

    public RuntimeHstSiteMapItem(final InternalHstSiteMapItem delegatee, final RuntimeHstSiteMap hstSiteMap,
            final RuntimeHstSiteMapItem parentHstSiteMapItem, final String scheme) {
        super(delegatee);
        this.delegatee = delegatee;
        this.hstSiteMap = hstSiteMap;
        this.scheme = scheme;

        delegatee.getChildren().forEach(child -> {
            RuntimeHstSiteMapItem runtimeHstSiteMapItem = new RuntimeHstSiteMapItem((InternalHstSiteMapItem) child,
                    hstSiteMap, RuntimeHstSiteMapItem.this, scheme);
            childrenSiteMapItems.put(runtimeHstSiteMapItem.getValue(), runtimeHstSiteMapItem);
        });

        delegatee.getWildCardChildSiteMapItems().forEach(child -> {
            RuntimeHstSiteMapItem runtimeHstSiteMapItem = new RuntimeHstSiteMapItem((InternalHstSiteMapItem) child,
                    hstSiteMap, RuntimeHstSiteMapItem.this, scheme);
            wildCardChildrenSiteMapItems.put(runtimeHstSiteMapItem.getValue(), runtimeHstSiteMapItem);
        });

        delegatee.getAnyChildSiteMapItems().forEach(child -> {
            RuntimeHstSiteMapItem runtimeHstSiteMapItem = new RuntimeHstSiteMapItem((InternalHstSiteMapItem) child,
                    hstSiteMap, RuntimeHstSiteMapItem.this, scheme);
            anyChildrenSiteMapItems.put(runtimeHstSiteMapItem.getValue(), runtimeHstSiteMapItem);
        });

        if (delegatee.getParentItem() != null && parentHstSiteMapItem != null) {
            parentItem = parentHstSiteMapItem;
        } else {
            parentItem = null;
        }

        siteMapItems = unmodifiableList(new ArrayList<>(childrenSiteMapItems.values()));
        wildCardChildSiteMapItems = unmodifiableList(new ArrayList<>(wildCardChildrenSiteMapItems.values()));
        anyChildSiteMapItems = unmodifiableList(new ArrayList<>(anyChildrenSiteMapItems.values()));
    }

    @Override
    public List<HstSiteMapItem> getChildren() {
        return siteMapItems;
    }

    @Override
    public HstSiteMapItem getChild(String value) {
        return childrenSiteMapItems.get(value);
    }

    @Override
    public List<InternalHstSiteMapItem> getWildCardChildSiteMapItems() {
        return wildCardChildSiteMapItems;
    }

    @Override
    public List<InternalHstSiteMapItem> getAnyChildSiteMapItems() {
        return anyChildSiteMapItems;
    }

    @Override
    public HstSiteMapItem getParentItem() {
        return parentItem;
    }

    @Override
    public HstSiteMap getHstSiteMap() {
        return hstSiteMap;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String toString() {
        return "RuntimeHstSiteMapItem{" + "delegatee=" + delegatee + '}';
    }

}
