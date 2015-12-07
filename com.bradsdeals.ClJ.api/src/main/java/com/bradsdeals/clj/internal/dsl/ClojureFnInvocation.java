package com.bradsdeals.clj.internal.dsl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ClojureFnInvocation extends ClojureFnLiteral {
    private Object[] args;

    public ClojureFnInvocation(String name, Object... args) {
        super(name);
        this.args = args;
    }

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
        return clojure.invoke(fn, resolvedArgs);
    }
}
