package com.bradsdeals.clj;

/**
 * An interface for objects that call Clojure and therefore need an instance
 * of IClojure injected.
 */
public interface IClojureCaller {
    /**
     * Set the Clojure instance to run against.
     *
     * @param clojure the IClojure instance to run against.
     */
    void setClojure(IClojure clojure);
}
