#!/bin/bash

cd ../m2-p2.repo
mvn p2:site

echo 'Type "mvn jetty:run" in the m2-p2 project to start a p2 server on localhost:8080'
