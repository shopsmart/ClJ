#!/bin/bash

cd ..
git submodule update --init --recursive

# Build Possible-monad

cd externals/possible-monad/
mvn install
cd ../..

# Refresh embedded dependencies
cd ClJ.osgi/lib

cp ../../externals/possible-monad/target/*.jar .
cp ../../ClJ.api/target/*.jar .
cp ../../ClJ/target/*.jar .

rm -f *sources.jar
rm -f *javadoc.jar
rm -f *tests.jar

mv ClJ.api*.jar ClJ.api.jar
mv ClJ*.jar ClJ.jar
mv com.coconut*.jar possible.jar
