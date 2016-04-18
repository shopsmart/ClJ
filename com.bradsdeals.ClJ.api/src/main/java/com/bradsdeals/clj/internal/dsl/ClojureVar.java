package com.bradsdeals.clj.internal.dsl;

public class ClojureVar {
    public final String name;
    public final Object value;

    public ClojureVar(String name, Object value) {
        this.name = name;
        this.value = value;
    }
}