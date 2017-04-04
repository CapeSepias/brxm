/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.api;

import java.util.EnumSet;
import java.util.Map;

import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cms7.services.SingletonService;

@SingletonService
public interface ConfigurationService {

    void apply(final MergedModel mergedModel,
               final Map<Module, ResourceInputProvider> resourceInputProviders,
               final EnumSet<DefinitionType> includeDefinitionTypes)
            throws Exception;

}
