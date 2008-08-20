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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IOverridable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class JavaClusterConfig extends JavaPluginConfig implements IClusterConfig, IOverridable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private List<IPluginConfig> configs;
    private List<String> overrides;

    public JavaClusterConfig() {
        configs = new LinkedList<IPluginConfig>();
        overrides = new LinkedList<String>();
    }

    public void addPlugin(IPluginConfig config) {
        configs.add(config);
    }

    public List<IPluginConfig> getPlugins() {
        return configs;
    }

    public void addOverride(String key) {
        overrides.add(key);
    }

    public List<String> getOverrides() {
        return overrides;
    }

    public Set<IPluginConfig> getPluginConfigSet() {
        return new TreeSet(configs);
    }
    
    public IPluginConfig getPluginConfig(Object key) {
        return new JavaPluginConfig();
    }
}
