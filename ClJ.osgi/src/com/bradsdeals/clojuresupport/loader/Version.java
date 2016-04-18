package com.bradsdeals.clojuresupport.loader;

import com.coconut_palm_software.possible.Possible;

public class Version implements Comparable<Version> {
    public Version(String source, int major, int minor, int micro) {
        this.source = source;
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.versionHash = major * (3 * 1000) + minor * (2 * 1000) + micro;
    }

    final int versionHash;
    private final String source;
    public final int major;
    public final int minor;
    public final int micro;

    @Override
    public int compareTo(Version o) {
        return versionHash - o.versionHash;
    }

    @Override
    public String toString() {
        return source;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (! (obj instanceof Version)) {
            return false;
        } else return obj.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return versionHash;
    }

    public static final Possible<Version> EMPTY = Possible.emptyValue();
}
