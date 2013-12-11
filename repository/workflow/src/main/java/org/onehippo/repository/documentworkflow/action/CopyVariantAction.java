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
import org.onehippo.repository.documentworkflow.task.CopyVariantTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * CopyVariantAction delegating the execution to CopyVariantTask.
 */
public class CopyVariantAction extends AbstractTaskAction<CopyVariantTask> {

    private static final long serialVersionUID = 1L;

    public String getSourceState() {
        return getWorkflowTask().getSourceState();
    }

    public void setSourceState(String sourceState) {
        getWorkflowTask().setSourceState(sourceState);
    }

    public String getTargetState() {
        return getWorkflowTask().getTargetState();
    }

    public void setTargetState(String targetState) {
        getWorkflowTask().setTargetState(targetState);
    }

    public String getAvailabilities() {
        return getWorkflowTask().getAvailabilities();
    }

    public void setAvailabilities(String availabilities) {
        getWorkflowTask().setAvailabilities(availabilities);
    }

    public boolean isApplyModified() {
        return getWorkflowTask().isApplyModified();
    }

    public void setApplyModified(String applyModified) {
        getWorkflowTask().setApplyModified(applyModified);
    }

    public boolean isSkipIndex() {
        return getWorkflowTask().isSkipIndex();
    }

    public void setSkipIndex(String skipIndex) {
        getWorkflowTask().setSkipIndex(skipIndex);
    }

    public boolean isVersionable() {
        return getWorkflowTask().isVersionable();
    }

    public void setVersionable(String versionable) {
        getWorkflowTask().setVersionable(versionable);
    }

    @Override
    protected CopyVariantTask createWorkflowTask() {
        return new CopyVariantTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(Map<String, Object> properties) {
        super.initTaskBeforeEvaluation(properties);
        getWorkflowTask().setWorkflowContext((WorkflowContext) getContextAttribute("workflowContext"));
        DocumentHandle dm = getContextAttribute("dm");
        getWorkflowTask().setDataModel(dm);
    }

}
