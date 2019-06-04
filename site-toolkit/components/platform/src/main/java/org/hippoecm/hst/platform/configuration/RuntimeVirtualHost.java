/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.platform.model.HstModelImpl.RuntimeHostConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeVirtualHost extends GenericVirtualHostWrapper {

    private static final Logger log = LoggerFactory.getLogger(RuntimeVirtualHost.class);

    private final VirtualHost delegatee;
    private final String hostName;
    private final String name;
    private final String hostGroupName;
    private final VirtualHost child;
    private final PortMount portMount;
    private final boolean isPortInUrl;
    private final String scheme;

    public RuntimeVirtualHost(final VirtualHost delegatee, final RuntimeHostConfiguration runtimeHostConfiguration) {
        this(delegatee, runtimeHostConfiguration, StringUtils.substringBefore(runtimeHostConfiguration.getHostName(), ":").split("\\."), runtimeHostConfiguration.getTargetHostGroupName());
    }

    private RuntimeVirtualHost(final VirtualHost delegatee, final RuntimeHostConfiguration runtimeHostConfiguration, final String[] hostNameSegments, final String hostGroupName) {
        this(delegatee, runtimeHostConfiguration, "", hostNameSegments, hostNameSegments.length - 1, hostGroupName);
    }

    public RuntimeVirtualHost(final VirtualHost delegatee, final RuntimeHostConfiguration runtimeHostConfiguration, final String hostNamePrefix, final String[] hostNameSegments, final int position, final String hostGroupName) {
        super(delegatee);
        this.delegatee = delegatee;
        if (hostNamePrefix.length() == 0) {
            hostName = hostNameSegments[position] + hostNamePrefix;
        } else {
            hostName = hostNameSegments[position] + "." + hostNamePrefix;
        }
        this.hostGroupName = hostGroupName;
        name = hostNameSegments[position];

        if (position > 0) {
            child = new RuntimeVirtualHost(delegatee, runtimeHostConfiguration, hostName, hostNameSegments, position - 1, hostGroupName);
            portMount = null;
            isPortInUrl = false;
            scheme = null;
        } else {
            child = null;
            // we can use '0' since we never really have port mounts, and '0' is the default catch all port
            final PortMount delegateePortMount = delegatee.getPortMount(0);

            final Mount rootMount = delegateePortMount.getRootMount();

            final int portNumber = (runtimeHostConfiguration.getPortNumber() != null) ? runtimeHostConfiguration.getPortNumber() : delegateePortMount.getPortNumber();
            isPortInUrl = runtimeHostConfiguration.isPortInUrl();
            scheme = runtimeHostConfiguration.getScheme();

            final RuntimeMount runtimeMount;
            if (rootMount instanceof ContextualizableMount) {
                runtimeMount = new RuntimeContextualizableMount((ContextualizableMount)rootMount, RuntimeVirtualHost.this);
            } else {
                runtimeMount = new RuntimeMount(rootMount, RuntimeVirtualHost.this);
            }

            this.portMount = new PortMount() {
                @Override
                public int getPortNumber() {
                    return portNumber;
                }

                @Override
                public Mount getRootMount() {
                    return runtimeMount;
                }
            };
            log.debug("Runtime virtual host {} is created with scheme {} and port {} for host {} with source {} and target {}.",
                    name, scheme, portNumber, runtimeHostConfiguration.getHostName(),
                    runtimeHostConfiguration.getSourceHostGroupName(),
                    runtimeHostConfiguration.getTargetHostGroupName());
        }
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public PortMount getPortMount(final int portNumber) {
        return portMount;
    }

    @Override
    public boolean isPortInUrl() {
        return isPortInUrl;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public VirtualHost getChildHost(final String name) {
        if (child != null && child.getName().equals(name)) {
            return child;
        }
        return null;
    }

    @Override
    public List<VirtualHost> getChildHosts() {
        return child == null ? Collections.emptyList() : Collections.singletonList(child);
    }

    @Override
    public String toString() {
        return "RuntimeVirtualHost{" + "delegatee=" + delegatee + '}';
    }
}
