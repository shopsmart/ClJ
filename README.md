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
      <version>0.5.0-SNAPSHOT</version>
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

* One for dynamically calling Clojure using a form similar to "do"

```java

    String result =
        doAll(require("clojure.string :as str",
                      "clojure.java.io :as io"),
                $("io/copy", input, output),
                $("str/replace", INPUT, Pattern.compile("C"), "see"));
```

Complete Javadoc is provided.

# License

Licensed under the Eclipse Public License version 1.0.

# Authors

David Orme
