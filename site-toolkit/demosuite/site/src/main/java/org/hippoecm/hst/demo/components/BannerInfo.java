/*
 *  Copyright 2010 Hippo.
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

package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParameterType;


public class BannerInfo {

    @Parameter(name = "bannerWidth", displayName = "Banner Width", type = ParameterType.NUMBER)
    private int getBannerWidth() {
        return 0;
    }
    
    @Parameter(name = "yesNo", displayName = "Yes or No ?", type = ParameterType.BOOLEAN)
    private int getYesNO() {
        return 0;
    }

    @Parameter(name = "date", displayName = "Some Date", type = ParameterType.DATE)
    private String getDate() {
        return "";
    }

    @Parameter(name = "borderColor", displayName = "Border Color", type = ParameterType.COLOR)
    private String getColor() {
        return "";
    }

    @Parameter(name = "someName", displayName = "Some String", type = ParameterType.STRING)
    private String getSomeName() {
        return "";
    }

}
