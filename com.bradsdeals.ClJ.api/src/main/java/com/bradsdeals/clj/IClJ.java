package com.bradsdeals.clj;

import com.bradsdeals.clj.ClJAnnotations.Ns;
import com.bradsdeals.clj.ClJAnnotations.Pt;
import com.bradsdeals.clj.ClJAnnotations.Require;

/**
 * An Interface defining helpers for calling Clojure code from Java. This defines
 * dynamic and type-safe methods for calling Clojure.<p>
 *
 * It is also separated into API and implementation packages, where only the
 * implementation package has a direct Clojure dependency.  This is to enable
 * ClJ to optionally be used in environments like OSGi where each instance of
 * Clojure must be isolated inside its own classloader. (Thanks to ShimDandy
 * for the techniques required to do this.  See: https://github.com/projectodd/shimdandy)<p>
 *
 * The initial API creates a Java interface whose method names and types match the
 * names/types of the Clojure function being called. Annotations on the Java interface
 * specify required namespaces and annotations on the interface methods specify
 * the namespace alias required to access the corresponding function.  Once this
 * is complete, you can use the {@link #define(Class, String...)} function to
 * create an instance of the interface referencing the corresponding Clojure
 * functions.<p>
 * @author dorme
 */
public interface IClJ extends IClojure {
    /*
     * Define Java interfaces corresponding to Clojure functions and call Clojure from
     * Java as if it was Java.
     */

    /**
     * Define an instance of a Clojure interface.  Calling methods on this instance will
     * delegate to the corresponding Clojure functions as specified by the "Require" and
     * "Ns" annotations.
     * <code>
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str") String replace(String source, Pattern regex, String replacement);
     *       \@Ns("io") void copy(byte[] input, OutputStream output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     *
     * @see Require
     * @see Ns
     * @see Pt
     *
     * @param clojureInterface The Clojure interface to define.
     * @param loadPackages Zero or more Clojure source code packages to load in order to define the interface
     * @param <T> The interface type.
     * @return T an instance of clojureInterface.
     */
    <T> T define(Class<T> clojureInterface, String...loadPackages);

}
