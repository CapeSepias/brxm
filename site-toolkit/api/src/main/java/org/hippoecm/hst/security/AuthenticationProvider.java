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
package org.hippoecm.hst.security;

import java.util.Set;

import javax.security.auth.Subject;

/**
 * AuthenticationProvider
 * <p>
 * Configures an authentication provider.
 * </p>
 * 
 * @version $Id$
 */
public interface AuthenticationProvider {

    /**
     * Authenticate a user.
     * 
     * @param userName The user name.
     * @param password The user password.
     * @return the {@link User}
     */
    User authenticate(String userName, char [] password) throws SecurityException;
    
    /**
     * Authenticate a user.
     * 
     * @param userName The user name.
     * @param password The user password.
     * @param subject The subject given by the container
     * @return the {@link User}
     */
    User authenticate(String userName, char [] password, Subject subject) throws SecurityException;
    
    /**
     * Returns security roles of the given username
     * @param user
     * @return
     */
    Set<Role> getRolesByUsername(String username) throws SecurityException;
    
}
