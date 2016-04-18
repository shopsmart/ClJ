# Release checklist

* Double-check the that the possible-monad version in com.bradsdeals.ClJ matches the one in m2-p2

* Update version number in parent pom.xml
* Update version number in ClJ.osgi/META-INF/MANIFEST.MF

* mvn install

* Commit all changes to GitHub
* Create a new release/version in GitHub matching new version number
