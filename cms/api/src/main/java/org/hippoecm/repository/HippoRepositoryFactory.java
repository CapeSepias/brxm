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
package org.hippoecm.repository;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.jcr.RepositoryException;

public class HippoRepositoryFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static String defaultLocation = null; // FIXME: should become: "java:comp/env/jcr/repository";
    private static HippoRepository defaultRepository = null;

    public static void setDefaultRepository(String location) {
        if(location == null || !location.equals(defaultLocation)) {
            defaultLocation = location;
            defaultRepository = null;
        }
    }

    public static void setDefaultRepository(HippoRepository repository) {
        defaultLocation = null;
        defaultRepository = repository;
    }

    private HippoRepositoryFactory() {
    }

    public static HippoRepository getHippoRepository() throws RepositoryException {
        if (defaultRepository != null) {
            return defaultRepository;
        }
        if (defaultLocation != null) {
            return getHippoRepository(defaultLocation);
        }
        try {
            defaultRepository = (HippoRepository) Class.forName("org.hippoecm.repository.LocalHippoRepository").newInstance();
        } catch(ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch(InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch(IllegalAccessException ex) {
            throw new RepositoryException(ex);
        }
        return defaultRepository;
    }

    public static HippoRepository getHippoRepository(String location) throws RepositoryException {
        HippoRepository repository = null;

        if (location.startsWith("file:")) {
            location = location.substring("file:".length());
        }

        if (defaultRepository != null && (location.equals(defaultRepository.getLocation()) ||
                                          (defaultLocation != null && location.equals(defaultLocation)))) {
            return defaultRepository;
        }

        if (location.startsWith("rmi://")) {
            try {
                defaultLocation = location;
                try {
                    if(!location.endsWith("/spi")) {
                        return (HippoRepository) Class.forName("org.hippoecm.repository.RemoteHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
                    } else {
                        return (HippoRepository) Class.forName("org.hippoecm.repository.SPIHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
                    }
                } catch(ClassNotFoundException ex) {
                    throw new RepositoryException(ex);
                } catch(NoSuchMethodException ex) {
                    throw new RepositoryException(ex);
                } catch(IllegalAccessException ex) {
                    throw new RepositoryException(ex);
                } catch(InvocationTargetException ex) {
                    if(ex.getCause() instanceof RemoteException) {
                        throw (RemoteException) ex.getCause();
                    } else if(ex.getCause() instanceof NotBoundException) {
                        throw (NotBoundException) ex.getCause();
                    } else if(ex.getCause() instanceof MalformedURLException) {
                        throw (MalformedURLException) ex.getCause();
                    } else if(ex.getCause() instanceof RepositoryException) {
                        throw (RepositoryException) ex.getCause();
                    } else {
                        throw new RepositoryException("unchecked exception: "+ex.getCause().getMessage(), ex);
                    }
                }
            } catch (RemoteException ex) {
                throw new RepositoryException("Unable to connect to repository", ex);
            } catch (NotBoundException ex) {
                throw new RepositoryException("Unable to find remote repository", ex);
            } catch (MalformedURLException ex) {
                throw new RepositoryException("Unable to locate remote repository", ex);
            }
        }

        if(location.startsWith("java:")) {
            try {
                defaultLocation = location;
                InitialContext ctx = new InitialContext();
                return (HippoRepository) ctx.lookup(location);
            } catch (NamingException ex) {
                return null;
                // FIXME
            }
        }

        if(location.startsWith("bootstrap:")) {
            try {
                defaultLocation = location;
                location = location.substring("bootstrap:".length());
                return (HippoRepository) Class.forName("org.hippoecm.repository.BootstrapHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
            } catch(ClassNotFoundException ex) {
                throw new RepositoryException(ex);
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException(ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException(ex);
            } catch(InvocationTargetException ex) {
                if (ex.getCause() instanceof RepositoryException) {
                    throw (RepositoryException) ex.getCause();
                } else if (ex.getCause() instanceof IllegalArgumentException) {
                    throw new RepositoryException("Invalid data: " + ex.getCause());
                } else {
                    throw new RepositoryException("unchecked exception: " + ex.getMessage());
                }
            }
        }
 
        if(location.startsWith("vm:")) {
            try {
                defaultLocation = location;
                return (HippoRepository) Class.forName("org.hippoecm.repository.VMHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
            } catch(ClassNotFoundException ex) {
                throw new RepositoryException(ex);
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException(ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException(ex);
            } catch(InvocationTargetException ex) {
                if (ex.getCause() instanceof RepositoryException) {
                    throw (RepositoryException) ex.getCause();
                } else if (ex.getCause() instanceof IllegalArgumentException) {
                    throw new RepositoryException("Invalid data: " + ex.getCause());
                } else {
                    throw new RepositoryException("unchecked exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

        // embedded/local with location
        try {
            repository = (HippoRepository) Class.forName("org.hippoecm.repository.LocalHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
        } catch(ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch(NoSuchMethodException ex) {
            throw new RepositoryException(ex);
        } catch(IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch(InvocationTargetException ex) {
            if (ex.getCause() instanceof RepositoryException) {
                throw (RepositoryException) ex.getCause();
            } else if (ex.getCause() instanceof IllegalArgumentException) {
                throw new RepositoryException("Invalid data: " + ex.getCause());
            } else {
                throw new RepositoryException("unchecked exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        // in case this local repository is build from the default location
        if (defaultRepository == null && location.equals(defaultLocation)) {
            defaultRepository = repository;
        }

        return repository;
    }

    static void unregister(HippoRepository repository) {
        if (repository == defaultRepository) {
            defaultRepository = null;
        }
    }

    public static URL getManifest(Class clazz) {
        try {
            StringBuffer sb = new StringBuffer();
            String[] classElements = clazz.getName().split("\\.");
            for (int i=0; i<classElements.length-1; i++) {
                sb.append("../");
            }
            sb.append("META-INF/MANIFEST.MF");
            URL classResource = clazz.getResource(classElements[classElements.length-1]+".class");
            if (classResource != null) {
                return new URL(classResource, new String(sb));
            }
        } catch (MalformedURLException ex) {
        }
        return null;
    }
}
