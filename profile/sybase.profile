# location of jar that you will be using for testing
# so that it can be copied into the dist/lib directory.

torque.testDatabaseJar = ${lib.repo}/jconnect-5.5.jar
torque.idMethod = idbroker
torque.defaultDatabase = bookstore

torque.sqlTest.defaultDatabase = sqltest
torque.sqlTest.databaseUrl = jdbc:sybase:Tds:localhost:5000/sqltest

lib.dir = ../lib

# -------------------------------------------------------------------
# 
# T O R Q U E  C O N F I G U R A T I O N  F I L E
# 
# -------------------------------------------------------------------

project = bookstore

# -------------------------------------------------------------------
# 
#  T A R G E T  D A T A B A S E
#
# -------------------------------------------------------------------

database = sybase

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

targetPackage=org.apache.torque.test
basePrefix=Base
addSaveMethod=true
addGetByNameMethod=true
complexObjectModel=true
addTimeStamp=true
addIntakeRetrievable=false
useManagers=true

# -------------------------------------------------------------------
# 
#  D A T A B A S E  S E T T I N G S
#
# -------------------------------------------------------------------
# JDBC connection settings. This is used by the JDBCToXML task that
# will create an XML database schema from JDBC metadata. These
# settings are also used by the SQL Ant task to initialize your
# Turbine system with the generated SQL.
# -------------------------------------------------------------------

createDatabaseUrl = jdbc:sybase:Tds:localhost:5000/master
buildDatabaseUrl = jdbc:sybase:Tds:localhost:5000/bookstore
databaseUrl = jdbc:sybase:Tds:localhost:5000/bookstore
databaseDriver = com.sybase.jdbc2.jdbc.SybDriver
databaseUser = sa
databasePassword = 
databaseHost = localhost

# Tells JDBC task that javaName attribute for the tables and columns
# should be made same as SQL name.
sameJavaName=false

# -------------------------------------------------------------------
#
#  D O C U M E N T A T I O   S E T T I N G S
#
# -------------------------------------------------------------------
# These settings will allow you to customize the way your schema
# documentation is created.
# Valid formats are: html, anakia (for use with jakarta-site2)
# -------------------------------------------------------------------
documentationFormat=html

# -------------------------------------------------------------------
# You should NOT have to edit anything below here.
# -------------------------------------------------------------------

# -------------------------------------------------------------------
# 
#  T E M P L A T E  P A T H
#
# -------------------------------------------------------------------

templatePath = ../templates

# -------------------------------------------------------------------
# 
#  C O N T R O L  T E M P L A T E S
#
# -------------------------------------------------------------------

SQLControlTemplate = sql/base/Control.vm
OMControlTemplate = om/Control.vm
idTableControlTemplate = sql/id-table/Control.vm
DataDTDControlTemplate = data/Control.vm
DataDumpControlTemplate = data/dump/Control.vm
DataSQLControlTemplate = sql/load/Control.vm
DocControlTemplate = doc/Control.vm

# -------------------------------------------------------------------
# 
#  O U T P U T  D I R E C T O R Y
#
# -------------------------------------------------------------------

outputDirectory=src

# -------------------------------------------------------------------
# 
#  S C H E M A  D I R E C T O R Y
#
# -------------------------------------------------------------------

schemaDirectory=schema
