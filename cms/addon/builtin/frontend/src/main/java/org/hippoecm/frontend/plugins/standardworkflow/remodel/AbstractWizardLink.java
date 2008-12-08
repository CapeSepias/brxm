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
package org.hippoecm.frontend.plugins.standardworkflow.remodel;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.IWizardModel;

public abstract class AbstractWizardLink extends AjaxLink {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final RemodelWizard wizard;

    public AbstractWizardLink(String id, RemodelWizard wizard) {
        super(id);
        this.wizard = wizard;
        setOutputMarkupId(true);
    }

    protected final RemodelWizard getWizard() {
        return wizard;
    }

    protected final IWizardModel getWizardModel() {
        return wizard.getWizardModel();
    }

}
