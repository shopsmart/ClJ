package com.bradsdeals.clj;

import static com.coconut_palm_software.possible.iterable.CollectionFactory.hashMap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

/**
 * Java helpers for calling Clojure code from Java. This class implements both
 * dynamic and type-safe methods for calling Clojure.
 *
 * The type-safe method involves creating a Java interface whose types match the
 * types of the Clojure function being called. Annotations on the Java interface
 * specify required namespaces and annotations on the interface methods specify
 * the namespace alias required to access the corresponding function.  Once this
 * is complete, you can use the {@link #define(Class)} function to create an instance
 * of the interface referencing the corresponding Clojure functions.
 *
 * The dynamic method mimics Clojure's "do" form, but allows specifying require
 * clauses with aliases at the beginning.  See {@link #doAll(String[], ClojureFn...)}
 * for details.
 *
 * @author dorme
 */
public class ClJ {

    /**
     * An annotation for Interfaces allowing a Java programmer to specify a "require"
     * clause with namespace aliases.  e.g.:
     * <code>
     *   @Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       @ns("str") String replace(String source, Pattern regex, String replacement);
     *       @ns("io") void copy(byte[] input, OutputStream output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Require {
        public String[] value() default {};
    }

    /**
     * An annotation for methods in Clojure interfaces allowing a Java programmer to
     * specify the alias for the namespace where the Clojure function corresponding to
     * the Java method lives.  e.g.:
     * <code>
     *   @Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       @ns("str") String replace(String source, Pattern regex, String replacement);
     *       @ns("io") void copy(byte[] input, OutputStream output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Ns {
        public String value() default "";
    }

    /**
     * Define an instance of a Clojure interface.  Calling methods on this instance will
     * delegate to the corresponding Clojure functions as specified by the "Require" and
     * "ns" annotations.
     * <code>
     *   @Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       @ns("str") String replace(String source, Pattern regex, String replacement);
     *       @ns("io") void copy(byte[] input, OutputStream output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @SuppressWarnings("unchecked")
    public static <T> T define(Class<T> clojureInterface) {
        Require requires = clojureInterface.getAnnotation(Require.class);
        String[] requirements = requires != null ? requires.value() : new String[] {};
        return (T) Proxy.newProxyInstance(clojureInterface.getClassLoader(),
                new Class[] {clojureInterface}, new ClojureModule(requirements));
    }

    /**
     * A "do" block for Java that dynamically calls Clojure code.  e.g.:
     * <code>
     * doAll(require("Leiningen.core.user :as l",
     *               "clojure.java.io :as io"),
     *     _("l/init"),
     *     _("l/profiles")
     *     _("io/copy", "/tmp/sourcefile", "/tmp/outputfile"));
     * </code>
     *
     * @param aliases A string array of Clojure package aliases.  A helper {@link #require(String...)}
     * function can easily provide this array.
     * @param block A varargs parameter containing the function invocations to run.  These are obtained
     * via the {@link #_(String, Object...)} factory function.  IFn objects may be resolved via the {@link #fn(String)}
     * function.
     *
     * @return The result of the last function call.
     */
    public static Object doAll(String[] aliases, ClojureFn...block) {
        Map<String, String> nsAliases = computeNsAliases(aliases);
        Object result = null;
        for (ClojureFn fn : block) {
            result = fn.invoke(nsAliases);
        }
        return result;
    }

    /**
     * A convenience factory method that returns its parameter list as a String[].  Intended to use to generate
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
    public static ClojureFn fn(String name) {
        return new ClojureFn(name);
    }

    /**
     * Return an unresolved ClojureFnInvocation for execution by a {@link #doAll(String[], ClojureFn...)}
     * form.
     *
     * @param name The namespace-aliased name of the function to call. e.g.: "s/replace".
     * @param args The function's arguments.
     * @return A ClojureFnInvocation that can be executed by {@link #doAll(String[], ClojureFn...)}.
     */
    public static ClojureFnInvocation _(String name, Object...args) {
        return new ClojureFnInvocation(name,args);
    }

    /**
     * Directly execute the fully-namespace-qualified Cloure function identified by fn, passing args
     * as arguments.
     *
     * @param fn The fully-namespace-qualified Clojure function to call.
     * @param args The arguments to pass.
     * @return the value the Clojure function returned.
     */
    public static Object $(String fn, Object...args) {
        IFn invokable = Clojure.var(fn);
        return $(invokable, args);
    }

