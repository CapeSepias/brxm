/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.onehippo.cms7.essentials.dashboard.utils.inject.PropertiesModule;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id$"
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {ApplicationModule.class, PropertiesModule.class})
public abstract class BaseTest {


    public static final String HIPPOPLUGINS_NAMESPACE = "hippoplugins";
    public static final String PROJECT_NAMESPACE_TEST = "testnamespace";
    public static final String PROJECT_NAMESPACE_TEST_URI = "http://www.onehippo.org/tesnamespace/nt/1.0";
    public static final Set<String> NAMESPACES_TEST_SET = new ImmutableSet.Builder<String>()
            .add("hippoplugins:extendingnews")
            .add("myproject:newsdocument")
            .add("hippoplugins:extendedbase")
            .add("hippoplugins:textdocument")
            .add("hippoplugins:basedocument")
            .add("hippoplugins:plugin")
            .add("hippoplugins:vendor")
            .add("hippoplugins:newsdocument")
            .add("hippoplugins:version")
            .add("hippoplugins:dependency")
            .build();
    private String oldSystemDir;
    private PluginContext context;
    private Path projectRoot;


    @After
    public void tearDown() throws Exception {
        // reset system property
        if (oldSystemDir != null) {
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, oldSystemDir);
        }
    }

    @Before
    public void setUp() throws Exception {

        context = getPluginContextFile();
        if (System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            oldSystemDir = System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
        }

        final URL resource = getClass().getResource("/project");
        final String path = resource.getPath();
        projectRoot = new File(path).toPath();
    }

    /**
     * Plugin context with file system support
     *
     * @return PluginContext with file system initialized (so no JCR session)
     */
    private PluginContext getPluginContextFile() {
        if (context == null) {
            final URL resource = getClass().getResource("/project");
            final String basePath = resource.getPath();
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, basePath);
            context = new TestPluginContext(null, null);
            context.setProjectNamespacePrefix(PROJECT_NAMESPACE_TEST);
            context.setBeansPackageName("org.onehippo.cms7.essentials.dashboard.test.beans");
            context.setComponentsPackageName("org.onehippo.cms7.essentials.dashboard.test.components");
            context.setRestPackageName("org.onehippo.cms7.essentials.dashboard.test.rest");
            context.setRestPackageName("org.onehippo.cms7.essentials.dashboard.test.rest");

            final File file = new File(basePath);
            if (file.exists()) {
                final File cmsFolder = new File(basePath + File.separator + "cms");
                if (!cmsFolder.exists()) {
                    cmsFolder.mkdir();
                }
            }
        }
        return context;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public PluginContext getContext() {

        if (context == null) {
            context = getPluginContextFile();
        }
        return context;
    }

}
