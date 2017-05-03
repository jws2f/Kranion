/*
 * The MIT License
 *
 * Copyright 2017 Focused Ultrasound Foundation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.kranion.plugin;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;
import java.net.URL;
import java.lang.reflect.*;

/**
 *
 * @author john
 */
public class PluginFinder {

    List<Plugin> pluginCollection;

    public PluginFinder() {
        pluginCollection = new ArrayList<Plugin>(5);
    }

    public void search(String directory) throws Exception {
        File dir = new File(directory);
        if (dir.isFile()) {
            return;
        }
        File[] files = dir.listFiles(new JarFilter());
        for (File f : files) {
            List<String> classNames = getClassNames(f.getAbsolutePath());
            for (String className : classNames) {
// Remove the ".class" at the back
                String name = className.substring(0, className.length() - 6);
                Class clazz = getClass(f, name);
                Class[] interfaces = clazz.getInterfaces();
                for (Class c : interfaces) {
// Implement the IPlugin interface
                    if (c.getName().equals("org.fusfoundation.kranion.plugin.Plugin")) {
                        pluginCollection.add((Plugin) clazz.newInstance());
                    }
                }
            }
        }
    }

    protected List<String> getClassNames(String jarName) throws IOException {
        ArrayList<String> classes = new ArrayList<String>(10);
        JarInputStream jarFile = new JarInputStream(new FileInputStream(jarName));
        JarEntry jarEntry;
        while (true) {
            jarEntry = jarFile.getNextJarEntry();
            if (jarEntry == null) {
                break;
            }
            if (jarEntry.getName().endsWith(".class")) {
                classes.add(jarEntry.getName().replaceAll("/", "\\."));
            }
        }

        return classes;
    }

    public Class getClass(File file, String name) throws Exception {
        addURL(file.toURL());

        URLClassLoader clazzLoader;
        Class clazz;
        String filePath = file.getAbsolutePath();
        filePath = "jar:file://" + filePath + "!/";
        URL url = new File(filePath).toURL();
        clazzLoader = new URLClassLoader(new URL[]{url});
        clazz = clazzLoader.loadClass(name);
        return clazz;

    }
    
    private static final Class<?>[] PARAMS = new Class[] { URL.class };
    
    public void addURL(URL u) throws IOException {
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL urls[] = sysLoader.getURLs();
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString().equalsIgnoreCase(u.toString())) {
                return;
            }
        }
        Class sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", PARAMS);
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    public List<Plugin> getPluginCollection() {
        return pluginCollection;
    }

    public void setPluginCollection(List<Plugin> pluginCollection) {
        this.pluginCollection = pluginCollection;
    }
}
