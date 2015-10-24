package com.bradsdeals.clj;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.bradsdeals.clj.ClJ.Require;
import com.bradsdeals.clj.ClJ.Ns;
import com.bradsdeals.clj.io.StringBufferOutputStream;

import static com.bradsdeals.clj.ClJ.*;

import junit.framework.TestCase;

public class ClJTest extends TestCase {

    private static final String INPUT = "I see because I C";

    @Require({"clojure.string :as str",
              "clojure.java.io :as io"})
    interface ClojureCalls {
        @Ns("str") String replace(String source, Pattern regex, String replacement);
        @Ns("io") void copy(byte[] input, OutputStream output) throws IOException;
    }
    private ClojureCalls clojure = ClJ.define(ClojureCalls.class);

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

    public void testDoReplace() throws Exception {
        String result = (String)
                doAll(require("clojure.string :as str"),
                        _("str/replace", INPUT, Pattern.compile("C"), "see"));
        assertEquals("I see because I see", result);
    }

    public void testDoTwo() throws Exception {
        byte[] input = INPUT.getBytes();
        final StringBufferOutputStream output = new StringBufferOutputStream();

        String result = (String)
            doAll(require("clojure.string :as str",
                          "clojure.java.io :as io"),
                    _("io/copy", input, output),
                    _("str/replace", INPUT, Pattern.compile("C"), "see"));

        assertEquals(INPUT, output.toString());
        assertEquals("I see because I see", result);
    }
}
