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
You must have ant version 1.4 or newer installed.

Building the decoupled version of Torque from CVS is now very
easy.  Recently, changes have been made to the Torque build
process to simplify the acquisition of jar dependencies.  The
entire build process is now a four-step process.

The first step of the process is to obtain the source.  Checkout
the jakarta-turbine-torque repository.  If you are unfamiliar
with the Jakarta CVS repositories, please refer to the CVS
Repositories document for assistance at: 
http://jakarta.apache.org/site/cvsindex.html.

Next, you must define the lib.repo property in your
${user.home}/build.properties file.  If you do not have a
${user.home}/build.properties file, create one in your home
directory and add the following line:

  lib.repo = /path/to/some/directory  

The value of this property determines the location that the
Torque dependencies will be stored after they have been
downloaded.  Note: this directory must exist in the filesystem.

Next, in the top-level directory of the Torque distribution, type
the following command to download all of the dependencies
required to build Torque:

  ant update-jars  

Lastly, after all of the jars have been downloaded to your
lib.repo directory, building the Torque distribution is only a
matter of typing the following command:

  ant dist  

The resulting jar file and zip distribution will be located in
the bin directory.  For those interested in building applications
with Torque, only the zip distribution is needed.
