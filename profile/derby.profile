# location of jar that you will be using for testing
# so that it can be copied into the dist/lib directory.

torque.testDatabaseJar = ${maven.repo.local}/derby/jars/db2jcc-licensed-10.0.2.1.jar
#torque.idMethod = idbroker
torque.idMethod = native
torque.defaultDatabase = bookstore

torque.sqlTest.defaultDatabase = sqltest
torque.sqlTest.databaseUrl = jdbc:derby:net:@//localhost/test

lib.dir = lib

# -------------------------------------------------------------------
#
# T O R Q U E  C O N F I G U R A T I O N  F I L E
#
# -------------------------------------------------------------------

torque.project = bookstore

# -------------------------------------------------------------------
#
#  T A R G E T  D A T A B A S E
#
# -------------------------------------------------------------------

torque.database = derby

# -------------------------------------------------------------------
#
#  O B J E C T  M O D E L  I N F O R M A T I O N
#
# -------------------------------------------------------------------
# These settings will allow you to customize the way your
# Peer-based object model is created.
# -------------------------------------------------------------------
# addSaveMethod=true adds tracking code to determine how to save objects
#
# addGetByNameMethod=true adds methods to get fields by name/position
#
# complexObjectModel=true generates an om with collection support
#
# addTimeStamp=true puts time stamps in generated files
#
# addIntakeRetrievable=implement Intake's Retrievable interface
# -------------------------------------------------------------------

torque.targetPackage=org.apache.torque.test
torque.basePrefix=Base
torque.addSaveMethod=true
torque.addGetByNameMethod=true
torque.complexObjectModel=true
torque.addTimeStamp=true
torque.addIntakeRetrievable=false
torque.useManagers=false
torque.generateBeans=true
torque.silentDbFetch=true

# -------------------------------------------------------------------
#
#  D A T A B A S E  S E T T I N G S
#
# -------------------------------------------------------------------
# JDBC connection settings. This is used by the JDBCToXML task that
# will create an XML database schema from JDBC metadata. These
# settings are also used by the SQL Ant task to initialize your
# Torque system with the generated SQL.
# -------------------------------------------------------------------

torque.database.createUrl = jdbc:derby:net://localhost/test
torque.database.buildUrl = jdbc:derby:net://localhost/test
torque.database.url = jdbc:derby:net://localhost/test
torque.database.driver = com.ibm.db2.jcc.DB2Driver
torque.database.user = test
torque.database.password = test
torque.database.host = localhost
# torque.database.schema = TORQUE
torque.database.validationQuery = values(1)

# Tells JDBC task that javaName attribute for the tables and columns
# should be made same as SQL name.
torque.sameJavaName=false
