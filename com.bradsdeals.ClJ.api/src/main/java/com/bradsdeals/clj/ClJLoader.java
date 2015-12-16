package com.bradsdeals.clj;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;


/**
 * A factory for IClJ instances.<p>
 *
 * @author dorme@bradsdeals.com
 */
public class ClJLoader {

    private static final String CLJ_SUPPORT_CLASS = "com.bradsdeals.clj.ClJ";

    /**
     * Create a classloader-private instance of the ClJ Clojure-Java bridge.  To use this class,
     * create a {@link URLClassLoader} that points to the ClJ jar and your Clojure jar.
     * Normally, this classloader should be created as a child of your current classloader.
     * Pass your custom classloader into this constructor, then use the {@link IClJ#define(Class, String...)}
     * method to define Java interfaces referring to Clojure functions or use the Clojure
     * API DSL defined in {@link ClJDSL}.<p>
     *
     * After using this form, you must call {@link IClJ#close()} if the module that owns this classloader
     * is ever unloaded or you will leak system resources.<p>
     *
     * @param context The classloader that will host this Clojure instance.
     *
     * @return IClJ an IClJ instance for executing Clojure code from Java.
     */
    @SuppressWarnings("unchecked")
    public static IClJ clj(ClassLoader context) {
        try {
            Class<IClJ> cljBridgeClass = (Class<IClJ>) Class.forName(CLJ_SUPPORT_CLASS, true, context);
            final Constructor<IClJ> constructor = cljBridgeClass.getConstructor(ClassLoader.class);
            IClJ clj = constructor.newInstance(context);
            return clj;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize private Clojure instance", e);
        }
    }

    /**
     * Dynamically load an IClJ instance using Class.forName and the current classloader.
     *
     * @return IClJ an IClJ instance for executing Clojure code from Java.
     */
    @SuppressWarnings({ "unchecked" })
    public static IClJ clj() {
        try {
            // If our Clojure implementation is not set, see if we can find it using the current classloader.
            final ClassLoader cl = ClJLoader.class.getClassLoader();
            Class<IClJ> cljBridgeClass = (Class<IClJ>) Class.forName(CLJ_SUPPORT_CLASS, true, cl);
            final Constructor<IClJ> constructor = cljBridgeClass.getConstructor(ClassLoader.class);
            return constructor.newInstance(cl);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize private Clojure instance", e);
        }
    }

}
