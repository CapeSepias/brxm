/*
 *  Copyright 2010 Hippo.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class UpgradeIntegrationTest
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final String USERNAME = "admin";
    protected static final char[] PASSWORD = "admin".toCharArray();
    private static final String MAGIC = "CAMIG";

    static private void delete(File path) {
        if(path.exists()) {
            if(path.isDirectory()) {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                    delete(files[i]);
            }
            path.delete();
        }
    }

    @Ignore
    public void dummy() {
    }

    @Ignore
    public void dump() throws RepositoryException, IOException {
        delete(new File("storage"));
        HippoRepository repository = HippoRepositoryFactory.getHippoRepository("storage");
        Session session = repository.login(USERNAME, PASSWORD);
        Property version = session.getRootNode().getProperty("hippo:configuration/hippo:initialize/hippo:version");
        Value[] oldValues = version.getValues();
        Value[] newValues = new Value[oldValues.length+1];
        System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
        newValues[oldValues.length] = session.getValueFactory().createValue(MAGIC, PropertyType.STRING);
        version.setValue(newValues);
        session.save();
        repository.close();
        FileOutputStream ostream = new FileOutputStream("../src/test/fixtures/dump-noindices.zip");
        dump(ostream, "storage");
        ostream.close();
    }

    @Test
    public void migrate() throws RepositoryException, IOException {
        delete(new File("storage"));
        FileInputStream istream = new FileInputStream("../src/test/fixtures/dump-noindices.zip");
        restore(istream);
        istream.close();
        HippoRepository repository = HippoRepositoryFactory.getHippoRepository("storage");
        Session session = repository.login(USERNAME, PASSWORD);
        assertTrue(session.getRootNode().hasNode("hippo:configuration"));
        assertTrue(session.getRootNode().hasNode("hippo:configuration/hippo:initialize"));
        assertTrue(session.getRootNode().hasProperty("hippo:configuration/hippo:initialize/hippo:version"));
        Set<String> versionTags = new TreeSet<String>();
        for(Value value : session.getRootNode().getProperty("hippo:configuration/hippo:initialize/hippo:version").getValues()) {
            versionTags.add(value.getString());
        }
        assertTrue(versionTags.contains("v16a"));
        assertTrue(versionTags.contains(MAGIC));
    }

    static public void restore(InputStream istream) throws IOException {
        JarInputStream input = new JarInputStream(istream);
        ZipEntry ze;
        do {
            ze = input.getNextEntry();
            if(ze != null) {
                if(ze.isDirectory()) {
                    String name = ze.getName();
                    File file = new File(name);
                    file.mkdir();
                } else {
                    FileOutputStream ostream = new FileOutputStream(ze.getName());
                    stream(input, ostream);
                    ostream.close();
                }
            }
        } while(ze != null);
        input.close();
    }

    static public void dump(OutputStream ostream, String location) throws IOException {
        JarOutputStream output = new JarOutputStream(ostream);
        dump(output, location);
        output.close();
    }

    static private void dump(JarOutputStream output, String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                ZipEntry ze = new ZipEntry(path + file.separator);
                output.putNextEntry(ze);
                output.closeEntry();
                for (String item : file.list()) {
                    dump(output, path + file.separator + item);
                }
            } else {
                ZipEntry ze = new ZipEntry(path);
                output.putNextEntry(ze);
                FileInputStream istream = new FileInputStream(file);
                stream(istream, output);
                output.closeEntry();
            }
        }
    }

    static private void stream(InputStream istream, OutputStream ostream) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        do {
            len = istream.read(buffer);
            if(len >= 0) {
                ostream.write(buffer, 0, len);
            }
        } while (len >= 0);
    }
}