    /**
     * Directly execute the Cloure function identified by fn, passing args as arguments.
     *
     * @param fn The Clojure function to call.
     * @param args The arguments to pass.
     * @return the value the Clojure function returned.
     */
    public static Object $(IFn fn, Object...args) {
        switch (args.length) {
        case 0:
            return fn.invoke();
        case 1:
            return fn.invoke(args[0]);
        case 2:
            return fn.invoke(args[0], args[1]);
        case 3:
            return fn.invoke(args[0], args[1], args[2]);
        case 4:
            return fn.invoke(args[0], args[1], args[2], args[3]);
        case 5:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4]);
        case 6:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
        case 7:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
        case 8:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
        case 9:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
        case 10:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
        case 11:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10]);
        case 12:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11]);
        case 13:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12]);
        case 14:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13]);
        case 15:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14]);
        case 16:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15]);
        case 17:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16]);
        case 18:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17]);
        case 19:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18]);
        case 20:
            return fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19]);
        default:
            throw new IllegalArgumentException("Too many arguments");
        }
    }


    @SuppressWarnings("unchecked")
    private static Map<String, String> computeNsAliases(String[] aliases) {
        Map<String,String> result = hashMap();
        for (String alias : aliases) {
            String[] parts = alias.split(" :as ");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Expecting 'namespace :as alias' but found: " + alias);
            }
            result.put(parts[1], parts[0]);
        }
        return result;
    }

    /**
     * Internal API.  Not for use by clients.
     */
    public static class ClojureFn {
        protected String name;
        protected IFn fn;

        public ClojureFn(String name) {
            this.name = name;
        }

        public Object invoke(Map<String, String> nsAliases) {
            if (fn != null) {
                return fn;
            }
            fn = resolve(name, "/", nsAliases);
            if (fn == null) {
                throw new IllegalArgumentException("Could not find function: " + name);
            }
            return fn;
        }
    }

    /**
     * Internal implementation detail only.  Use the #_ factory function to create
     * a function invocation and #doAll to run them.
     */
    public static class ClojureFnInvocation extends ClojureFn {
        private Object[] args;

        public ClojureFnInvocation(String name, Object... args) {
            super(name);
            this.args = args;
        }

        @Override
        public Object invoke(Map<String,String> nsAliases) {
            Object[] resolvedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof ClojureFnInvocation) {
                    resolvedArgs[i] = ((ClojureFnInvocation)arg).invoke(nsAliases);
                } else {
                    resolvedArgs[i] = arg;
                }
            }

            IFn fn = resolve(name, "/", nsAliases);
            if (fn == null) {
                throw new IllegalArgumentException("Could not find function: " + name);
            }
            return $(fn, resolvedArgs);
        }

    }

    private static IFn resolve(String name, String separatorChar, Map<String, String> nsAliases) {
        IFn fn = null;
        if (name.contains(separatorChar)) {
            String[] parts = name.split(separatorChar);
            fn = Clojure.var(nsAliases.get(parts[0]), parts[1]);
        } else {
            fn = Clojure.var(name);
        }
        return fn;
    }

    /**
     * Privae implementation detail.  Not for use by clients.
     */
    public static class ClojureModule implements InvocationHandler {
        private Map<String, String> nsAliases;
        @SuppressWarnings("unchecked")
        private Map<String,IFn> fnCache = hashMap();

        protected ClojureModule(String... nsAliases) {
            this.nsAliases = computeNsAliases(nsAliases);
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            IFn fn = fnCache.get(method.getName());
            if (fn == null) {
                Ns alias = method.getAnnotation(Ns.class);
                if (alias == null) {
                    try {
                        fn = Clojure.var(method.getName());
                    } catch (Exception e) {
                        throw new IllegalStateException("Function: " + method.getName() + "is not defined in the core namespace.", e);
                    }
                } else {
                    String namespace = nsAliases.get(alias.value());
                    if (namespace == null) {
                        throw new IllegalStateException(alias.value() + " is not aliased to any namespace.");
                    }
                    try {
                        fn = Clojure.var(namespace, method.getName());
                    } catch (Exception e) {
                        throw new IllegalStateException("Undefined function: " + namespace + "/" + method.getName(), e);
                    }
                }
                if (fn == null) {
                    throw new IllegalStateException("Method : " + method.getName() + " is not defined in the specified Clojure modules");
                }
                fnCache.put(method.getName(), fn);
            }
            return $(fn, args);
        }
    }

}
