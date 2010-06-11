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
package org.hippoecm.frontend.plugins.gallery;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.ImageNodeDragBehavior;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;

public class ImageContainer extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private JcrResourceStream stream;
    private int width;

    public ImageContainer(String wicketId, JcrNodeModel model, IPluginContext pluginContext,
            final IPluginConfig pluginConfig) {
        super(wicketId, model);

        try {
            width = pluginConfig.getInt("gallery.thumbnail.size");
        } catch (StringValueConversionException e) {
            width = 0;
        }

        stream = new JcrResourceStream(model);

        Image img;
        final Time lastModified = stream.lastModifiedTime();

        if(lastModified == null) {
            img = new NonCachingImage("image", new JcrResource(stream)) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);

                    if (width > 0) {
                        tag.put("width", width);
                    }
                }
            };
        } else {
            img = new Image("image", new JcrResource(stream)) {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);

                    if (width > 0) {
                        tag.put("width", width);
                    }

                    String url = tag.getAttributes().getString("src");
                    url = url + ((url.indexOf("?") >= 0) ? "&amp;" : "?");
                    url = url + "w:lm=" + (lastModified.getMilliseconds() / 1000);
                    tag.put("src", url);
                }
            };

        }
        img.add(new ImageNodeDragBehavior(new DragSettings(YuiPluginHelper.getConfig(pluginConfig)), model));
        add(img);
    }

    @Override
    public void onDetach() {
        stream.detach();
        super.onDetach();
    }
}
