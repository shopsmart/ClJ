#!/bin/bash

echo "Building Possible-monad dependency"
cd ..
git submodule update --init --recursive

# Build Possible-monad

cd externals/possible-monad/
mvn install
cd ../..

# Refresh embedded dependencies

echo "Refreshing embedded dependencies in lib"
cd ClJ.osgi/lib

rm -f ClJ* possible.jar

ls

cp ../../externals/possible-monad/target/*.jar .
cp ../../ClJ.api/target/*.jar .
cp ../../ClJ/target/*.jar .

rm -f *sources.jar
rm -f *javadoc.jar
rm -f *tests.jar

mv -f ClJ.api-*.jar ClJ.api.jar
mv -f ClJ-*.jar ClJ.jar
mv -f com.coconut*.jar possible.jar

echo ""
echo "Refreshed dependencies"

ls
