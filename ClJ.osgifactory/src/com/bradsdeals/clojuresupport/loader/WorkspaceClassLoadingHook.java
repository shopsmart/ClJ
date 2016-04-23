/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.bradsdeals.clojuresupport.loader;

import java.io.BufferedReader;

/*
 * @see https://www.eclipse.org/forums/index.php/m/1442089/?srch=ClassLoadingHook#msg_1442089
 *
 * @see https://github.com/Talend/tcommon-studio-se/blob/51fe5eb893a16bcc162632caaa3eb7d8c5abc24d/main/plugins/org.talend.osgi.lib.loader/src/org/talend/osgi/configurator/JarLoaderConfigurator.java
 * @see https://github.com/Talend/tcommon-studio-se/blob/51fe5eb893a16bcc162632caaa3eb7d8c5abc24d/main/plugins/org.talend.osgi.lib.loader/hookconfigurators.properties
 * @see https://github.com/Talend/tcommon-studio-se/tree/51fe5eb893a16bcc162632caaa3eb7d8c5abc24d/main/plugins/org.talend.osgi.lib.loader
 */

import static com.coconut_palm_software.possible.iterable.fn.Join.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.NativeCodeFinder;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;

import com.bradsdeals.clj.wrappers.IClojureIterable;
import com.bradsdeals.clojuresupport.loader.DynamicClojure.Leiningen;

public class WorkspaceClassLoadingHook extends ClassLoaderHook implements KeyedElement {
	public static final String KEY = WorkspaceClassLoadingHook.class.getName();
	public static final int HASHCODE = KEY.hashCode();

	public WorkspaceClassLoadingHook(EquinoxConfiguration configuration) {
	    //noop
	}

	/*
	 * Parse the .classpath file and add output folders.  Next step: Clojure support via Leiningen.
	 *
	 * <?xml version="1.0" encoding="UTF-8"?>
     * <classpath>
     *     <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"/>
     *     <classpathentry kind="con" path="org.eclipse.pde.core.requiredPlugins"/>
     *     <classpathentry kind="src" output="bin2" path="src"/>   ****HERE****
     *     <classpathentry kind="output" path="bin"/>              ****AND HERE****
     * </classpath>
	 */

	@Override
	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, Generation sourceGeneration) {
	    boolean result = false;

	    BundleFile bundleFile = sourceGeneration.getBundleFile();
	    if (bundleFile instanceof DirBundleFile) {
            result = addSrcFoldersToClasspath(result, bundleFile, cpEntries, hostmanager, sourceGeneration);
            result = resolveLeinClasspath(result, bundleFile, cpEntries, hostmanager, sourceGeneration);
	    }
		return result;
	}

    @SuppressWarnings("unchecked")
    private boolean resolveLeinClasspath(boolean result, BundleFile bundleFile, ArrayList<ClasspathEntry> cpEntries,
            ClasspathManager hostmanager, Generation sourceGeneration) {
        if (DynamicClojure.isInitialized()) {
            final File projectClj = bundleFile.getFile("project.clj", false);
            if (projectClj != null && projectClj.exists()) {
                try {
                    Leiningen lein = DynamicClojure.lein.get();
                    List<String> projectCljFileLines = readFile(projectClj);
                    String projectCljFile = join(projectCljFileLines, "\n");

                    // Note: Maps come back as iterables of iterables
                    final IClojureIterable<IClojureIterable<Object>> projectMap = lein.resolveProject(projectCljFile);
                    for (IClojureIterable<Object> projectEntry : projectMap) {
                        String first = projectEntry.get(0).toString();
                        if (":dep-files".equals(first)) {
                            IClojureIterable<File> jars = (IClojureIterable<File>) projectEntry.get(1);
                            for (File jar : jars) {
                                if (jar.exists()) {
                                    if (hostmanager.addClassPathEntry(cpEntries,
                                            NativeCodeFinder.EXTERNAL_LIB_PREFIX + jar.getCanonicalPath(),
                                            hostmanager,
                                            sourceGeneration)) {
                                        result = true;
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    // FIXME: Figure out how to log...
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    protected boolean addSrcFoldersToClasspath(boolean result, BundleFile bundleFile,
            ArrayList<ClasspathEntry> cpEntries, ClasspathManager hostmanager, Generation sourceGeneration) {
        final File classpathFile = bundleFile.getFile(".classpath", false);
        final File manifestFile = bundleFile.getFile("META-INF/MANIFEST.MF", false);
        if (classpathFile != null && manifestFile != null && classpathFile.exists() && manifestFile.exists()) {
            try {
                // TODO: Use a real XML parser
                List<String> classpathLines = readFile(classpathFile);
                Pattern outputAttr = Pattern.compile(".*kind=\"src\" output=\"([^\".]*)\".*");
                result = addProjectFolders(outputAttr, classpathLines, cpEntries, hostmanager, sourceGeneration, result,
                        bundleFile);
                Pattern outputKind = Pattern.compile(".*kind=\"output\" path=\"([^\".]*)\".*");
                result = addProjectFolders(outputKind, classpathLines, cpEntries, hostmanager, sourceGeneration, result,
                        bundleFile);
            } catch (IOException e) {
                // FIXME: Figure out how to log...
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean addProjectFolders(Pattern outputAttr, List<String> classpathLines, ArrayList<ClasspathEntry> cpEntries,
            ClasspathManager hostmanager, Generation sourceGeneration, boolean result, BundleFile bundleFile) {
        for (String line : classpathLines) {
            final Matcher matcher = outputAttr.matcher(line);
            if (matcher.matches()) {
                String match = matcher.group(1);
                final File file = bundleFile.getFile(match, false);
                if (file != null && file.exists()) {
                    if (hostmanager.addClassPathEntry(cpEntries, match, hostmanager, sourceGeneration)) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    public List<String> readFile(final File file) throws FileNotFoundException, IOException {
        List<String> lines = new ArrayList<>(20);
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            String line = r.readLine();
            while (line != null) {
                lines.add(line);
                line = r.readLine();
            }
        } finally {
            r.close();
        }
        return lines;
    }


//	private Generation findFragmentSource(Generation hostGeneration, String cp, ClasspathManager manager, boolean fromFragment) {
//		if (hostGeneration != manager.getGeneration())
//			return hostGeneration;
//
//		File file = new File(cp);
//		if (!file.isAbsolute())
//			return hostGeneration;
//		FragmentClasspath[] fragCP = manager.getFragmentClasspaths();
//		for (int i = 0; i < fragCP.length; i++) {
//			BundleFile fragBase = fragCP[i].getGeneration().getBundleFile();
//			File fragFile = fragBase.getBaseFile();
//			if (fragFile != null && file.getPath().startsWith(fragFile.getPath()))
//				return fragCP[i].getGeneration();
//		}
//		return fromFragment ? null : hostGeneration;
//	}

	public boolean compare(KeyedElement other) {
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public int getKeyHashCode() {
		return HASHCODE;
	}
}