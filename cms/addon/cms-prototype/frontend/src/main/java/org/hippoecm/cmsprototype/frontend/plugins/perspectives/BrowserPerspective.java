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
package org.hippoecm.cmsprototype.frontend.plugins.perspectives;

import javax.jcr.Node;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;

public class BrowserPerspective extends Plugin {
    private static final long serialVersionUID = 1L;

    public BrowserPerspective(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    public void receive(Notification notification) {
        if ("browse".equals(notification.getOperation())) {
            Channel incoming = getDescriptor().getIncoming();
            if (incoming != null) {
                // FIXME: should the map be constructed by the PluginDescriptor?
                PluginModel model = new PluginModel();
                model.put("plugin", getDescriptor().getPluginId());
                Request request = incoming.createRequest("focus", model);
                request.setContext(notification.getContext());
                incoming.send(request);
            }

            Channel outgoing = getDescriptor().getOutgoing();
            if (outgoing != null) {
                Notification selectNotice = outgoing.createNotification("select", notification.getModel());
                selectNotice.setContext(notification.getContext());
                outgoing.publish(selectNotice);
            }
            return;
        }
        super.receive(notification);
    }

    @Override
    public void handle(Request request) {
        if ("select".equals(request.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(request.getModel());
            Node node = model.getNode();
            if (node != null) {
                Channel outgoing = getDescriptor().getOutgoing();
                if (outgoing != null) {
                    outgoing.publish(outgoing.createNotification(request));
                }
            }
            return;
        }
        super.handle(request);
    }
}
