package com.bradsdeals.ClJ;

import static com.coconut_palm_software.possible.iterable.CollectionFactory.*;

import java.util.Map;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class ClJ {

    @SuppressWarnings("unchecked")
    public static <T> T define(Class<T> clojureInterface, String... nsAliases) {
        return (T) Proxy.newProxyInstance(clojureInterface.getClassLoader(),
                new Class[] {clojureInterface}, new ClojureModule(nsAliases));
    }

    /**
     * <code>
     * doAll(require("Leiningen.core.user :as u"),
     *     _("u/init"),
     *     _("u/profiles"));
     * </code>
     *
     * @param aliases
     * @param block
     * @return
     */
    public static Object doAll(String[] aliases, ClojureFn...block) {
        Map<String, String> nsAliases = computeNsAliases(aliases);
        Object result = null;
        for (ClojureFn fn : block) {
            result = fn.invoke(nsAliases);
        }
        return result;
    }

    public static String[] require(String...aliases) {
        return aliases;
    }

    public static ClojureFn fn(String name) {
        return new ClojureFn(name);
    }

    public static ClojureFnInvocation _(String name, Object...args) {
        return new ClojureFnInvocation(name,args);
    }

    public static Object $(String fn, Object...args) {
        IFn invokable = Clojure.var(fn);
        return $(invokable, args);
    }

    public static Object $(IFn fn, Object...args) {
        switch (args.length) {
        case 0:
            fn.invoke();
        case 1:
            fn.invoke(args[0]);
        case 2:
            fn.invoke(args[0], args[1]);
        case 3:
            fn.invoke(args[0], args[1], args[2]);
        case 4:
            fn.invoke(args[0], args[1], args[2], args[3]);
        case 5:
            fn.invoke(args[0], args[1], args[2], args[3], args[4]);
        case 6:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
        case 7:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
        case 8:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
        case 9:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
        case 10:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
        case 11:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10]);
        case 12:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11]);
        case 13:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12]);
        case 14:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13]);
        case 15:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14]);
        case 16:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15]);
        case 17:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16]);
        case 18:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17]);
        case 19:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18]);
        case 20:
            fn.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
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
                fn = resolve(method.getName(), "$", nsAliases);
                if (fn == null) {
                    throw new IllegalStateException("Method : " + method.getName() + " is not defined in the specified Clojure modules");
                }
                fnCache.put(method.getName(), fn);
            }
            return $(fn, args);
        }
    }

}
