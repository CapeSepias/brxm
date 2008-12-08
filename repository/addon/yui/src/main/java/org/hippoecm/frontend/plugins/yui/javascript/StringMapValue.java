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

import java.util.Map;
import java.util.Map.Entry;

public class StringMapValue extends Value<Map<String, String>> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean escaped = true;//TODO: make configurable

    public StringMapValue(Map<String, String> value) {
        super(value);
    }

    public String getScriptValue() {
        if(value == null)  {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Entry<String, String> e : value.entrySet()) {
            //TODO: A IPluginConfig map can be passed into this method, which will results in a jcr:primaryType key-value entry, which breaks
            //the js-object and shouldn't be present. We could just try and ignore it by wrapping the js-object key's with quotes as well.
            if (e.getKey().startsWith("jcr:"))
                continue;
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(e.getKey()).append(':');
            if (escaped) {
                buf.append(StringValue.escapeString(e.getValue()));
            } else {
                buf.append(e.getValue());
            }
        }
        buf.insert(0, '{').append('}');
        return buf.toString();
    }

}
