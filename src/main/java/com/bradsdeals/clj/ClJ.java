package com.bradsdeals.clj;

import static com.coconut_palm_software.possible.iterable.CollectionFactory.hashMap;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str")
     *       String replace(String source,
     *                      \@Pt({String.class, Character.class, Pattern.class}) Object match,
     *                      \@Pt({String.class, Character.class}) Object replacement);
     *       \@Ns("io")
     *       void copy(\@Pt({InputStream.class, Reader.class, File.class, byte[].class, String.class}) Object input,
     *                 \@Pt({OutputStream.class, Writer.class, File.class}) Object output) throws IOException;
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
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str")
     *       String replace(String source,
     *                      \@Pt({String.class, Character.class, Pattern.class}) Object match,
     *                      \@Pt({String.class, Character.class}) Object replacement);
     *       \@Ns("io")
     *       void copy(\@Pt({InputStream.class, Reader.class, File.class, byte[].class, String.class}) Object input,
     *                 \@Pt({OutputStream.class, Writer.class, File.class}) Object output) throws IOException;
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
     * <p>Specify the parameter types Clojure accepts in a given method argument.  The ClJ runtime
     * will type-check parameters against the specified parameter types; in the future,
     * annotation processors and/or IDE tooling will be able to check method calls
     * against these types at compile time.</p>
     *
     * <p>This annotation is not required to use ClJ.  If preferred, one can use Java method
     * overloading to achieve a similar effect and gain type safety without resorting to
     * annotations.  But some Clojure methods have so many permutations of possible
     * parameter types that adding overloaded methods for each permutation of possible
     * argument types would itself become unwieldy.  That is the use-case this annotation
     * is intended to address.</p>
     *
     * <p>E.g.:</p>
     *
     * <code>
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str")
     *       String replace(String source,
     *                      \@Pt({String.class, Character.class, Pattern.class}) Object match,
     *                      \@Pt({String.class, Character.class}) Object replacement);
     *       \@Ns("io")
     *       void copy(\@Pt({InputStream.class, Reader.class, File.class, byte[].class, String.class}) Object input,
     *                 \@Pt({OutputStream.class, Writer.class, File.class}) Object output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Pt {
        public Class<?>[] value() default {};
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
    @SuppressWarnings("unchecked")
    public static <T> T doAll(String[] aliases, ClojureFn...block) {
        Map<String, String> nsAliases = computeNsAliases(aliases);
        Object result = null;
        for (ClojureFn fn : block) {
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
        return new ClojureFnLiteral(name);
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
    public static Object invoke(String fn, Object...args) {
        Object invokable = Clojure.var(fn);
        if (invokable instanceof IFn) {
            return invoke((IFn) invokable, args);
        } else {
            if (args.length > 0) {
                throw new IllegalArgumentException(fn + " is a " + invokable.getClass().getName() + " and cannot be called as a function with arguments.");
            }
            return invokable;
        }
    }

    /**
     * Directly execute the Clojure function identified by fn, passing args as arguments.
     *
     * @param fn The Clojure function to call.
     * @param args The arguments to pass.
     * @return the value the Clojure function returned.
     */
    public static Object invoke(IFn fn, Object...args) {
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

    public static abstract class ClojureFn {
        abstract <T> T invoke(Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars);

        protected Object resolve(String name, String separatorChar, Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars) {
            Object fn = null;

            fn = findVar(name, vars);
            if (fn != null) {
                return fn;
            }

            if (name.contains(separatorChar)) {
                String[] parts = name.split(separatorChar);
                fn = Clojure.var(nsAliases.get(parts[0]), parts[1]);
            } else {
                fn = Clojure.var(name);
            }

            return fn;
        }

        protected Object findVar(String name, LinkedList<HashMap<String, Object>> vars) {
            for (Map<String, Object> varMap : vars) {
                if (varMap.containsKey(name)) {
                    return varMap.get(name);
                }
            }
            return null;
        }
    }

    private static class ClojureVar {
        public final String name;
        public final Object value;

        public ClojureVar(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class ClojureLet extends ClojureFn {
        private final ClojureVar[] newVars;
        private final ClojureFn[] block;

        public ClojureLet(ClojureVar[] vars, ClojureFn...block) {
            this.newVars = vars;
            this.block = block;
        }

        @SuppressWarnings("unchecked")
        @Override
        <T> T invoke(Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars) {
            Object result = null;
            try {
                HashMap<String,Object> resolvedVars = resolveVars(nsAliases, vars, newVars);
                vars.addFirst(resolvedVars);
                for (ClojureFn fn : block) {
                    result = fn.invoke(nsAliases, vars);
                }
            } finally {
                vars.removeFirst();
            }
            return (T) result;
        }

        protected HashMap<String,Object> resolveVars(Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars, ClojureVar[] newVars) {
            HashMap<String,Object> result = new HashMap<String,Object>();
            for (ClojureVar clojureVar : newVars) {
                if (result.containsKey(clojureVar.name)) {
                    throw new IllegalStateException("Cannot modify an existing var: " + clojureVar.name);
                }
                Object invocationResult = null;
                if (clojureVar.value instanceof ClojureFn) {
                    invocationResult = ((ClojureFn)clojureVar.value).invoke(nsAliases, vars);
                } else {
                    invocationResult = clojureVar.value;
                }
                result.put(clojureVar.name, invocationResult);
            }
            return result;
        }
    }

    /**
     * Internal API.  Not for use by clients.
     */
    public static class ClojureFnLiteral extends ClojureFn {
        protected String name;
        protected Object fn;

        public ClojureFnLiteral(String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public <T> T invoke(Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars) {
            if (fn != null) {
                return (T) fn;
            }
            fn = resolve(name, "/", nsAliases, vars);
            if (fn == null) {
                throw new IllegalArgumentException("Could not find function: " + name);
            }
            if (! (fn instanceof IFn)) {
                throw new IllegalStateException("Expected Clojure function but found: " + fn.getClass().getName() + " : " + fn.toString());
            }
            return (T)fn;
        }
    }

    /**
     * Internal implementation detail only.  Use the #_ factory function to create
     * a function invocation and #doAll to run them.
     */
    public static class ClojureFnInvocation extends ClojureFnLiteral {
        private Object[] args;

        public ClojureFnInvocation(String name, Object... args) {
            super(name);
            this.args = args;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T invoke(Map<String,String> nsAliases, LinkedList<HashMap<String, Object>> vars) {
            Object[] resolvedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof ClojureFn) {
                    resolvedArgs[i] = ((ClojureFn)arg).invoke(nsAliases, vars);
                } else if (arg instanceof String) {
                    resolvedArgs[i] = findVar((String) arg, vars);
                    if (resolvedArgs[i] == null) {
                        resolvedArgs[i] = arg;
                    }
                } else {
                    resolvedArgs[i] = arg;
                }
            }

            Object fn = resolve(name, "/", nsAliases, vars);
            if (fn == null) {
                throw new IllegalArgumentException("Could not find function: " + name);
            }
            if (fn instanceof IFn) {
                return (T) ClJ.invoke((IFn)fn, resolvedArgs);
            } else {
                return (T)fn;
            }
        }
    }

    /**
     * Private implementation detail.  Not for use by clients.
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
            validateArgTypes(method, args);
            return ClJ.invoke(fn, args);
        }

        private void validateArgTypes(Method method, Object[] args) {
            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int argNum = 0; argNum < parameterAnnotations.length; argNum++) {
                Annotation[] annotations = parameterAnnotations[argNum];
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Pt) {
                        boolean found = false;
                        Pt t = (Pt) annotation;
                        final Class<?>[] valueTypes = t.value();
                        for (Class<?> expectedType : valueTypes) {
                            if (expectedType.isAssignableFrom(args[argNum].getClass())) {
                                found=true;
                            }
                        }
                        if (!found) {
                            throw new IllegalArgumentException("Clojure function " + method.getName() + ", argument " + argNum + " (0-based) is type " + args[argNum].getClass().getName() + "; expected one of: " + getValueTypeNames(valueTypes));
                        }
                    }
                }
            }
        }

        private String getValueTypeNames(Class<?>[] valueTypes) {
            StringBuffer result = new StringBuffer(valueTypes[0].getName());
            for (int i = 1; i < valueTypes.length; i++) {
                result.append(", " + valueTypes[i].getName());
            }
            return result.toString();
        }
    }

}
