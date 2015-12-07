package com.bradsdeals.clj.internal.dsl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ClojureFnLiteral extends ClojureFn {
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
        return (T)fn;
    }
}
