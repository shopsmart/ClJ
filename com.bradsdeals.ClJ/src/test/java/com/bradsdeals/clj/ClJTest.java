package com.bradsdeals.clj;

import static com.bradsdeals.clj.ClJ.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import com.bradsdeals.clj.ClJAnnotations.Ns;
import com.bradsdeals.clj.ClJAnnotations.Pt;
import com.bradsdeals.clj.ClJAnnotations.Require;
import com.bradsdeals.clj.io.StringBufferOutputStream;

import junit.framework.TestCase;

public class ClJTest extends TestCase {

    @Require({ "clojure.string :as str", "clojure.java.io :as io" })
    interface ClojureCalls {
        @Ns("str")
        String replace(String source, @Pt({ String.class, Character.class, Pattern.class }) Object match,
                @Pt({ String.class, Character.class }) Object replacement);

        @Ns("io")
        void copy(@Pt({ InputStream.class, Reader.class, File.class, byte[].class, String.class }) Object input,
                @Pt({ OutputStream.class, Writer.class, File.class }) Object output) throws IOException;
    }
    private ClojureCalls clojure = ClJ.define(ClojureCalls.class);

    private static final String INPUT = "I see because I C";

    public void testClJ_replace() throws Exception {
        String result = clojure.replace(INPUT, Pattern.compile("C"), "see");
        assertEquals("I see because I see", result);
    }

    public void testClJ_copy() throws Exception {
        byte[] input = INPUT.getBytes();
        final StringBufferOutputStream output = new StringBufferOutputStream();
        clojure.copy(input, output);
        assertEquals(INPUT, output.toString());
    }

    public void testInvalidParameter_IllegalArgumentException() throws Exception {
        try {
            clojure.replace("source", "invalidInput".getBytes(), "replacement");
            fail("Should have thrown IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            //success
        }
    }

    public void testDoReplace() throws Exception {
        String result = doAll(require("clojure.string :as str"), $("str/replace", INPUT, Pattern.compile("C"), "see"));
        assertEquals("I see because I see", result);
    }

    public void testDoTwo() throws Exception {
        byte[] input = INPUT.getBytes();
        final StringBufferOutputStream output = new StringBufferOutputStream();

        String result = doAll(require("clojure.string :as str", "clojure.java.io :as io"), $("io/copy", input, output),
                $("str/replace", INPUT, Pattern.compile("C"), "see"));

        assertEquals(INPUT, output.toString());
        assertEquals("I see because I see", result);
    }

    public void testDoWithLet() throws Exception {
        byte[] input = INPUT.getBytes();
        final StringBufferOutputStream output = new StringBufferOutputStream();

        String result = doAll(require("clojure.string :as str", "clojure.java.io :as io", "clojure.core :as core"),
                let(vars("see", $("str/replace", INPUT, Pattern.compile("C"), "see")),
                        $("io/copy", input, output),
                        $("core/str", "see", " because ", "see")));

        assertEquals(INPUT, output.toString());
        assertEquals("I see because I see because I see because I see", result);
    }

    public void testDoWithSubLetAndShadowing() throws Exception {
        byte[] input = INPUT.getBytes();
        final StringBufferOutputStream output = new StringBufferOutputStream();

        String result =
                doAll(require("clojure.string :as str",
                        "clojure.java.io :as io",
                        "clojure.core :as core"),
                    let(vars("see", $("str/replace", INPUT, Pattern.compile("C"), "see")),
                            $("io/copy", input, output),
                            $("core/str", "see", " because ", "see"),
                            let(vars("see", "I C"),
                                    $("core/str", "see", " because ", "see"))));

        assertEquals(INPUT, output.toString());
        assertEquals("I C because I C", result);
    }
}
