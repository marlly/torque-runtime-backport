--------------------------------------------------------------------------
T O R Q U E
--------------------------------------------------------------------------

Torque is an Object/Relational persistence layer.

bin/        Temporary directory for building the project.
build/      Location of Ant build.xml and build.properties files.
examples/   Example XML Torque schemas that are part of the distribution.
src/        Location of Java sources and Torque templates.
xdocs/      Torque documention in Anakia formatted tags.

--------------------------------------------------------------------------
B U I L D I N G
--------------------------------------------------------------------------
You must first rename the build.properties.sample to build.properties

You must set the following properties in either your  
${user.home}/build.properties file, or the build.properties file provided 
in the Torque build/ directory:

velocity.jar
xerces.jar
village.jar
log4j.jar

Torque uses Velocity to generate the OM sources, and Torque
XML schema parsing requires Xerces. We will soon move to using
the Digester to parse the XML schema, and at that point any
SAX parser will be sufficient.

Village is required to build the distribution. Village isn't
needed for building, but the generated classes are dependent
on Village.

Log4j is required for logging in Torque.
