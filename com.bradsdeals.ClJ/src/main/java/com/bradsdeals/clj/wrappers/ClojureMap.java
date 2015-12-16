package com.bradsdeals.clj.wrappers;

import java.util.Iterator;

import com.bradsdeals.clj.ClJ;

import clojure.lang.IPersistentMap;

/**
 * A wrapper for a Clojure Map.
 */
public class ClojureMap implements IClojureIterable<Object> {

    private IPersistentMap delegate;

    /**
     * Construct a ClojureMap.
     * @param delegate the underlying {@link IPersistentMap}.
     */
    public ClojureMap(IPersistentMap delegate) {
        this.delegate = delegate;
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.wrappers.IClojureIterable#size()
     */
    public int size() {
        return delegate.count();
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.wrappers.IClojureIterable#isEmpty()
     */
    public boolean isEmpty() {
        return delegate.count() == 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @SuppressWarnings("unchecked")
    public Iterator<Object> iterator() {
        final Iterator<Object> delegateIterator = delegate.iterator();

        return new Iterator<Object>() {
            public boolean hasNext() {
                return delegateIterator.hasNext();
            }

            public Object next() {
                return ClJ.toJava(delegateIterator.next());
            }
        };
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.wrappers.IClojureIterable#get(java.lang.Object)
     */
    public Object get(Object keyOrIndex) {
        return ClJ.toJava(delegate.valAt(keyOrIndex));
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.wrappers.IClojureIterable#toClojure()
     */
    public Object toClojure() {
        return delegate;
    }

}
