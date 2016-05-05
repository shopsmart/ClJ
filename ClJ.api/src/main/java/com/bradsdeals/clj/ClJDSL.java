package com.bradsdeals.clj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.bradsdeals.clj.internal.dsl.ClojureFn;
import com.bradsdeals.clj.internal.dsl.ClojureFnInvocation;
import com.bradsdeals.clj.internal.dsl.ClojureFnLiteral;
import com.bradsdeals.clj.internal.dsl.ClojureLet;
import com.bradsdeals.clj.internal.dsl.ClojureVar;

/**
 * A dynamic Clojure DSL mimicking Clojure's "do" form, and that allows specifying require
 * clauses with aliases at the beginning.  See {@link #doAll(IClojure, String[], ClojureFn...)}
 * for details.
 *
 * @author dorme
 */
public class ClJDSL {

    /**
     * A "do" block for Java that dynamically calls Clojure code.  e.g.:
     * <code>
     * doAll(c, require("Leiningen.core.user :as l",
     *                  "clojure.java.io :as io"),
     *     $("l/init"),
     *     $("l/profiles")
     *     $("io/copy", "/tmp/sourcefile", "/tmp/outputfile"));
     * </code>
     *
     * @param clojure An {@link IClojure} instance that can make the actual calls into Clojure.
     * @param aliases A string array of Clojure package aliases.  A helper {@link #require(String...)}
     * function can easily provide this array.
     * @param block A varargs parameter containing the function invocations to run.  These are obtained
     * via the {@link #$(String, Object...)} factory function.  IFn objects may be resolved via the {@link #fn(String)}
     * function.
     * @param <T> The result type.
     *
     * @return The result of the last function call.
     */    @SuppressWarnings("unchecked")
    public static <T> T doAll(IClojure clojure, String[] aliases, ClojureFn...block) {
        Map<String, String> nsAliases = computeNsAliases(aliases);
        Object result = null;
        for (ClojureFn fn : block) {
            if (fn instanceof IClojureCaller) {
                ((IClojureCaller)fn).setClojure(clojure);
            }
            result = fn.invoke(nsAliases, new LinkedList<HashMap<String,Object>>());
        }
        return (T)result;
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
        return new ClojureLet(vars, block);
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
        if (nvPairs.length % 2 != 0) {
            throw new IllegalArgumentException("There must be an even number of values in a let binding");
        }
        ArrayList<ClojureVar> result = new ArrayList<ClojureVar>(nvPairs.length/2);
        for (int i=0; i <= nvPairs.length-2; i += 2) {
            result.add(new ClojureVar((String)nvPairs[i], nvPairs[i+1]));
        }
        return result.toArray(new ClojureVar[nvPairs.length/2]);
    }

    /**
     * A convenience factory method that returns its parameter list as a String[].  Intended to be used to generate
     * the String[] of namespace aliases used by {@link #doAll(IClojure, String[], ClojureFn...)}.
     *
     * @param aliases The aliases to return.
     * @return A String[] containing the aliases.
     */
    public static String[] require(String...aliases) {
        return aliases;
    }

    /**
     * Return a ClojureFn to be used inside a {@link #doAll(IClojure, String[], ClojureFn...)} form
     * for functions that expect IFn objects as parameters.  Resolves namespace aliases
     * declared in the requires clause of the {@link #doAll(IClojure, String[], ClojureFn...)} function.
     *
     * @param name The alias-qualified name of the Clojure function to return.  e.g.: "c/inc"
     * @return An unresolved ClojureFn that will be resolved during execution of {@link #doAll(IClojure, String[], ClojureFn...)}.
     */
    public static ClojureFnLiteral fn(String name) {
        return new ClojureFnLiteral(name);
    }

    /**
     * Return an unresolved ClojureFnInvocation for execution by a {@link #doAll(IClojure, String[], ClojureFn...)}
     * form.
     *
     * @param name The namespace-aliased name of the function to call. e.g.: "s/replace".
     * @param args The function's arguments.
     * @return A ClojureFnInvocation that can be executed by {@link #doAll(IClojure, String[], ClojureFn...)}.
     */
    public static ClojureFnInvocation $(String name, Object...args) {
        return new ClojureFnInvocation(name,args);
    }

    private static Map<String, String> computeNsAliases(String[] aliases) {
        Map<String,String> result = new HashMap<String, String>();
        for (String alias : aliases) {
            String[] parts = alias.split(" :as ");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Expecting 'namespace :as alias' but found: " + alias);
            }
            result.put(parts[1], parts[0]);
        }
        return result;
    }
}
