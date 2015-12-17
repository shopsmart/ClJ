package com.bradsdeals.clj.internal.dsl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.bradsdeals.clj.IClojure;
import com.bradsdeals.clj.IClojureCaller;

public abstract class ClojureFn implements IClojureCaller {

    protected IClojure clojure;

    public void setClojure(IClojure clojure) {
        this.clojure = clojure;
    }

    public abstract <T> T invoke(Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars);

    protected Object resolve(String name, String separatorChar, Map<String, String> nsAliases, LinkedList<HashMap<String, Object>> vars) {
        Object fn = null;

        fn = findVar(name, vars);
        if (fn != null) {
            return fn;
        }

        if (name.contains(separatorChar)) {
            String[] parts = name.split(separatorChar);
            fn = clojure.var(nsAliases.get(parts[0]), parts[1]); // FIXME: NPE here if name isn't found
        } else {
            fn = clojure.var(name);
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

