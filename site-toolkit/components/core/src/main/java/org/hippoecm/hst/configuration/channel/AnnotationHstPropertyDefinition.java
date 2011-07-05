/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.configuration.components.HstValueType;
import org.hippoecm.hst.configuration.components.Parameter;

public class AnnotationHstPropertyDefinition extends AbstractHstPropertyDefinition {

    private List<Annotation> annotations = new ArrayList<Annotation>();

    public AnnotationHstPropertyDefinition(Parameter propAnnotation, Class<?> returnType, Annotation[] annotations) {
        super(propAnnotation.name());

        type = getHstType(returnType);
        required = propAnnotation.required();
        defaultValue = propAnnotation.defaultValue();

        for (Annotation annotation : annotations) {
            if (annotation == propAnnotation) {
                continue;
            }
            this.annotations.add(annotation);
        }
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    private static HstValueType getHstType(Class type) {
        if (type == String.class) {
            return HstValueType.STRING;
        } else if (type == Boolean.class) {
            return HstValueType.BOOLEAN;
        } else if (type == Long.class || type == Integer.class) {
            return HstValueType.INTEGER;
        } else if (type == Calendar.class) {
            return HstValueType.DATE;
        } else if (type == Double.class) {
            return HstValueType.DOUBLE;
        }
        throw new ClassCastException("Could not cast " + type + " to any of the primitive types");
    }
}
