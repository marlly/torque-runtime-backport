#!/bin/sh

SCARAB=../../../scarab
#SCARAB=.
CPWD=`pwd`

CP=../../bin/classes
CP=${CP}:.
CP=${CP}:${SCARAB}/lib/mm.mysql-2.0.4.jar
CP=${CP}:${SCARAB}/lib/velocity-1.1-dev.jar
CP=${CP}:${SCARAB}/lib/log4j-1.1.jar
CP=${CP}:${SCARAB}/lib/village-1.5.1.jar
CP=${CP}:${SCARAB}/lib/jdbc2_0-stdext.jar

cd ../..
ant compile-test
cd ${CPWD}

java -cp ${CP} org.apache.torque.pool.PoolTest
