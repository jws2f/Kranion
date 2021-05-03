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
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author john
 */
public class PluginFinder {

    List<Plugin> pluginCollection;

    public PluginFinder() {
        pluginCollection = new ArrayList<>(5);
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
//                System.out.println("*** Loading class: " + className);
                try {
                    Class clazz = getClass(f, name);
                    Class[] interfaces = clazz.getInterfaces();
                    for (Class c : interfaces) {
                        // Implements the Plugin interface?
                        if (c.getName().equals("org.fusfoundation.kranion.plugin.Plugin")) {
                            pluginCollection.add((Plugin) clazz.newInstance());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception thrown for " + className);
                    Logger.getGlobal().log(Level.WARNING, "Failed to load class: " + className);
                }
            }
        }
    }

    protected List<String> getClassNames(String jarName) throws IOException {
        ArrayList<String> classes = new ArrayList<>(10);
        JarInputStream jarFile = new JarInputStream(new FileInputStream(jarName));
        JarEntry jarEntry;
        while (true) {
            jarEntry = jarFile.getNextJarEntry();
            if (jarEntry == null) {
                break;
            }
            if (jarEntry.getName().endsWith(".class") && !jarEntry.getName().startsWith("META-INF")) {
                classes.add(jarEntry.getName().replaceAll("/", "\\."));
            }
        }

        return classes;
    }

    public Class getClass(File file, String name) throws Exception {
        addURL(file.toURI().toURL());

        URLClassLoader clazzLoader;
        Class clazz;
        String filePath = file.getAbsolutePath();
        filePath = "jar:file://" + filePath + "!/";
        URL url = new File(filePath).toURI().toURL();
        clazzLoader = new URLClassLoader(new URL[]{url});
        clazz = clazzLoader.loadClass(name);
        return clazz;

    }
    
    private static final Class<?>[] PARAMS = new Class<?>[] { URL.class };
    
    public void addURL(URL u) throws IOException, URISyntaxException {
        
// Copyright notice from CG Jennings for jar-loader inclusion:
// https://github.com/CGJennings/jar-loader/blob/master/LICENSE

//        BSD 2-Clause License
//
//Copyright (c) 2018, Christopher G. Jennings
//All rights reserved.
//
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions are met:
//
//1. Redistributions of source code must retain the above copyright notice, this
//   list of conditions and the following disclaimer.
//
//2. Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
//AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
//IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
//FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
//DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
//CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
//OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
        ca.cgjennings.jvm.JarLoader.addToClassPath(Paths.get(u.toURI()).toFile());
//        ca.cgjennings.jvm.JarLoader.addToClassPath(new File(u.getFile()));

//
// Java 8 class loader, but breaks Java 9+
//        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//        URL urls[] = sysLoader.getURLs();
//        for (URL url : urls) {
//            if (url.toString().equalsIgnoreCase(u.toString())) {
//                return;
//            }
//        }
//        Class sysclass = URLClassLoader.class;
//        try {
//            Method method = sysclass.getDeclaredMethod("addURL", PARAMS);
//            method.setAccessible(true);
//            method.invoke(sysLoader, new Object[]{u});
//        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException t) {
//            //t.printStackTrace();
//            throw new IOException("Error, could not add URL to system classloader");
//        }
    }

    public List<Plugin> getPluginCollection() {
        return pluginCollection;
    }

    public void setPluginCollection(List<Plugin> pluginCollection) {
        this.pluginCollection = pluginCollection;
    }
}
