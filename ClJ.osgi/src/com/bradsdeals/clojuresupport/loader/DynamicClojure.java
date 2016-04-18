package com.bradsdeals.clojuresupport.loader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import com.bradsdeals.clj.ClJAnnotations.Ns;
import com.bradsdeals.clj.ClJAnnotations.Require;
import com.bradsdeals.clj.ClJLoader;
import com.bradsdeals.clj.IClJ;
import com.bradsdeals.clj.wrappers.IClojureIterable;
import com.coconut_palm_software.possible.Possible;

public class DynamicClojure {

    /*
     * Clojure bootstrap algorithm:
     *  loader.clj is design-time support for dynamically resetting
     *  the classpath whenever either manifest or project.clj change.
     *
     *  It may eventually provide UI for showing workspace Clojure
     *  plugins, their status, and UI for starting/stopping/reloading.
     *
     *  - Make this a M2 project with "referenced libraries" as dependencies and a shaded JAR.
     *  - REPL library: Use Cider
     *  - Refactor noted parts in #initLein below as described
     */

    private static final String LIB_CLJ_JAR = "lib/ClJ.jar";
    private static final String LIB_LEININGEN_JAR = "lib/leiningen-2.5.2-standalone.jar";
    private static final Version LIB_LEININGEN_VERSION = new Version(LIB_LEININGEN_JAR, 2, 5, 2);
    private static final String CLJ_LOAD_DEPS_NS = "/com/bradsdeals/clojuresupport/loader/project_classpath_loader";

    @Require({"com.bradsdeals.clojuresupport.loader.project-classpath-loader :as l",
        "leiningen.core.user :as u",
        "leiningen.core.project :as p",
        "leiningen.core.classpath :as cp"})
    public interface Leiningen {
      @Ns("u") void init();
      @Ns("u") void profiles();
      @Ns("l") IClojureIterable<IClojureIterable<Object>> resolveProject(String projectCljContents); // defined in project_classloader.clj
    }

    /**
     * This is the public API for this class.  It is initialized when the Activator runs.
     */
    public static Possible<Leiningen> lein = Possible.emptyValue();

    private static String PLUGIN_ID = "com.bradsdeals.clojuresupport.loader";
    private static String HOST_ID = "org.eclipse.osgi";

    private static Possible<BundleContext> parentContext = Possible.emptyValue();
    private static Possible<Bundle> self = Possible.emptyValue();

    public static boolean isInitialized() {
        return parentContext.hasValue() && self.hasValue() && lein.hasValue();
    }

    public static void start(BundleContext osgiBundleContext) throws Exception {
        final Bundle[] bundles = osgiBundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (PLUGIN_ID.equals(bundle.getSymbolicName())) {
                self = Possible.value(bundle);
            }
            if (HOST_ID.equals(bundle.getSymbolicName())) {
                parentContext = Possible.value(bundle.getBundleContext());
            }
        }
        if (self.hasValue() && parentContext.hasValue()) {
            initLeiningen();
        }
    }

    private static void initLeiningen() throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BundleContext context = parentContext.get();

        if (lein.isEmpty()) {
            ClassLoader parentClassloader = getBundleClassloader(context)
                    .getOrThrow(new IllegalStateException("Could not get bundle classloader"));
            URL leiningenJar = null;
            Possible<Version> systemLeinVersion = FileFinder.findNewestLeiningen();
            if (systemLeinVersion.hasValue() && systemLeinVersion.get().compareTo(LIB_LEININGEN_VERSION) > 0) {
                leiningenJar = FileFinder.findLeiningen(systemLeinVersion.get().toString())
                        .getOrThrow(new RuntimeException("Could not find Leiningen: " + systemLeinVersion.get()))
                        .toURI().toURL();
            } else {
                leiningenJar = self.get().getEntry(LIB_LEININGEN_JAR);
            }
            URL cljToJavaJar = self.get().getEntry(LIB_CLJ_JAR);
            ClassLoader clojureClassloader = new URLClassLoader(new URL[] {leiningenJar, cljToJavaJar}, parentClassloader);
            IClJ clj = ClJLoader.clj(clojureClassloader);

            final Leiningen privateLein = clj.define(Leiningen.class, CLJ_LOAD_DEPS_NS);
            privateLein.init();
            privateLein.profiles();
            lein = Possible.value(privateLein);
        }
    }

    private static Possible<ClassLoader> getBundleClassloader(BundleContext bundleContext) {
        BundleWiring wiring = bundleContext.getBundle().adapt(BundleWiring.class);
        if (wiring != null) {
            return Possible.value(wiring.getClassLoader());
        }
        return Possible.emptyValue("Unable to obtain BundleWiring");
    }

}
