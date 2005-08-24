# Copyright 2001-2005 The Apache Software Foundation.
#
# Licensed under the Apache License, Version 2.0 (the "License")
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# !/bin/sh

SCARAB=../../../scarab
# SCARAB=.
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
