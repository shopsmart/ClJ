package com.bradsdeals.clj;

import java.net.URLClassLoader;

import com.bradsdeals.clj.internal.dsl.ClojureFn;
import com.bradsdeals.clj.internal.dsl.ClojureFnInvocation;
import com.bradsdeals.clj.internal.dsl.ClojureFnLiteral;
import com.bradsdeals.clj.internal.dsl.ClojureLet;
import com.bradsdeals.clj.internal.dsl.ClojureVar;


/**
 * Static interface to IClJ instances.<p>
 *
 * Java helpers for calling Clojure code from Java. This class implements both
 * dynamic and type-safe methods for calling Clojure.<p>
 *
 * It is also separated into API and implementation packages, where only the
 * implementation package has a direct Clojure dependency.  This is to enable
 * ClJ to optionally be used in environments like OSGi where each instance of
 * Clojure must be isolated inside its own classloader. (Thanks to ShimDandy
 * for the techniques required to do this.  See: https://github.com/projectodd/shimdandy)<p>
 *
 * The following describes the two APIs:<p>
 *
 * The type-safe method involves creating a Java interface whose types match the
 * types of the Clojure function being called. Annotations on the Java interface
 * specify required namespaces and annotations on the interface methods specify
 * the namespace alias required to access the corresponding function.  Once this
 * is complete, you can use the {@link #define(Class, String...)} function to
 * create an instance of the interface referencing the corresponding Clojure
 * functions.<p>
 *
 * The dynamic method mimics Clojure's "do" form, but allows specifying require
 * clauses with aliases at the beginning.  See {@link #doAll(String[], ClojureFn...)}
 * for details.
 *
 * @author dorme@bradsdeals.com
 */
public class ClJ {

    private static final String CLJ_SUPPORT_CLASS = "com.bradsdeals.clj.ClJSupport";
    private static IClJ clj = null;

    /**
     * Manually set the {@link IClJ} implementation to use.
     * @param clj the {@link IClJ} implementation to use.
     */
    public static void setClojure(IClJ clj) {
        ClJ.clj = clj;
    }

    /**
     * Create a classloader-private instance of the ClJ Clojure-Java bridge.  To use this class,
     * create a {@link URLClassLoader} that points to the ClJ jar and your Clojure jar.
     * Normally, this classloader should be created as a child of your current classloader.
     * Pass your custom classloader into this constructor, then use the {@link #define(Class, String...)}
     * method to define Java interfaces referring to Clojure functions or use the Clojure
     * API DSL defined below.<p>
     *
     * After using this form, you must call {@link #close()} if the module that owns this classloader
     * is ever unloaded or you will leak system resources.<p>
     *
     * This method just delegates to {@link #init(ClassLoader)}.
     *
     * @param context The classloader that will host this Clojure instance.
     */
    public static void setClojure(ClassLoader context) {
        init(context);
    }

    /*
     * Get the current Clojure API bridge
     */
    private static IClJ clj() {
        if (clj == null) {
            try {
                // If our Clojure implementation is not set, see if we can find it using the current classloader.
                @SuppressWarnings("rawtypes")
                Class cljBridgeClass = Class.forName(CLJ_SUPPORT_CLASS);
                clj = (IClJ) cljBridgeClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot find ClJSupport class and #setClojure was never called", e);
            }
        }
        return clj;
    }

    /*
     * Define Java interfaces corresponding to Clojure functions and call Clojure from
     * Java as if it was Java.
     */

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
     * @param loadPackages Zero or more Clojure source code packages to load in order to define the interface
     * @param <T> The interface type.
     * @return T an instance of clojureInterface.
     */
    public static <T> T define(Class<T> clojureInterface, String...loadPackages) {
        return clj().define(clojureInterface, loadPackages);
    }

    /*
     * The dynamic Clojure DSL is implemented here
     */

    /**
     * A "do" block for Java that dynamically calls Clojure code.  e.g.:
     * <code>
     * doAll(require("Leiningen.core.user :as l",
     *               "clojure.java.io :as io"),
     *     $("l/init"),
     *     $("l/profiles")
     *     $("io/copy", "/tmp/sourcefile", "/tmp/outputfile"));
     * </code>
     *
     * @param aliases A string array of Clojure package aliases.  A helper {@link #require(String...)}
     * function can easily provide this array.
     * @param block A varargs parameter containing the function invocations to run.  These are obtained
     * via the {@link #$(String, Object...)} factory function.  IFn objects may be resolved via the {@link #fn(String)}
     * function.
     * @param <T> The result type.
     *
     * @return The result of the last function call.
     */
    public static <T> T doAll(String[] aliases, ClojureFn...block) {
        return clj().doAll(aliases, block);
    }

