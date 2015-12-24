(defproject com.bradsdeals/clojure-deps "1.0.0-SNAPSHOT"
  :description "${description}"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
;  :jvm-opts ["-Xmx10g" "-Xms512m" "-XX:+UseParallelGC" "-Dlog4j.configurationFile=log4j2-subproject.xml"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-logging-config "1.9.12"]
                 [org.clojure/tools.logging "0.3.1"]])

