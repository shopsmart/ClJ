# ClJ

Clojure-Java interop utilities

## Features

* Java to Clojre interop DSL and utilities with experimental OSGi support.
* Automatic wrapping of Clojure-returned collections to reasonable implementations of
  Java's Iterable<T> interface.
* Like Shimdandy, supports multiple Clojure runtimes within a single Java application.
* Highly experimental OSGi integration with Equinox and Eclipse.

## How?

### There are two APIs:

* One based on defining a Java interface to Clojure's functions.  This provides the simplest
and most Java-idiomatic interface to Clojure.

```java
    @Require({"leiningen.core.user :as u",
              "leiningen.core.project :as p",
              "leiningen.core.classpath :as cp"})
    private interface Leiningen {
        @Ns("u") void init();
        @Ns("u") void profiles();
        @Ns("p") Object read(String projectPath);
        @Ns("cp") IClojureIterable<String> get_classpath(Object project);
        @Ns("cp") IClojureIterable<String> ext_classpath(Object project);
    }

    private Leiningen lein = ClJ.define(Leiningen.class);

    public void initialize() {
        lein.init();
        lein.profiles();
        //...
    }
```

In addition to annotating methods for Clojure namespaces, individual parameters can be type-annotated,
even in cases where Clojure allows the same function to accept, say, a Vector or a Map in the initial
parameter.  These will be type-checked at runtime, but at least the types will be clearly specified and
obvious in the code.

* The second API allows for dynamically calling Clojure using a form similar to "do".  With this form, Clojure functions
can be passed easily to Clojure functions.  The DSL also provides a lexically-scoped implementation
of "let".  See the tests for documentation on what is supported.  Here is an example, from the integration tests:

```java
    byte[] input = "I see because I C".getBytes();
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
```

### Methods that return Clojure collections

Clojure's persistent collections APIs are convenient and easy to use from Clojure, but many of the
interfaces do not interoperate nicely with Java's collections API or make it easy to write idiomatic
Java code that interoperates with Clojure.  When ClJ invokes a method that returns any of Clojure's
core collection types, it automatically wraps that collection in an instance of *IClojureIterable*.
While these collections are still immutable, this enables use by Java's foreach construct as well as
random access to the collection's elements.  As-of this writing, this is the *IClojureIterable*
interface definition:

```java
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
```

This interface is produced by both the interface-based and the dynamic Clojure APIs.


## Dynamic usage: Multiple Clojure instances inside a single Java VM.

The Java interface-based API is also usable to create multiple private instances of Clojure, like would be needed in Java containers like OSGi, Spring, and some web containers.  Here's how to set that up.

First, define an interface to your Clojure functions using the first (Java interfaced-based) API above.  Next, ensure that the Clojure and ClJ JARs are NOT anywhere on the classpath, but that their jars are available to the container in the file system.  Only the ClJ.api jar should be on the classpath.  Then, write code similar to the following to initialize a private Clojure instance using ClJ:

```java
    URL clojureJar = new File("/path/to/clojure.jar").toURI().toURL();      // Customize this for your container
    URL cljToJavaJar = new File("/path/to/ClJ-version.jar").toURI().toURL();
    ClassLoader clojureClassloader = new URLClassLoader(new URL[] {clojureJar, cljToJavaJar}, parentClassloader);
    PrivateClJ clJ = new PrivateClJ(clojureClassloader);
```

Then to use your new Clojure instance, PrivateClJ's API is the same as ClJ's API.  Define an instance of your interface using the #define method, and call methods on your object like usual.

```java
    lein = clJ.define(YourCljInterface.class);
    lein.someClojureMethod("foo", "bar", "baz");
```

Note that data passed from one Clojure instance to another within the same container will fail Clojure *(instance?* checks.  E.g.: an instance of IPersistentList from one Clojure instance is not the same class as an instance of IPersistentList in a different Clojure instance in the same JVM.  Finding elegant ways to approach this challenge is a subject of ongoing API refinement in this library.

Lastly, since you created a private Clojure instance, if your container unloads your module, the module unloader also needs to close your Clojure instance and free all of the objects the Clojure environment allocated:

```java
    clJ.close()
```


## Usage

### With a single Clojure runtime

Add both ClJ and ClJ.api to your Maven build.  Done.  See below for Maven coordinates.


### With multiple Clojure runtimes in the same Java application

* Add ClJ.api to your Maven build.
* Create a URLClassLoader that is a child of your module's classloader.  This URLClassLoader
  should reference both the ClJ.jar and Clojure.jar.
* Use ClJLoader#clj(yourClassLoader) to obtain an IClJ instance to interact with the new Clojure runtime.
* If you ever unload your module, call #close() on your Clojure instance to free its resources.

See the code above for specific examples.


### With OSGi and Eclipse (work in progress)

The goal of the OSGi support is to enable:

* Individual OSGi bundles or groups of bundles to share a private Clojure instance.
* Developers to attach a Clojure REPL to any arbitrary active OSGi bundle for experimentation or debugging purposes.
* Eclipse tool support for the above.

We currently do not provide a P2 repository build out of the box.  However, it is easy to do from
the code.

After checking out the code from Github, simply type:

```bash
make
# ...lots of stuff scrolls by
make p2
```

At this point you will have a P2 repository server running on localhost:8080 with all the built artifacts.

Install possible-monad, ClJ.api, and ClJ.osgi.


## Maven coordinates for main ClJ library

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
	<name>Jitpack repo</name>
	<url>https://jitpack.io</url>
  </repository>
</repositories>
```

### ClJ.api - The API library

* GroupId: com.github.shopsmart.ClJ
* ArtifactId: ClJ.api
* Version: [![Release](http://jitpack.io/v/com.github.shopsmart.ClJ/ClJ.api.svg)](https://jitpack.io/#shopsmart.ClJ/ClJ.api)

### ClJ - The API implementation

* GroupId: com.github.shopsmart.ClJ
* ArtifactId: ClJ
* Version: [![Release](http://jitpack.io/v/com.github.shopsmart.ClJ/ClJ.svg)](https://jitpack.io/#shopsmart.ClJ/ClJ)


## Documentation

Complete Javadoc is provided.

# License

Licensed under the Eclipse Public License version 1.0.

# Authors

David Orme
