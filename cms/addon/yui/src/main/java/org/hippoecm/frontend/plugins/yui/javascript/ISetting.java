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
package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public interface ISetting<K> extends IClusterable {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";
    
    void set(K value, Settings settings);
    K get(Settings settings);

    void setFromString(String value, Settings settings);
    void setFromConfig(IPluginConfig config, Settings settings);

    Value<K> newValue();
    
    String getKey();

}
