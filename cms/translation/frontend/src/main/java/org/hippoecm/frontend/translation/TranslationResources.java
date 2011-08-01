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
package org.hippoecm.frontend.translation;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;

public final class TranslationResources {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private TranslationResources() {
    }

    public static HeaderContributor getTranslationsHeaderContributor() {
        return JavascriptPackageResource.getHeaderContribution(TranslationResources.class, "translations.js");
    }

    public static HeaderContributor getCountriesCss() {
        return CSSPackageResource.getHeaderContribution(TranslationResources.class, "countries.css", true);
    }

}
