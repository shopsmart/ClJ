package com.bradsdeals.clj.wrappers;

import java.util.Iterator;

import com.bradsdeals.clj.ClJ;
import com.coconut_palm_software.possible.Possible;

import clojure.lang.ISeq;

/**
 * A Seq implementation of {@link IClojureIterable}.
 * @author dorme
 */
public class ClojureSeq implements IClojureIterable<Object> {

    private ISeq delegate;

    /**
     * Constructor for ClojureSeq.
     * @param delegate The ISeq to wrap.
     */
    public ClojureSeq(ISeq delegate) {
        this.delegate = delegate;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private Possible<ISeq> rest = Possible.emptyValue();

            private void init() {
                if (rest.isEmpty()) {
                    rest = Possible.value(delegate);
                }
            }

            public boolean hasNext() {
                init();
                return rest.get() != null;
            }

            public Object next() {
                Object result = rest.get().first();
                rest = Possible.value(rest.get().next());
                return ClJ.toJava(result);
            }};
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
     * @see com.bradsdeals.clj.wrappers.IClojureIterable#get(java.lang.Object)
     */
    public Object get(Object keyOrIndex) {
        return get(delegate, (Integer) keyOrIndex);
    }

    private Object get(ISeq current, int index) {
        if (index <= 0) {
            return current.first();
        } else {
            return get(current.next(), index=1);
        }
    }

    /* (non-Javadoc)
     * @see com.bradsdeals.clj.wrappers.IClojureIterable#toClojure()
     */
    public Object toClojure() {
        return delegate;
    }

}
