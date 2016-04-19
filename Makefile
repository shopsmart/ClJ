# Makefile containing a synopsis of build / install commands.
#  - clean: Get rid of incremental build artifacts in case Taquari is messed up
#  - build: Run Maven and build everything; NOTE: subbuild errors don't cause
#           parent builds to fail!!!
#  - p2: Serve the built results at http://localhost/site ; Ctrl-c to exit

ALL: clean build

clean:
	find . | grep target$ | xargs rm -fr

build:
	mvn install

p2:
	cd CLJ.p2 && mvn jetty:run
