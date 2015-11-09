package com.bradsdeals.clj;

import java.lang.reflect.Method;
import java.net.URLClassLoader;

/**
 * A ClassLoader-private ClJ instance, intended for use in dynamic environments like OSGi, where
 * it is desirable for multiple modules (e.g.: OSGi bundles) to have their own private Clojure
 * instance.
 *
 * @author dorme
 */
public class PrivateClJ {

    private final Class<?> cljBridgeClass;
    private final Method cljDefine;
    private final Method cljClose;

    /**
     * Create a classloader-private instance of the ClJ Clojure-Java bridge.  To use this class,
     * create a {@link URLClassLoader} that points to the ClJ jar and your Clojure Jar.
     * Normally, this classloader should be created as a child of your current classloader.
     * Pass your custom classloader into this constructor, then use the {@link #define(Class)}
     * method to define Java interfaces referring to Clojure functions.
     *
     * @param context The classloader that will host this Clojure instance.
     */
    public PrivateClJ(ClassLoader context) {
        try {
            cljBridgeClass = context.loadClass("com.bradsdeals.clj.ClJ");
            final Method init = cljBridgeClass.getDeclaredMethod("init", ClassLoader.class);
            init.setAccessible(true);
            init.invoke(null, context);
            cljDefine = cljBridgeClass.getDeclaredMethod("define", Class.class);
            cljDefine.setAccessible(true);
            cljClose = cljBridgeClass.getDeclaredMethod("close");
            cljClose.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize private Clojure instance", e);
        }
    }

    /**
     * Define an instance of a Clojure interface.  Calling methods on this instance will
     * delegate to the corresponding Clojure functions as specified by the "Require" and
     * "Ns" annotations.
     * <code>
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str") String replace(String source, Pattern regex, String replacement);
     *       \@Ns("io") void copy(byte[] input, OutputStream output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     *
     * @param clojureInterface The Clojure interface to define.
     * @param <T> The interface type.
     * @return T an instance of clojureInterface.
     */
    @SuppressWarnings("unchecked")
    public <T> T define(Class<T> clojureInterface) {
        try {
            return (T) cljDefine.invoke(null, clojureInterface);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot define Clojure interface", e);
        }
    }


    /**
     * Close the private Clojure instance.  When your module/bundle unloads, it is necessary
     * to call this method in order to avoid leaking Threads.
     */
    public void close() {
        try {
            cljClose.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot close private Clojure instance", e);
        }
    }

}
