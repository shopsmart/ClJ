package com.bradsdeals.clj.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that collects all of the output into a new StringBuffer.
 *
 * We use StringBuffer instead of StringBuilder, for thread safety.
 */
public class StringBufferOutputStream extends OutputStream {
    protected StringBuffer buffer = new StringBuffer();
    protected boolean closed = false;

    synchronized public void write(int b) throws IOException {
        if (closed)
            throw new IOException("StringBufferOutputStream already closed.");
        buffer.append((char) b);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++)
            write((int) b[i]);
    }

    public void close() throws IOException {
        if (closed)
            throw new IOException("StringBufferOutputStream already closed.");
        closed = true;
    }

    public void flush() throws IOException {
        if (closed)
            throw new IOException("StringBufferOutputStream already closed.");
    }

    public boolean equals(Object obj) {
        if (obj instanceof String)
            return toString().equals(obj);
        return super.equals(obj);
    }

    synchronized public int hashCode() {
        return buffer.hashCode();
    }

    synchronized public String toString() {
        return buffer.toString();
    }
}
