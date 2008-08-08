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
package org.hippoecm.frontend.editor.editor;

import java.util.List;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorPlugin extends RenderPlugin implements IValidateService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditorPlugin.class);

    private EditorForm form;
    private FeedbackPanel feedback;

    public EditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(form = newForm());

        feedback = new FeedbackPanel("feedback", new IFeedbackMessageFilter() {
            private static final long serialVersionUID = 1L;

            public boolean accept(FeedbackMessage message) {
                if (config.getString(RenderService.FEEDBACK) != null) {
                    List<IFeedbackMessageFilter> filters = context.getServices(config.getString(RenderService.FEEDBACK),
                            IFeedbackMessageFilter.class);
                    for (IFeedbackMessageFilter filter : filters) {
                        if (filter.accept(message)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        feedback.setOutputMarkupId(true);
        add(feedback);

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.info("No validator id {} specified", IValidateService.VALIDATE_ID);
        }
    }

    @Override
    public void onModelChanged() {
        form.destroy();
        replace(form = newForm());
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        target.addComponent(feedback);
        if (form != null) {
            form.render(target);
        }
    }

    protected EditorForm newForm() {
        return new EditorForm("form", (JcrNodeModel) getModel(), this, getPluginContext(), getPluginConfig());
    }

    public boolean hasError() {
        return form.hasError();
    }

    public void validate() {
        form.validate();
    }

}
