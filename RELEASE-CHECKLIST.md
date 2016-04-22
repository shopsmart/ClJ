# Release checklist

## Ensure dependencies are up to date

* git submodule update --init --recursive ; # Then check for a new possible-monad version.
* Double-check the that the possible-monad version is in ClJ and ClJ.p2

## Choose new and update version number

* Update version number in parent pom.xml
* Update version number in parent pom refernces
* Update version number in ClJ.osgi/pom.xml and ClJ.osgi/META-INF/MANIFEST.MF

## Build/test

* mvn install

## Release

* Commit all changes to GitHub
* Create a new release/version in GitHub matching new version number
