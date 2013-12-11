/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow.action;

import java.util.Map;

import org.hippoecm.repository.api.WorkflowContext;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.RequestTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * RequestAction delegating the execution to RequestTask.
 */
public class RequestAction extends AbstractTaskAction<RequestTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getWorkflowTask().getType();
    }

    public void setType(String type) {
        getWorkflowTask().setType(type);
    }

    public String getContextVariantExpr() {
        return (String) getProperties().get("contextVariant");
    }

    public void setContextVariantExpr(String contextVariantExpr) {
        getProperties().put("contextVariant", contextVariantExpr);
    }

    public String getTargetDateExpr() {
        return (String) getProperties().get("targetDate");
    }

    public void setTargetDateExpr(String targetDateExpr) {
        getProperties().put("targetDate", targetDateExpr);
    }

    @Override
    protected RequestTask createWorkflowTask() {
        return new RequestTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(Map<String, Object> properties) {
        super.initTaskBeforeEvaluation(properties);
        getWorkflowTask().setWorkflowContext((WorkflowContext) getContextAttribute("workflowContext"));
        DocumentHandle dm = getContextAttribute("dm");
        getWorkflowTask().setDataModel(dm);
    }

}
