#!/bin/sh

ant clean
ant dist

(
    cd bin/torque/torque
    # Get rid of the templates directory, we want to
    # use the templates inside the JAR file.
    rm -rf templates
    ant main-classpath
    ant compile
)    
