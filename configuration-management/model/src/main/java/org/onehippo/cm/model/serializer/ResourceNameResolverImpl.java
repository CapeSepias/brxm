/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.serializer;

import java.util.HashSet;
import java.util.Set;

import org.onehippo.cm.model.util.FileConfigurationUtils;

/**
 * Unique file name generator
 */
public class ResourceNameResolverImpl implements ResourceNameResolver {

    private Set<String> knownFileEntries = new HashSet<>();

    /**
     * Generates file unique name and add it to known list along with its full path.
     * @param filePath
     * @return unique filename within already known paths
     */
    @Override
    public String generateName(final String filePath) {
        final String lowerCaseFilePath = filePath.toLowerCase();
        final String generatedPath = FileConfigurationUtils.generateUniquePath(lowerCaseFilePath, knownFileEntries::contains, 0);
        knownFileEntries.add(generatedPath);
        return generatedPath;
    }

}
