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
package org.hippoecm.repository.jackrabbit.version;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.version.InternalVersionItem;

/**
 * Implements a <code>InternalVersionItem</code>.
 */
abstract class InternalVersionItemImpl implements InternalVersionItem {

    /**
     * the underlying persistance node
     */
    protected final NodeStateEx node;

    /**
     * the version manager
     */
    protected final AbstractVersionManager vMgr;

    /**
     * Creates a new Internal version item impl
     *
     * @param vMgr
     */
    protected InternalVersionItemImpl(AbstractVersionManager vMgr, NodeStateEx node) {
        this.vMgr = vMgr;
        this.node = node;
    }

    /**
     * Returns the persistent version manager for this item
     *
     * @return the version manager.
     */
    protected AbstractVersionManager getVersionManager() {
        return vMgr;
    }

    /**
     * Returns the id of this item
     *
     * @return the id of this item.
     */
    public abstract NodeId getId();

    /**
     * returns the parent version item or <code>null</code>.
     *
     * @return the parent version item or <code>null</code>.
     */
    public abstract InternalVersionItem getParent();
}
