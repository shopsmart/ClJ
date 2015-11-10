# ClJ

Java to Clojre interop DSL and utilities

## Usage

* Download and compile using

```
    mvn clean install
```

* Depend on in your Maven projects using:

```xml
    <dependency>
      <groupId>com.bradsdeals</groupId>
      <artifactId>ClJ</artifactId>
      <version>${ClJ.version}</version>
    </dependency>
    <dependency>
      <groupId>com.bradsdeals</groupId>
      <artifactId>ClJ</artifactId>
      <version>${ClJ.version}</version>
    </dependency>
```

## There are two APIs:

* One based on defining a Java interface to Clojure's functions

```java

    @Require({"Leiningen.core.user :as u",
              "Leiningen.core.project :as p",
              "Leiningen.core.classpath :as cp"})
    private interface Leiningen {
        @Ns("u") void init();
        @Ns("u") void profiles();
        @Ns("p") Object read(String projectPath);
        @Ns("cp") ISeq get_classpath(Object project);
        @Ns("cp") ISeq ext_classpath(Object project);
    }

    private Leiningen lein = ClJ.define(Leiningen.class);

    public void initialize() {
        lein.init();
        lein.profiles();
        //...
    }
```

* One for dynamically calling Clojure using a form similar to "do".  With this form, clojure functions
can be passed easily to Clojure functions.  The DSL also provides a lexically-scoped implementation
of "let".  See the tests for documentation on what is supported.

```java

    String result =
        doAll(require("clojure.string :as str",
                      "clojure.java.io :as io"),
                $("io/copy", input, output),
                $("str/replace", INPUT, Pattern.compile("C"), "see"));
```

* The first API is also usable in a private classloader like found in Java containers like OSGi, Spring,
and some web containers.  First, define an interface to your Clojure API like described above.  Next,
ensure that Clojure and ClJ are NOT anywhere on the classpath, but that their jars are available
to the container.  Then, write code similar to the following:

```java

    URL clojureJar = new File("/path/to/clojure.jar").toURI().toURL();
    URL cljToJavaJar = new File("/path/to/ClJ-version.jar").toURI().toURL();
    ClassLoader clojureClassloader = new URLClassLoader(new URL[] {clojureJar, cljToJavaJar}, parentClassloader);
    PrivateClJ clJ = new PrivateClJ(clojureClassloader);

    lein = clJ.define(Leiningen.class);
```

Complete Javadoc is provided.

# License

Licensed under the Eclipse Public License version 1.0.

# Authors

David Orme
