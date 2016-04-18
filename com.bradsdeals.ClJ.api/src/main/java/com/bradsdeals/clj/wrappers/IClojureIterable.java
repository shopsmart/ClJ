package com.bradsdeals.clj.wrappers;

/**
 * Interface IClojureIterable.  A generic iterable class for Clojure collections returned
 * by the Java API.
 *
 * @author dorme
 *
 * @param <T> The type of object to return; in practice always java.lang.Object.
 */
public interface IClojureIterable<T> extends Iterable<T> {
    /**
     * Return the underlying collection's size.
     * @return the underlying collection's size.
     */
    int size();
    /**
     * Return true if the underlying collection is empty and false otherwise.
     * @return true if the underlying collection is empty and false otherwise.
     */
    boolean isEmpty();
    /**
     * Returns the object referred to by the specified key or index.
     *
     * @param keyOrIndex The key or index to look up.
     * @return the referenced object or throws {@link ArrayIndexOutOfBoundsException} on failure.
     */
    Object get(Object keyOrIndex);
    /**
     * Return the underlying Clojure object.
     * @return the underlying Clojure object.
     */
    Object toClojure();
}
