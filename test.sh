#!/bin/sh

ant clean
ant dist

(
    cd bin/torque/torque
    ant
    ant compile
)    