    /**
     * Let expression.  Vars are lexically scoped to the let expression's block.  Let expressions
     * can be nested, in which case inner let expressions can redeclare the same variable names and
     * shadow outer variables.  Whenever a let expression is in scope, all strings are first resolved
     * against declared variable names and values substituted.  Just like in Clojure, variables can
     * contain any value, including IFns.
     *
     * @param vars An array of ClojarVar objects, normally created using the {@link #vars(Object...)} function.
     * @param block 0-n Clojure expressions to execute with vars in scope.
     * @return The result of evaluating the last expression in block or null if block is empty.
     */
    public static ClojureLet let(ClojureVar[] vars, ClojureFn...block) {
        return clj().let(vars, block);
    }

    /**
     * Declare an array of ClojureVar objects to be used as the initial parameter to a let expression.
     * There must be an even number of arguments, alternating between String and Clojure expression
     * objects.
     *
     * @param nvPairs The variables to declare as a sequence of names and values.
     * @return an array of ClojureVar objects suitable for a let expression.
     */
    public static ClojureVar[] vars(Object...nvPairs) {
        return clj().vars(nvPairs);
    }

    /**
     * A convenience factory method that returns its parameter list as a String[].  Intended to be used to generate
     * the String[] of namespace aliases used by {@link #doAll(String[], ClojureFn...)}.
     *
     * @param aliases The aliases to return.
     * @return A String[] containing the aliases.
     */
    public static String[] require(String...aliases) {
        return aliases;
    }

    /**
     * Return a ClojureFn to be used inside a {@link #doAll(String[], ClojureFn...)} form
     * for functions that expect IFn objects as parameters.  Resolves namespace aliases
     * declared in the requires clause of the {@link #doAll(String[], ClojureFn...)} function.
     *
     * @param name The alias-qualified name of the Clojure function to return.  e.g.: "s/replace"
     * @return An unresolved ClojureFn that will be resolved during execution of {@link #doAll(String[], ClojureFn...)}.
     */
    public static ClojureFnLiteral fn(String name) {
        return clj().fn(name);
    }

    /**
     * Return an unresolved ClojureFnInvocation for execution by a {@link #doAll(String[], ClojureFn...)}
     * form.
     *
     * @param name The namespace-aliased name of the function to call. e.g.: "s/replace".
     * @param args The function's arguments.
     * @return A ClojureFnInvocation that can be executed by {@link #doAll(String[], ClojureFn...)}.
     */
    public static ClojureFnInvocation $(String name, Object...args) {
        return clj().$(name, args);
    }


    /*
     * Functions for accessing Clojure directly
     */

    /**
     * Return a Clojure Var referenced by fullyQualifiedName.
     *
     * @param fullyQualifiedName A Clojure fully qualified symbol name.
     * @return a Clojure Var referenced by fullyQualifiedName
     */
    public static Object var(final String fullyQualifiedName) {
        return clj().var(fullyQualifiedName);
    }

    /**
     * Directly execute the fully-namespace-qualified Cloure function identified by fn, passing args
     * as arguments.
     *
     * @param fn The fully-namespace-qualified Clojure function to call.
     * @param args The arguments to pass.
     * @param <T> The type of the return value.
     * @return the value the Clojure function returned.
     */
    public static <T> T invoke(final String fn, Object...args) {
        return clj().invoke(fn, args);
    }

    /**
     * Directly execute the Clojure function identified by fn, passing args as arguments.
     *
     * @param <T> The return type
     * @param fn The Clojure function to call.
     * @param args The arguments to pass.
     * @return the value the Clojure function returned.
     */
    public static <T> T invoke(final Object fn, final Object...args) {
        return clj().invoke(fn, args);
    }


    //
    // Init/close for dynamic Clojure instances
    // Not needed otherwise.
    //

    /**
     * Create a classloader-private instance of the ClJ Clojure-Java bridge.  To use this class,
     * create a {@link URLClassLoader} that points to the ClJ jar and your Clojure Jar.
     * Normally, this classloader should be created as a child of your current classloader.
     * Pass your custom classloader into this constructor, then use the {@link #define(Class, String...)}
     * method to define Java interfaces referring to Clojure functions.
     *
     * After using this form, you must call {@link #close()} if the module that owns this classloader
     * is ever unloaded or you will leak system resources.<p>
     *
     * @param context The classloader that will host this Clojure instance.
     */
    @SuppressWarnings("rawtypes")
    public static void init(ClassLoader context) {
        try {
            Class cljBridgeClass = Class.forName(CLJ_SUPPORT_CLASS, true, context);
            clj = (IClJ) cljBridgeClass.newInstance();
            clj.init(context);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize private Clojure instance", e);
        }
    }


    /**
     * If you initialize this class with a private classloader / Clojure instance, when you
     * want to shut down the Clojure instance, you need to call {@link #close()} to free resources
     * held by Clojure and by this library.
     */
    public static void close() {
        clj().close();
    }
}
