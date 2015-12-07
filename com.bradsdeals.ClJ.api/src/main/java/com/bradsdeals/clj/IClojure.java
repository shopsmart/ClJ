package com.bradsdeals.clj;

import java.util.Map;

/**
 * Interface to the Clojure runtime that allows for classloader-private Clojure runtime instances.
 *
 * @author dorme
 */
public interface IClojure {

    /**
     * Initialize a private Clojure instance using the specified ClassLoader.  The ClassLoader must have
     * clojure.jar and the ClJ runtime on its classpath and no other current ClassLoader should be able to
     * see Clojure or the ClJ runtime.  This method is intended for use in container applications (e.g.:
     * web containers, OSGi) where Clojure's runtime needs to be private to a specific dynamically-loadable
     * module.  If you are using Clojure/ClJ in a normal standalone Java application, you can ignore this
     * method.
     *
     * @param context The {@link ClassLoader} referencing clojure.jar
     */
    void init(ClassLoader context);

    /**
     * Compute a Map from namespace aliases to the fully-expanded namespace given a String array of aliases
     * in the form 'namespace :as alias'.
     *
     * @param aliases A String[] of aliases in the form 'namespace :as alias'.
     * @return A Map from alias string to namespace string
     */
    public Map<String, String> computeNsAliases(String[] aliases);

    /**
     * Shutdown the current private Clojure instance.  Only call this method if you initialized a private
     * Clojure instance using {@link #init(ClassLoader)} and the module containing this Clojure instance
     * is being unloaded by its container.  Otherwise, (e.g.: if Clojure and ClJ are on a globally-accessible
     * classpath) you can safely ignore this method.
     */
    void close();

    /**
     * Resolve a Clojure Var given its fully-qualified name.
     *
     * @param fullyQualifiedName The package-qualified name of the Var to look up.
     * @return The Clojure Var/IFn
     */
    Object var(final String fullyQualifiedName);

    /**
     * Resolve a Clojure Var given its fully-qualified name.
     *
     * @param namespace The var's namespace.
     * @param varName The var's name.
     * @return THe Clojure Var/IFn.
     */
    Object var(String namespace, String varName);

    /**
     * Directly execute the fully-namespace-qualified Cloure function identified by fn, passing args
     * as arguments.
     *
     * @param fn The fully-namespace-qualified Clojure function to call.
     * @param args The arguments to pass.
     * @param <T> The type of the return value.
     * @return the value the Clojure function returned.
     */
    <T> T invoke(final String fn, Object...args);

    /**
     * Directly execute the Clojure function identified by fn, passing args as arguments.
     *
     * @param <T> The return type
     * @param fn The Clojure function to call.
     * @param args The arguments to pass.
     * @return the value the Clojure function returned.
     */
    <T> T invoke(final Object fn, final Object...args);

}
