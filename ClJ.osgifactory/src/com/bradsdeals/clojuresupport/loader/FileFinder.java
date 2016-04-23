package com.bradsdeals.clojuresupport.loader;

import static com.coconut_palm_software.possible.iterable.CollectionFactory.*;
import static com.coconut_palm_software.possible.iterable.FluentIterable.*;

import java.io.File;
import java.io.IOException;

import com.coconut_palm_software.possible.Possible;
import com.coconut_palm_software.possible.iterable.F;
import com.coconut_palm_software.possible.iterable.F2;

public class FileFinder {

    private static String MAVEN_CLOJURE_LOCATION = ".m2/repository/org/clojure/clojure";
    private static String LEIN_LOCATION = ".lein/self-installs";

    public static String getHomeDirectory() {
        return System.getProperty("user.home");
    }

    private static class ParseMavenVersionDirectory implements F<String, Possible<Version>> {
        @Override
        public Possible<Version> apply(String subdir) {
            String[] parts = subdir.split("\\.|-");
            if (parts.length == 3) {
                try {
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1]);
                    int micro = Integer.parseInt(parts[2]);
                    return Possible.value(new Version(subdir, major, minor, micro));
                } catch (NumberFormatException e) {
                    return Possible.emptyValue(new IllegalStateException("Expected version of form major.minor.micro but found :" + subdir, e));
                }
            }
            return Possible.emptyValue();
        }
    }
    private static ParseMavenVersionDirectory parseMavenVersionDirectory = new ParseMavenVersionDirectory();

    private static class ParseLeinVersion implements F<String, Possible<Version>> {
        @Override
        public Possible<Version> apply(String filename) {
            String[] parts = filename.split("\\.|-");
            if (parts.length == 6) {
                try {
                    int major = Integer.parseInt(parts[1]);
                    int minor = Integer.parseInt(parts[2]);
                    int micro = Integer.parseInt(parts[3]);
                    return Possible.value(new Version(filename, major, minor, micro));
                } catch (NumberFormatException e) {
                    return Possible.emptyValue(new IllegalStateException("Expected version of form major.minor.micro but found :" + filename, e));
                }
            }
            return Possible.emptyValue();
        }
    }
    private static ParseLeinVersion parseLeinVersion = new ParseLeinVersion();

    private static class FindHighestVersion implements F2<Possible<Version>, Version, Possible<Version>> {
        @Override
        public Possible<Version> apply(Possible<Version> currentResult, Version v) {
            if (currentResult.isEmpty()) {
                return Possible.value(v);
            } else if (v.compareTo(currentResult.get()) > 0) {
                return Possible.value(v);
            }
            return currentResult;
        }
    }
    private static FindHighestVersion findHighestVersion = new FindHighestVersion();

    public static Possible<File> findNewestClojureLib() throws IOException {
        final File clojureDir = new File(getHomeDirectory() + File.separator + MAVEN_CLOJURE_LOCATION);
        if (clojureDir.exists() && clojureDir.isDirectory()) {
            Possible<Version> highestVersion = iterateOver(arrayList(clojureDir.list()))
                    .transformAndConcat(parseMavenVersionDirectory)
                    .reduce(findHighestVersion, Version.EMPTY);
            if (highestVersion.hasValue()) {
                return findClojureLib(highestVersion.get().toString());
            } else {
                return Possible.emptyValue(new IllegalStateException("Could not find a Clojure version under: " + clojureDir.getAbsolutePath()));
            }
        } else {
            return Possible.emptyValue(new IllegalStateException("The Clojure directory does not exist."));
        }
    }

    public static Possible<File> findClojureLib(String version) throws IOException {
        String clojureLocation = getHomeDirectory() + "/" + MAVEN_CLOJURE_LOCATION;
        String jarFileLocation = clojureLocation + "/" + version + "/" + "clojure-" + version + ".jar";
        final File file = new File(jarFileLocation);
        if (file.exists()) {
            return Possible.value((File) file);
        } else {
            return Possible.emptyValue();
        }
    }

    public static Possible<Version> findNewestLeiningen() throws IOException {
        final File leinDir = new File(getHomeDirectory() + File.separator + LEIN_LOCATION);
        if (leinDir.exists() && leinDir.isDirectory()) {
            Possible<Version> highestVersion = iterateOver(arrayList(leinDir.list()))
                    .transformAndConcat(parseLeinVersion)
                    .reduce(findHighestVersion, Version.EMPTY);
            if (highestVersion.hasValue()) {
                return highestVersion;
            } else {
                return Possible.emptyValue(new IllegalStateException("Could not find a Leiningen version under: " + leinDir.getAbsolutePath()));
            }
        } else {
            return Possible.emptyValue(new IllegalStateException("The Leiningen directory does not exist."));
        }
    }

    public static Possible<File> findLeiningen(String versionFile) throws IOException {
        String leinLocation = getHomeDirectory() + "/" + LEIN_LOCATION + "/" + versionFile;
        final File file = new File(leinLocation);
        if (file.exists()) {
            return Possible.value((File) file);
        } else {
            return Possible.emptyValue();
        }
    }

}
