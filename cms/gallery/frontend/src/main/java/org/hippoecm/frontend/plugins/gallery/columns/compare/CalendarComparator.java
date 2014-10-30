/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns.compare;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Calendar;

public class CalendarComparator extends PropertyComparator {

    public CalendarComparator(String prop) {
        super(prop);
    }

    public CalendarComparator(String prop, String relPath) {
        super(prop, relPath);
    }

    @Override
    protected int compare(Property p1, Property p2) {
        try {
            Calendar c1 = p1 == null ? null : p1.getDate();
            Calendar c2 = p2 == null ? null : p2.getDate();
            if (c1 == null) {
                if (c2 == null) {
                    return 0;
                }
                return 1;
            } else if (c2 == null) {
                return -1;
            }
            return c1.compareTo(c2);
        } catch (RepositoryException e) {
        }
        return 0;
    }
}
