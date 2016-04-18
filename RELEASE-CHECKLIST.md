# Release checklist

* Double-check the that the possible-monad version in com.bradsdeals.ClJ matches the one in m2-p2

* Update version number in parent pom.xml
* Update version number in com.bradsdeals.clojuresupport.loader/pom.xml
* Update version number in com.bradsdeals.clojuresupport.loader/META-INF/MANIFEST.MF

* Run ./build.sh

* Commit all changes to GitHub
* Create a new release/version in GitHub matching new version number
