/*
 * Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.onehippo.forge.polldemo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="polldemo:textdocument")
public class TextDocument extends BaseDocument{
    
    public HippoHtml getHtml(){
        return getHippoHtml("polldemo:body");    
    }

    public String getSummary() {
        return getProperty("polldemo:summary");
    }
 
    public String getTitle() {
        return getProperty("polldemo:title");
    }

}
