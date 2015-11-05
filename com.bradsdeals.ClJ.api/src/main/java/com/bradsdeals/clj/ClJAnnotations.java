package com.bradsdeals.clj;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ClJAnnotations {
    /**
     * An annotation for Interfaces allowing a Java programmer to specify a "require"
     * clause with namespace aliases.  e.g.:
     * <code>
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str")
     *       String replace(String source,
     *                      \@Pt({String.class, Character.class, Pattern.class}) Object match,
     *                      \@Pt({String.class, Character.class}) Object replacement);
     *       \@Ns("io")
     *       void copy(\@Pt({InputStream.class, Reader.class, File.class, byte[].class, String.class}) Object input,
     *                 \@Pt({OutputStream.class, Writer.class, File.class}) Object output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Require {
        public String[] value() default {};
    }

    /**
     * An annotation for methods in Clojure interfaces allowing a Java programmer to
     * specify the alias for the namespace where the Clojure function corresponding to
     * the Java method lives.  e.g.:
     * <code>
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str")
     *       String replace(String source,
     *                      \@Pt({String.class, Character.class, Pattern.class}) Object match,
     *                      \@Pt({String.class, Character.class}) Object replacement);
     *       \@Ns("io")
     *       void copy(\@Pt({InputStream.class, Reader.class, File.class, byte[].class, String.class}) Object input,
     *                 \@Pt({OutputStream.class, Writer.class, File.class}) Object output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Ns {
        public String value() default "";
    }

    /**
     * <p>Specify the parameter types Clojure accepts in a given method argument.  The ClJ runtime
     * will type-check parameters against the specified parameter types; in the future,
     * annotation processors and/or IDE tooling will be able to check method calls
     * against these types at compile time.</p>
     *
     * <p>This annotation is not required to use ClJ.  If preferred, one can use Java method
     * overloading to achieve a similar effect and gain type safety without resorting to
     * annotations.  But some Clojure methods have so many permutations of possible
     * parameter types that adding overloaded methods for each permutation of possible
     * argument types would itself become unwieldy.  That is the use-case this annotation
     * is intended to address.</p>
     *
     * <p>E.g.:</p>
     *
     * <code>
     *   \@Require({"clojure.string :as str",
     *             "clojure.java.io :as io"})
     *   interface ClojureCalls {
     *       \@Ns("str")
     *       String replace(String source,
     *                      \@Pt({String.class, Character.class, Pattern.class}) Object match,
     *                      \@Pt({String.class, Character.class}) Object replacement);
     *       \@Ns("io")
     *       void copy(\@Pt({InputStream.class, Reader.class, File.class, byte[].class, String.class}) Object input,
     *                 \@Pt({OutputStream.class, Writer.class, File.class}) Object output) throws IOException;
     *   }
     *   private ClojureCalls clojure = ClJ.define(ClojureCalls.class);
     *
     *   // Then call methods on the 'clojure' object normally.
     * </code>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Pt {
        public Class<?>[] value() default {};
    }

}
