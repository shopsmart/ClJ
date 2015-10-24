# ClJ

Java to Clojre interop DSL and utilities

## Usage

* Download and compile using

```
    mvn clean install
```

* Depend on in your Maven projects using:

```
    <dependency>
      <groupId>com.bradsdeals</groupId>
      <artifactId>ClJ</artifactId>
      <version>0.5.0-SNAPSHOT</version>
    </dependency>
```

## There are two APIs:

* One based on defining a Java interface to Clojure's functions

```
    @Require({"Lieningen.core.user :as u",
              "Lieningen.core.project :as p",
              "Lieningen.core.classpath :as cp"})
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

```
    String result = (String)
        doAll(require("clojure.string :as str",
                      "clojure.java.io :as io"),
                $("io/copy", input, output),
                $("str/replace", INPUT, Pattern.compile("C"), "see"));
```

Complete Javadoc is provided.
