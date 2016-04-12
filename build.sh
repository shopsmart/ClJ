#!/bin/bash

git submodule update --init --recursive

# Build ClJ

cd clojure-java
mvn install
cd ..

# Build Eclipse plugin
cd osgi-support/com.bradsdeals.clojuresupport.loader/lib

cp ../../../clojure-java/com.bradsdeals.ClJ.api/target/*.jar .
rm -f *sources.jar
rm -f *javadoc.jar
mv com.bradsdeals.ClJ.api*.jar ClJ.api.jar

cp ../../../clojure-java/com.bradsdeals.ClJ/target/*.jar .
rm -f *sources.jar
rm -f *javadoc.jar
mv com.bradsdeals.ClJ*.jar ClJ.jar
cd ..
#mvn clean install
cd ..

# Build P2 site
cd com.bradsdeals.clojuresupport.loader.releng
mvn p2:site

echo 'Type "mvn jetty:run" to start a p2 server on localhost:8080'

