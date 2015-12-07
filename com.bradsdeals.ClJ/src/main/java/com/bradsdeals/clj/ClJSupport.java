package com.bradsdeals.clj;

import static com.coconut_palm_software.possible.iterable.CollectionFactory.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.bradsdeals.clj.ClJAnnotations.Ns;
import com.bradsdeals.clj.ClJAnnotations.Pt;
import com.bradsdeals.clj.ClJAnnotations.Require;
import com.bradsdeals.clj.internal.dsl.ClojureFn;
import com.bradsdeals.clj.internal.dsl.ClojureFnInvocation;
import com.bradsdeals.clj.internal.dsl.ClojureFnLiteral;
import com.bradsdeals.clj.internal.dsl.ClojureLet;
import com.bradsdeals.clj.internal.dsl.ClojureVar;
import com.bradsdeals.clj.wrappers.ClojureMap;
import com.bradsdeals.clj.wrappers.ClojureSeq;
import com.bradsdeals.clj.wrappers.ClojureVector;
import com.coconut_palm_software.possible.Nulls;
import com.coconut_palm_software.possible.Possible;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.ISeq;
import clojure.lang.RT;
import clojure.lang.Seqable;
import clojure.lang.Symbol;
import clojure.lang.Var;

/**
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
 * @author dorme
 */
public class ClJSupport implements IClJ {

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#init(java.lang.ClassLoader)
     */
    @SuppressWarnings("rawtypes")
    public void init(final ClassLoader privateClassloader) {
        Exception ex = null;
        try {
            Field dvalField = Var.class.getDeclaredField("dvals");
            dvalField.setAccessible(true);
            final LocalThreadData threadData = new LocalThreadData(privateClassloader, (ThreadLocal)dvalField.get(null));
            localThreadData = Possible.value(threadData);
            safeCall(new Callable<Object>() {
                public Object call() throws Exception {
                    IFn require = RT.var("clojure.core", "require");
                    IFn resolve = RT.var("clojure.core", "resolve");
                    threadData.setResolvers(require, resolve);
                    clojure.lang.Compiler.LOADER.bindRoot(privateClassloader);
                    return null;
                }
            });
        } catch (IllegalAccessException e) {
            ex = e;
        } catch (NoSuchFieldException e) {
            ex = e;
        }

        if (ex != null) {
            throw new RuntimeException("Failed to access Var.dvals", ex);
        }
    }


    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#close()
     */
    public void close() {
        invoke("clojure.core/shutdown-agents");
        localThreadData = Possible.emptyValue();
    }


    /*
     * Define Java interfaces corresponding to Clojure functions and call Clojure from
     * Java as if it was Java.
     */

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#define(java.lang.Class, java.lang.String[])
     */
    public <T> T define(Class<T> clojureInterface, String...loadPackages) {
        if (localThreadData.hasValue()) {
            return define(clojureInterface, localThreadData.get().classloader, loadPackages);
        } else {
            return define(clojureInterface, clojureInterface.getClassLoader(), loadPackages);
        }
    }

     // Implementation detail
    @SuppressWarnings("unchecked")
    private <T> T define(Class<T> clojureInterface, ClassLoader classloader, String[] loadPackages) {
        Require requires = clojureInterface.getAnnotation(Require.class);
        String[] requirements = requires != null ? requires.value() : new String[] {};
        return (T) Proxy.newProxyInstance(classloader,
                new Class[] {clojureInterface}, new ClojureModule(this, loadPackages, requirements));
    }


