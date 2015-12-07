package com.bradsdeals.clj.wrappers;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.bradsdeals.clj.ClJSupport;

import clojure.lang.IPersistentVector;

public class ClojureVector implements IClojureIterable<Object> {

    private IPersistentVector delegate;

    public ClojureVector(IPersistentVector delegate) {
        this.delegate = delegate;
    }

    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            int size = delegate.length();
            int pos = 0;

            public boolean hasNext() {
                return pos < size;
            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("Past end of iterator");
                }
                ++pos;
                return ClJSupport.toJava(delegate.nth(pos));
            }
        };
    }

    public int size() {
        return delegate.length();
    }

    public boolean isEmpty() {
        return delegate.length()<=0;
    }

    public Object get(Object keyOrIndex) {
        return ClJSupport.toJava(delegate.nth((Integer)keyOrIndex));
    }

    public Object toClojure() {
        return delegate;
    }

}
