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
package org.hippoecm.frontend.plugin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.INotificationListener;
import org.hippoecm.frontend.plugin.channel.IRequestHandler;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Plugin extends Panel implements INotificationListener, IRequestHandler {

    static final Logger log = LoggerFactory.getLogger(Plugin.class);

    private PluginManager pluginManager;
    private PluginDescriptor pluginDescriptor;
    private Plugin parentPlugin;
    private Channel topChannel;
    private Channel bottomChannel;

    public Plugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor.getWicketId(), model);
        setOutputMarkupId(true);

        this.parentPlugin = parentPlugin;
        this.pluginDescriptor = pluginDescriptor;

        if(parentPlugin!=null) {
            topChannel = parentPlugin.getBottomChannel();
            if(topChannel != null) {
                topChannel.subscribe(this);
            }
            bottomChannel = parentPlugin.getPluginManager().getChannelFactory().createChannel();
            bottomChannel.register(this);
        }
    }

    public void destroy() {
        onDestroy();

        // disconnect bottom and top channels
        if (topChannel != null) {
            topChannel.unsubscribe(this);
        }
        if (bottomChannel != null) {
            Notification notification = bottomChannel.createNotification("destroy", null);
            bottomChannel.publish(notification);
            bottomChannel.unregister(this);
        }
    }

    protected void onDestroy() {
    }

    public PluginDescriptor getDescriptor() {
        return pluginDescriptor;
    }

    public PluginManager getPluginManager() {
        if (pluginManager == null) {
            return parentPlugin.getPluginManager();
        }
        return pluginManager;
    }

    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Plugin getParentPlugin() {
        return parentPlugin;
    }

    public IPluginModel getPluginModel() {
        return (IPluginModel) getModel();
    }

    public void setPluginModel(IPluginModel model) {
        setModel(model);
    }

    public void addChildren() {
        List<PluginDescriptor> children = getDescriptor().getChildren();
        Iterator<PluginDescriptor> it = children.iterator();
        while (it.hasNext()) {
            PluginDescriptor childDescriptor = it.next();
            Plugin child = addChild(childDescriptor);
            if (child != null) {
                child.addChildren();
            }
        }
    }

    public Plugin addChild(PluginDescriptor childDescriptor) {
        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        Plugin child = pluginFactory.createPlugin(childDescriptor, getPluginModel(), this);

        add(child);
        return child;
    }

    public void removeChild(PluginDescriptor childDescriptor) {
        remove(childDescriptor.getWicketId());
    }

    @Override
    public void remove(Component component) {
        if (component instanceof Plugin) {
            ((Plugin) component).destroy();
        }
        super.remove(component);
    }

    // channels

    public Channel getTopChannel() {
        return topChannel;
    }

    public Channel getBottomChannel() {
        return bottomChannel;
    }

    // implement INotificationListener

    public void receive(Notification notification) {
        // forward the notification to children
        Channel outgoing = getBottomChannel();
        if (outgoing != null) {
            outgoing.publish(notification);
        }
        if ("destroy".equals(notification.getOperation())) {
            onDestroy();
        }
    }

    // implement IRequestHandler

    public void handle(Request request) {
        // forward the request
        Channel channel = getTopChannel();
        if (channel != null) {
            channel.send(request);
        }
    }
}