    /*
     * The dynamic Clojure DSL is implemented here
     */

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#doAll(java.lang.String[], com.bradsdeals.clj.internal.dsl.ClojureFn[])
     */
    @SuppressWarnings("unchecked")
    public <T> T doAll(String[] aliases, ClojureFn...block) {
        Map<String, String> nsAliases = computeNsAliases(aliases);
        Object result = null;
        for (ClojureFn fn : block) {
            if (fn instanceof IClojureCaller) {
                ((IClojureCaller)fn).setClojure(this);
            }
            result = fn.invoke(nsAliases, new LinkedList<HashMap<String,Object>>());
        }
        return (T)result;
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#let(com.bradsdeals.clj.internal.dsl.ClojureVar[], com.bradsdeals.clj.internal.dsl.ClojureFn[])
     */
    public ClojureLet let(ClojureVar[] vars, ClojureFn...block) {
        return new ClojureLet(vars, block);
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#vars(java.lang.Object[])
     */
    public ClojureVar[] vars(Object...nvPairs) {
        if (nvPairs.length % 2 != 0) {
            throw new IllegalArgumentException("There must be an even number of values in a let binding");
        }
        ArrayList<ClojureVar> result = new ArrayList<ClojureVar>(nvPairs.length/2);
        for (int i=0; i <= nvPairs.length-2; i += 2) {
            result.add(new ClojureVar((String)nvPairs[i], nvPairs[i+1]));
        }
        return result.toArray(new ClojureVar[nvPairs.length/2]);
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#require(java.lang.String[])
     */
    public String[] require(String...aliases) {
        return aliases;
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#fn(java.lang.String)
     */
    public ClojureFnLiteral fn(String name) {
        return new ClojureFnLiteral(name);
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClJ#$(java.lang.String, java.lang.Object[])
     */
    public ClojureFnInvocation $(String name, Object...args) {
        return new ClojureFnInvocation(name,args);
    }


    /*
     * Functions for accessing Clojure directly
     */

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#var(java.lang.String)
     */
    public Object var(final String fullyQualifiedName) {
        Object invokable = safeCall(new Callable<Object>() {
            public Object call() throws Exception {
                if (localThreadData.hasValue()) {
                    return localThreadData.get().fn(fullyQualifiedName);
                }
                return Clojure.var(fullyQualifiedName);
            }});
        return invokable;
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#var(java.lang.String, java.lang.String)
     */
    public Object var(final String namespace, final String fn) {
        return var(namespace + "/" + fn);
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#invoke(java.lang.String, java.lang.Object[])
     */
    @SuppressWarnings("unchecked")
    public <T> T invoke(final String fn, Object...args) {
        Object invokable = var(fn);
        if (invokable instanceof IFn) {
            return (T) invoke((IFn) invokable, args);
        } else {
            if (args.length > 0) {
                throw new IllegalArgumentException(fn + " is a " + invokable.getClass().getName() + " and cannot be called as a function with arguments.");
            }
            return (T) invokable;
        }
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#invoke(java.lang.Object, java.lang.Object[])
     */
    @SuppressWarnings("unchecked")
    public <T> T invoke(final Object fnObject, final Object...args) {
        final IFn fn = (IFn) fnObject;
        return toJava(safeCall(new Callable<T>() {
            public T call() throws Exception {
                return (T) invokeInternal(fn, args);
            }
        }));
    }

    /**
     * Turn a Clojure result into an object that is easier for Java to handle.
     *
     * @param result The result value to wrap.
     * @param <T> The type of the return value.
     * @return If the result is a core Clojure collection, returns an instanceof IClojureIterable, else returns result.
     */
    @SuppressWarnings("unchecked")
    public static <T> T toJava(Object result) {
        if (result instanceof IPersistentMap) {
            return (T) new ClojureMap((IPersistentMap) result);
        } else if (result instanceof IPersistentVector) {
            return (T) new ClojureVector((IPersistentVector) result);
        } else if (result instanceof ISeq) {
            return (T) new ClojureSeq((ISeq) result);
        } else if (result instanceof Seqable) {
            return (T) new ClojureSeq(((Seqable)result).seq());
        }
        return (T) result;
    }

    private Object invokeInternal(Object fnObj, Object...args) {
        if (!(fnObj instanceof IFn)) {
            return fnObj;
        }
        IFn fn = (IFn) fnObj;
        if (args == null) {
            return fn.invoke();
        }
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


    /* (non-Javadoc)
     * @see com.bradsdeals.clj.IClojure#computeNsAliases(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> computeNsAliases(String[] aliases) {
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
     * Private implementation detail for the Clojure / Java Interface bridge.  Not for use by clients.
     */
    public static class ClojureModule implements InvocationHandler {
        private IClojure clj;
        private Map<String, String> nsAliases;
        @SuppressWarnings("unchecked")
        private Map<String,IFn> fnCache = hashMap();

        protected ClojureModule(IClojure clj, String[] loadPackages, String... nsAliases) {
            this.clj = clj;
            this.nsAliases = clj.computeNsAliases(nsAliases);
            for (String ns : loadPackages) {
                loadNamespaceFromClasspath(ns);
            }
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            IFn fn = fnCache.get(method.getName());
            if (fn == null) {
                Ns alias = method.getAnnotation(Ns.class);
                if (alias == null) {
                    try {
                        fn = (IFn) clj.var(method.getName());
                    } catch (Exception e) {
                        throw new IllegalStateException("Function: " + method.getName() + "is not defined in the core namespace.", e);
                    }
                } else {
                    String namespace = nsAliases.get(alias.value());
                    if (namespace == null) {
                        throw new IllegalStateException(alias.value() + " is not aliased to any namespace.");
                    }
                    try {
                        fn = (IFn) clj.var(namespace + "/" + method.getName());
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
            return clj.invoke(fn, args);
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

        private IFn loadNamespace = null;

        private void loadNamespaceFromClasspath(String packagePath) {
            if (loadNamespace == null) {
                loadNamespace = (IFn) clj.var("clojure.core/load");
            }
            loadNamespace.invoke(packagePath);
        }
    }


    /*
     * Support classloader-private instances of the Clojure runtime
     */

    private <T> T safeCall(Callable<T> runInClojure) {
        if (localThreadData.hasValue()) {
            ClassLoader origloader = localThreadData.get().preInvoke();
            try {
                return runInClojure.call();
            } catch (Exception e) {
                throw new RuntimeException("Exception calling Clojure", e);
            } finally {
                localThreadData.get().postInvoke(origloader);
            }
        } else {
            try {
                return runInClojure.call();
            } catch (Exception e) {
                throw new RuntimeException("Exception calling Clojure", e);
            }
        }
    }

    private class LocalThreadData {
        public ClassLoader classloader;
        @SuppressWarnings("rawtypes")
        public ThreadLocal dvals;
        private IFn require;
        private IFn resolve;

        public final ThreadLocal<AtomicLong> callDepth = new ThreadLocal<AtomicLong>() {
          protected AtomicLong initialValue() {
              return new AtomicLong(0);
          }
        };

        @SuppressWarnings("rawtypes")
        public LocalThreadData(ClassLoader classloader, ThreadLocal dvals) {
            this.classloader = classloader;
            this.dvals = dvals;
        }

        private void assertInitialized(IFn require, IFn resolve) {
            Nulls.assertNotNull(require, "require");
            Nulls.assertNotNull(resolve, "resolve");
        }

        public void setResolvers(IFn require, IFn resolve) {
            assertInitialized(require, resolve);
            this.require = require;
            this.resolve = resolve;
        }

        private ClassLoader preInvoke() {
            final ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(localThreadData.get().classloader);
            callDepth.get().getAndIncrement();
            return originalClassloader;
        }

        private void postInvoke(ClassLoader loader) {
            if (callDepth.get().decrementAndGet() == 0) {
                dvals.remove();    // Fixed according to http://dev.clojure.org/jira/browse/CLJ-1125???
                callDepth.remove();
            }
            Thread.currentThread().setContextClassLoader(loader);
        }

        public IFn fn(String namespacedFunction) {
            assertInitialized(require, resolve);
            Var var = (Var)resolve.invoke(Symbol.create(namespacedFunction));
            if (var == null) {
                String[] parts = namespacedFunction.split("/");
                require.invoke(Symbol.create(parts[0]));
                var = RT.var(parts[0], parts[1]);
            }
            return var;
        }

    }

    private Possible<LocalThreadData> localThreadData = Possible.emptyValue();
}
