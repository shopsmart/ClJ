package com.bradsdeals.clj;

import com.bradsdeals.clj.ClJAnnotations.Ns;
import com.bradsdeals.clj.ClJAnnotations.Pt;
import com.bradsdeals.clj.ClJAnnotations.Require;
import com.bradsdeals.clj.internal.dsl.ClojureFn;
import com.bradsdeals.clj.internal.dsl.ClojureFnInvocation;
import com.bradsdeals.clj.internal.dsl.ClojureFnLiteral;
import com.bradsdeals.clj.internal.dsl.ClojureLet;
import com.bradsdeals.clj.internal.dsl.ClojureVar;

/**
 * An Interface defining helpers for calling Clojure code from Java. This defines
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
 * @author dorme
 */
public interface IClJ extends IClojure {
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
     * @see Require
     * @see Ns
     * @see Pt
     *
     * @param clojureInterface The Clojure interface to define.
     * @param loadPackages Zero or more Clojure source code packages to load in order to define the interface
     * @param <T> The interface type.
     * @return T an instance of clojureInterface.
     */
    <T> T define(Class<T> clojureInterface, String...loadPackages);

    /*
     * The dynamic Clojure DSL is declared here
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
    <T> T doAll(String[] aliases, ClojureFn...block);

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
    ClojureLet let(ClojureVar[] vars, ClojureFn...block);

    /**
     * Declare an array of ClojureVar objects to be used as the initial parameter to a let expression.
     * There must be an even number of arguments, alternating between String and Clojure expression
     * objects.
     *
     * @param nvPairs The variables to declare as a sequence of names and values.
     * @return an array of ClojureVar objects suitable for a let expression.
     */
    ClojureVar[] vars(Object...nvPairs);

    /**
     * A convenience factory method that returns its parameter list as a String[].  Intended to be used to generate
     * the String[] of namespace aliases used by {@link #doAll(String[], ClojureFn...)}.
     *
     * @param aliases The aliases to return.
     * @return A String[] containing the aliases.
     */
    String[] require(String...aliases);

    /**
     * Return a ClojureFn to be used inside a {@link #doAll(String[], ClojureFn...)} form
     * for functions that expect IFn objects as parameters.  Resolves namespace aliases
     * declared in the requires clause of the {@link #doAll(String[], ClojureFn...)} function.
     *
     * @param name The alias-qualified name of the Clojure function to return.  e.g.: "s/replace"
     * @return An unresolved ClojureFn that will be resolved during execution of {@link #doAll(String[], ClojureFn...)}.
     */
    ClojureFnLiteral fn(String name);

    /**
     * Return an unresolved ClojureFnInvocation for execution by a {@link #doAll(String[], ClojureFn...)}
     * form.
     *
     * @param name The namespace-aliased name of the function to call. e.g.: "s/replace".
     * @param args The function's arguments.
     * @return A ClojureFnInvocation that can be executed by {@link #doAll(String[], ClojureFn...)}.
     */
    ClojureFnInvocation $(String name, Object...args);
}
