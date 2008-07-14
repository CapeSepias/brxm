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
package org.hippoecm.repository.reviewedactions;

import java.util.Date;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;

public interface FullReviewedActionsWorkflow extends BasicReviewedActionsWorkflow {
    final static String SVN_ID = "$Id$";

    /**
     * Immediate unpublication and deletion of document.
     * The current user must have authorization for this.
     */
    public void delete()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication and deletion of document.
     */
    public void requestDeletion()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication.
     * The current user must have authorization for this.
     */
    public void depublish()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate publication.
     * The current user must have authorization for this.
     */
    public void publish()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date.
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date, and depublish at the requested second date
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate, Date unpublicationDate)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
