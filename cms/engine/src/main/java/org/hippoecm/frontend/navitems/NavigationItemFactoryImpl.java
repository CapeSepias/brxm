/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.navitems;

import org.apache.wicket.Session;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;

import java.util.MissingResourceException;

public class NavigationItemFactoryImpl implements NavigationItemFactory {

    @Override
    public NavigationItem newInstance(String perspectiveClassName, String appIframeUrl) {
        final NavigationItem navigationItem = new NavigationItem();
        navigationItem.setId(getId(perspectiveClassName));
        navigationItem.setAppIframeUrl(appIframeUrl);
        navigationItem.setAppPath(getId(perspectiveClassName));
        navigationItem.setDisplayName(getDisplayName(perspectiveClassName));
        return navigationItem;
    }

    private String getId(String perspectiveClassName) {
        final int lastDotIndex = perspectiveClassName.lastIndexOf('.');
        final String perspectiveName = perspectiveClassName.substring(1 + lastDotIndex);
        return String.format("hippo-perspective-%s", perspectiveName.toLowerCase());
    }

    private String getDisplayName(String perspectiveClassName) {
        try {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle(perspectiveClassName, Session.get().getLocale());
            if (bundle != null && bundle.containsKey(Perspective.TITLE_KEY)) {
                return bundle.getString(Perspective.TITLE_KEY);
            }
        } catch(MissingResourceException ignored) {
            // perspective-title and resource bundle are optional. Ignore if the resource bundle doesn't exist
        }
        return null;
    }
}
