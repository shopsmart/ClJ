#!/bin/bash

git submodule update --init --recursive

# Build Possible-monad

cd externals/possible-monad/
mvn install
cd ../..

# Build ClJ

mvn install

# Build Eclipse plugin
cd com.bradsdeals.clojuresupport.loader/lib

cp ../../externals/possible-monad/target/*.jar .
cp ../../com.bradsdeals.ClJ.api/target/*.jar .
cp ../../com.bradsdeals.ClJ/target/*.jar .

rm -f *sources.jar
rm -f *javadoc.jar
rm -f *tests.jar

mv com.bradsdeals.ClJ.api*.jar ClJ.api.jar
mv com.bradsdeals.ClJ*.jar ClJ.jar
mv com.coconut*.jar possible.jar

cd ..
mvn install

# Build P2 site
cd ../com.bradsdeals.clojuresupport.loader.m2-p2
mvn p2:site

echo 'Type "mvn jetty:run" in the m2-p2 project to start a p2 server on localhost:8080'
