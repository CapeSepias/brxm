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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import javax.jcr.Session;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.server.ServerRepositoryService;
import org.apache.jackrabbit.spi2jcr.BatchReadConfig;
import org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl;
import org.hippoecm.repository.decorating.remote.RemoteRepository;

public class ServerRepository extends org.apache.jackrabbit.rmi.server.ServerRepository implements RemoteRepository {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ServerQuery.java 12563 2008-07-15 20:21:22Z wgrevink $";
    private Repository repository;
    private RemoteServicingAdapterFactory factory;

    public ServerRepository(Repository repository, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(repository, factory);
        this.repository = repository;
        this.factory = factory;
    }

    public RemoteRepositoryService getRepositoryService() throws RepositoryException, RemoteException {
        final ServerRepositoryService serverService = new ServerRepositoryService();
        BatchReadConfig cfg = new BatchReadConfig();
        cfg.setDepth(NameFactoryImpl.getInstance().create("internal", "root"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.jcp.org/jcr/nt/1.0", "unstructured"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("nt", "unstructured"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("hippostd", "folder"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("hippostd", "directory"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/hippostd/nt/1.2", "folder"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/hippostd/nt/1.2", "directory"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("defaultcontent", "article"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/defaultcontent/1.2", "article"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("hippo", "document"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/nt/1.2", "document"), 3);

        Repository loginRepository = new Repository() {

            public Session login() throws LoginException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login();
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }

                return newSession;
            }

            public Session login(String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login(workspace);
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }

                return newSession;
            }

            public Session login(Credentials credentials) throws LoginException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login(credentials);
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }

                return newSession;
            }

            public Session login(Credentials credentials, String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login(credentials, workspace);
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }
                return newSession;
            }

            public String getDescriptor(String name) {
                return ServerRepository.this.repository.getDescriptor(name);
            }

            public String[] getDescriptorKeys() {
                return ServerRepository.this.repository.getDescriptorKeys();
            }
        };
        RepositoryService repositoryService = new RepositoryServiceImpl(loginRepository, cfg);
        serverService.setRepositoryService(repositoryService);
        return serverService;
    }
}
