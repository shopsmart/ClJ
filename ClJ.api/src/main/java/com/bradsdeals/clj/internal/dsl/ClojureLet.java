package com.bradsdeals.clj.internal.dsl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.bradsdeals.clj.IClojureCaller;

public class ClojureLet extends ClojureFn implements IClojureCaller {
    private final ClojureVar[] newVars;
    private final ClojureFn[] block;

    public ClojureLet(ClojureVar[] vars, ClojureFn...block) {
        this.newVars = vars;
        this.block = block;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars) {
        Object result = null;
        try {
            HashMap<String,Object> resolvedVars = resolveVars(nsAliases, vars, newVars);
            vars.addFirst(resolvedVars);
            for (ClojureFn fn : block) {
                if (fn instanceof IClojureCaller) {
                    ((IClojureCaller)fn).setClojure(clojure);
                }
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
                ClojureFn fn = (ClojureFn) clojureVar.value;
                fn.setClojure(clojure);
                invocationResult = fn.invoke(nsAliases, vars);
            } else {
                invocationResult = clojureVar.value;
            }
            result.put(clojureVar.name, invocationResult);
        }
        return result;
    }
}

