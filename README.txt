--------------------------------------------------------------------------
T O R Q U E
--------------------------------------------------------------------------

Torque is an Object/Relational persistence layer.

Use Torque generator to generate your object model classes and associated SQL.

Use the Torque maven-plugin or Ant to invoke Torque generator.

The Torque runtime provides the necessary libraries to compile and use the
object model classes produced by Torque generator.  It also provides an Ant
build file for generating Torque.properties - the Torque runtime configuration
properties file.

database/         <--- Contains database specific property files used 
                       during the generating of runtime configuration
                       property files.
docs/             <--- Contains a copy of the Torque documentation, 
                       including the API JavaDocs.
lib/              <--- Contains the jar files required by the Torque 
                       runtime.
master/build.xml  <--- The Ant build file for regenerating Torque.properties.
master/default.prperties
                  <--- The properties that will be used when regenerating 
                       Torque.properties.
master/Torque.master
                  <--- The unprocessed property file template.
componentConfiguration.xml
roleConfiguration.xml
                  <--- These are included to assist with using Torque as 
                       component in a container (e.g. one of the Avalon
                       containers).
LICENSE.txt       <--- The License for the Torque runtime.
README.txt        <--- This file.
Torque.properties <--- A sample generated runtime configuration file - this
                       will be replaced when you regenerate the runtime 
                       configuration.

--------------------------------------------------------------------------
B U I L D I N G
--------------------------------------------------------------------------

Building Torque from CVS is fairly straight forward - follow the links to
the Developer Guide for further details:

	http://db.apache.org/torque/
