package org.apache.torque.util;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Turbine" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import com.workingdogs.village.Column;
import com.workingdogs.village.DataSet;
import com.workingdogs.village.KeyDef;
import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.Schema;
import com.workingdogs.village.TableDataSet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.torque.om.NumberKey;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.SimpleKey;
import org.apache.torque.om.StringKey;
import org.apache.torque.Torque;
import org.apache.torque.adapter.DB;
import org.apache.torque.oid.IdGenerator;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.MapBuilder;
import org.apache.torque.map.TableMap;
import org.apache.torque.pool.DBConnection;

//!! no good.
import org.apache.commons.collections.StringStack;
import org.apache.torque.TorqueException;
import org.apache.log4j.Category;

/**
 * This is the base class for all Peer classes in the system.  Peer
 * classes are responsible for isolating all of the database access
 * for a specific business object.  They execute all of the SQL
 * against the database.  Over time this class has grown to include
 * utility methods which ease execution of cross-database queries and
 * the implementation of concrete Peers.
 *
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @version $Id$
 */
public abstract class BasePeer implements java.io.Serializable
{
    /** Constant criteria key to reference ORDER BY columns. */
    public static final String ORDER_BY = "ORDER BY";

    /**
     * Constant criteria key to remove Case Information from
     * search/ordering criteria.
     */
    public static final String IGNORE_CASE = "IgNOrE cAsE";

    /** Classes that implement this class should override this value. */
    public static final String TABLE_NAME = "TABLE_NAME";

    /** The Turbine default MapBuilder. */
    public static final String DEFAULT_MAP_BUILDER =
        "org.apache.torque.util.db.map.TurbineMapBuilder";

    /** Hashtable that contains the cached mapBuilders. */
    private static Hashtable mapBuilders = new Hashtable(5);

    protected static Category category =
        Category.getInstance(BasePeer.class.getName());

    /**
     * Converts a hashtable to a byte array for storage/serialization.
     *
     * @param hash The Hashtable to convert.
     * @return A byte[] with the converted Hashtable.
     * @exception TorqueException
     */
    public static byte[] hashtableToByteArray( Hashtable hash )
        throws TorqueException
    {
        Hashtable saveData = new Hashtable(hash.size());
        String key = null;
        Object value = null;
        byte[] byteArray = null;

        Iterator keys = hash.keySet().iterator();
        while (keys.hasNext())
        {
            key = (String) keys.next();
            value = hash.get(key);
            if ( value instanceof Serializable )
            {
                saveData.put ( key, value );
            }
        }

        ByteArrayOutputStream baos = null;
        BufferedOutputStream bos = null;
        ObjectOutputStream out = null;
        try
        {
            // These objects are closed in the finally.
            baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos);
            out = new ObjectOutputStream(bos);

            out.writeObject(saveData);
            out.flush();
            bos.flush();
            byteArray = baos.toByteArray();
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ignored)
                {
                }
            }

            if (bos != null)
            {
                try
                {
                    bos.close();
                }
                catch (IOException ignored)
                {
                }
            }

            if (baos != null)
            {
                try
                {
                    baos.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
        return byteArray;
    }

    /**
     * Sets up a Schema for a table.  This schema is then normally
     * used as the argument for initTableColumns().
     *
     * @param tableName The name of the table.
     * @return A Schema.
     */
    public static Schema initTableSchema(String tableName)
    {
        return initTableSchema(tableName, null);
    }

    /**
     * Sets up a Schema for a table.  This schema is then normally
     * used as the argument for initTableColumns
     *
     * @param tableName The propery name for the database in the
     * Turbineresources file.
     * @param dbName The name of the database.
     * @return A Schema.
     */
    public static Schema initTableSchema(String tableName, String dbName)
    {
        Schema schema = null;
        DBConnection db = null;

        try
        {
            if (dbName == null)
            {
                // Get a connection to the db.
                db = Torque.getConnection();
            }
            else
            {
                // Get a connection to the db.
                db = Torque.getConnection( dbName );
            }

            Connection connection = db.getConnection();

            schema = new Schema().schema(connection, tableName);
        }
        catch (Exception e)
        {
            category.error(e);
            throw new Error("Error in BasePeer.initTableSchema(" +
                            tableName + "): " +
                            e.getMessage());
        }
        finally
        {
            try
            {
                Torque.releaseConnection(db);
            }
            catch (Exception e)
            {
            }
        }
        return schema;
    }

    /**
     * Creates a Column array for a table based on its Schema.
     *
     * @param schema A Schema object.
     * @return A Column[].
     */
    public static Column[] initTableColumns(Schema schema)
    {
        Column[] columns = null;
        try
        {
            int numberOfColumns = schema.numberOfColumns();
            columns = new Column[numberOfColumns];
            for (int i = 0; i < numberOfColumns; i++)
            {
                columns[i] = schema.column(i + 1);
            }
        }
        catch (Exception e)
        {
            category.error(e);
            throw new Error("Error in BasePeer.initTableColumns(): " +
                            e.getMessage());
        }
        return columns;
    }

    /**
     * Convenience method to create a String array of column names.
     *
     * @param columns A Column[].
     * @return A String[].
     */
    public static String[] initColumnNames(Column[] columns)
    {
        String[] columnNames = null;
        columnNames = new String[columns.length];
        for (int i = 0; i < columns.length; i++)
        {
            columnNames[i] = columns[i].name().toUpperCase();
        }
        return columnNames;
    }

    /**
     * Convenience method to create a String array of criteria keys.
     * Primary use is with TurbineUserPeer.
     *
     * @param tableName Name of table.
     * @param columnNames A String[].
     * @return A String[].
     */
    public static String[] initCriteriaKeys(String tableName,
                                            String[] columnNames)
    {
        String[] keys = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; i++)
        {
            keys[i] = tableName + "." + columnNames[i].toUpperCase();
        }
        return keys;
    }

    /**
     * Begin a transaction.  This method will fallback gracefully to
     * return a normal connection, if the database being accessed does
     * not support transactions.
     *
     * @param dbName Name of database.
     * @return The DBConnection for the transaction.
     * @exception TorqueException
     */
    public static DBConnection beginTransaction(String dbName)
        throws TorqueException
    {
        DBConnection dbCon = Torque.getConnection( dbName );
        try
        {
            if ( dbCon.getConnection().getMetaData().supportsTransactions() )
            {
                dbCon.setAutoCommit(false);
            }
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }

        return dbCon;
    }

    /**
     * Commit a transaction.  This method takes care of releasing the
     * connection after the commit.  in databases that do not support
     * transactions, it only returns the connection.
     *
     * @param dbCon The DBConnection for the transaction.
     * @exception TorqueException
     */
    public static void commitTransaction(DBConnection dbCon)
        throws TorqueException
    {
        try
        {
            if ( dbCon.getConnection().getMetaData().supportsTransactions() )
            {
                dbCon.commit();
                dbCon.setAutoCommit(true);
            }
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            // Release the connection to the pool.
            Torque.releaseConnection( dbCon );
        }
    }

    /**
     * Roll back a transaction in databases that support transactions.
     * It also releases the connection.  in databases that do not support
     * transactions, this method will log the attempt and release the
     * connection.
     *
     * @param dbCon The DBConnection for the transaction.
     * @exception TorqueException
     */
    public static void rollBackTransaction(DBConnection dbCon)
        throws TorqueException
    {
        try
        {
            if ( dbCon.getConnection().getMetaData().supportsTransactions() )
            {
                dbCon.rollback();
                dbCon.setAutoCommit(true);
            }
            else
            {
                category.error(
                        "An attempt was made to rollback a transaction but the"
                        + " database did not allow the operation to be rolled back.");
            }
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            // Release the connection to the pool.
            Torque.releaseConnection( dbCon );
        }
    }


    /**
     * Convenience method that uses straight JDBC to delete multiple
     * rows.  Village throws an Exception when multiple rows are
     * deleted.
     *
     * @param dbCon A DBConnection.
     * @param table The table to delete records from.
     * @param column The column in the where clause.
     * @param value The value of the column.
     * @exception TorqueException
     */
    public static void deleteAll( DBConnection dbCon,
                                  String table,
                                  String column,
                                  int value )
        throws TorqueException
    {
        Statement statement = null;
        try
        {
            Connection conn = dbCon.getConnection();
            statement = conn.createStatement();

            StringBuffer query = new StringBuffer();
            query.append( "DELETE FROM " )
                .append( table )
                .append( " WHERE " )
                .append( column )
                .append( " = " )
                .append( value );

            statement.executeUpdate( query.toString() );
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException ignored)
                {
                }
            }
        }
    }

    /**
     * Convenience method that uses straight JDBC to delete multiple
     * rows.  Village throws an Exception when multiple rows are
     * deleted.  This method attempts to get the default database from
     * the pool.
     *
     * @param table The table to delete records from.
     * @param column The column in the where clause.
     * @param value The value of the column.
     * @exception TorqueException
     */
    public static void deleteAll(String table, String column, int value)
            throws TorqueException
    {
        DBConnection dbCon = null;
        try
        {
            // Get a connection to the db.
            dbCon = Torque.getConnection();

            deleteAll( dbCon, table, column, value );
        }
        finally
        {
            Torque.releaseConnection(dbCon);
        }
    }

    /**
     * Method to perform deletes based on values and keys in a
     * Criteria.
     *
     * @param criteria The criteria to use.
     * @exception TorqueException
     */
    public static void doDelete(Criteria criteria)
            throws TorqueException
    {
        DBConnection dbCon = null;
        String dbName = criteria.getDbName();

        // Do the work, assuring the database connection is released.
        if (criteria.isUseTransaction())
        {
            // For PostgreSQL and LOBs.
            try
            {
                dbCon = beginTransaction(criteria.getDbName());
                doDelete(criteria, dbCon);
                commitTransaction(dbCon);
            }
            catch (Exception e)
            {
                rollBackTransaction(dbCon);
                throw new TorqueException(e);
            }
        }
        else
        {
            try
            {
                dbCon = Torque.getConnection(dbName);
                doDelete(criteria, dbCon);
            }
            finally
            {
                Torque.releaseConnection(dbCon);
            }
        }
    }

    /**
     * Method to perform deletes based on values and keys in a
     * Criteria.
     *
     * @param criteria The criteria to use.
     * @param dbCon A DBConnection.
     * @exception TorqueException
     */
    public static void doDelete(Criteria criteria, DBConnection dbCon)
            throws TorqueException
    {
        DB db = Torque.getDB( criteria.getDbName() );
        DatabaseMap dbMap = Torque.getDatabaseMap( criteria.getDbName() );
        Connection connection = null;
        try
        {
            connection = dbCon.getConnection();
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }

        // Set up a list of required tables and add extra entries to
        // criteria if directed to delete all related records.
        // StringStack.add() only adds element if it is unique.
        StringStack tables = new StringStack();
        Iterator it = criteria.keySet().iterator();
        while (it.hasNext())
        {
            String key = (String) it.next();
            Criteria.Criterion c = criteria.getCriterion(key);
            String[] tableNames = c.getAllTables();
            for (int i = 0; i < tableNames.length; i++)
            {
                String tableName2 = criteria.getTableForAlias(tableNames[i]);
                if ( tableName2 != null )
                {
                    tables.add(
                        new StringBuffer(tableNames[i].length() +
                                         tableName2.length() + 1)
                        .append(tableName2).append(' ').append(tableNames[i])
                        .toString() );
                }
                else
                {
                    tables.add(tableNames[i]);
                }
            }

            if ( criteria.isCascade() )
            {
                // This steps thru all the columns in the database.
                TableMap[] tableMaps = dbMap.getTables();
                for (int i = 0; i < tableMaps.length; i++)
                {
                    ColumnMap[] columnMaps = tableMaps[i].getColumns();
                    for (int j = 0; j < columnMaps.length; j++)
                    {
                        // Only delete rows where the foreign key is
                        // also a primary key.  Other rows need
                        // updateing, but that is not implemented.
                        if ( columnMaps[j].isForeignKey()
                            && columnMaps[j].isPrimaryKey()
                            && key.equals(columnMaps[j].getRelatedName()) )
                        {
                            tables.add(tableMaps[i].getName());
                            criteria.add(columnMaps[j].getFullyQualifiedName(),
                                         criteria.getValue(key));
                        }
                    }
                }
            }
        }

        for (int i = 0; i < tables.size(); i++)
        {
            KeyDef kd = new KeyDef();
            StringStack whereClause = new StringStack();

            ColumnMap[] columnMaps =
                dbMap.getTable( tables.get(i) ).getColumns();
            for (int j = 0; j < columnMaps.length; j++)
            {
                ColumnMap colMap = columnMaps[j];
                if ( colMap.isPrimaryKey() )
                {
                    kd.addAttrib( colMap.getColumnName() );
                }
                String key = new StringBuffer(colMap.getTableName())
                    .append('.').append(colMap.getColumnName()).toString();
                if ( criteria.containsKey(key) )
                {
                    if ( criteria.getComparison(key).equals(Criteria.CUSTOM) )
                    {
                        whereClause.add( criteria.getString(key) );
                    }
                    else
                    {
                        whereClause.add( SqlExpression.build( colMap.getColumnName(),
                            criteria.getValue(key),
                            criteria.getComparison(key),
                            criteria.isIgnoreCase(),
                            db));
                    }
                }
            }

            // Execute the statement.
            TableDataSet tds = null;
            try
            {
                tds = new TableDataSet(connection, tables.get(i), kd);
                String sqlSnippet = whereClause.toString(" AND ");

                category.debug("BasePeer.doDelete: whereClause=" + sqlSnippet);

                tds.where(sqlSnippet);
                tds.fetchRecords();
                if ( tds.size() > 1 && criteria.isSingleRecord() )
                {
                    handleMultipleRecords(tds);
                }
                for (int j = 0; j < tds.size(); j++)
                {
                    Record rec = tds.getRecord(j);
                    rec.markToBeDeleted();
                    rec.save();
                }
            }
            catch (Exception e)
            {
                throw new TorqueException(e);
            }
            finally
            {
                if (tds != null)
                {
                    try
                    {
                        tds.close();
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }
        }
    }

    /**
     * Method to perform inserts based on values and keys in a
     * Criteria.
     * <p>
     * If the primary key is auto incremented the data in Criteria
     * will be inserted and the auto increment value will be returned.
     * <p>
     * If the primary key is included in Criteria then that value will
     * be used to insert the row.
     * <p>
     * If no primary key is included in Criteria then we will try to
     * figure out the primary key from the database map and insert the
     * row with the next available id using util.db.IDBroker.
     * <p>
     * If no primary key is defined for the table the values will be
     * inserted as specified in Criteria and -1 will be returned.
     *
     * @param criteria Object containing values to insert.
     * @return An Object which is the id of the row that was inserted
     * (if the table has a primary key) or null (if the table does not
     * have a primary key).
     * @exception TorqueException
     */
    public static ObjectKey doInsert(Criteria criteria)
            throws TorqueException
    {
        DBConnection dbCon = null;
        ObjectKey id = null;

        boolean doTransaction = criteria.isUseTransaction();

        try
        {
            // Get a connection to the db.
            if (doTransaction)
            {
                dbCon = beginTransaction(criteria.getDbName());
            }
            else
            {
                dbCon = Torque.getConnection( criteria.getDbName() );
            }
            id = doInsert(criteria, dbCon);
        }
        finally
        {
            if (doTransaction)
            {
                commitTransaction(dbCon);
            }
            else
            {
                Torque.releaseConnection(dbCon);
            }
        }
        return id;
    }


    /**
     * Method to perform inserts based on values and keys in a
     * Criteria.
     * <p>
     * If the primary key is auto incremented the data in Criteria
     * will be inserted and the auto increment value will be returned.
     * <p>
     * If the primary key is included in Criteria then that value will
     * be used to insert the row.
     * <p>
     * If no primary key is included in Criteria then we will try to
     * figure out the primary key from the database map and insert the
     * row with the next available id using util.db.IDBroker.
     * <p>
     * If no primary key is defined for the table the values will be
     * inserted as specified in Criteria and null will be returned.
     *
     * @param criteria Object containing values to insert.
     * @param dbCon A DBConnection.
     * @return An Object which is the id of the row that was inserted
     * (if the table has a primary key) or null (if the table does not
     * have a primary key).
     * @exception TorqueException
     */
    public static ObjectKey doInsert(Criteria criteria, DBConnection dbCon)
            throws TorqueException
    {
        SimpleKey id = null;

        // Get the table name and method for determining the primary
        // key value.
        String tableName = null;
        Iterator keys = criteria.keySet().iterator();
        if (keys.hasNext())
        {
            tableName = criteria.getTableName((String)keys.next());
        }
        else
        {
            throw new TorqueException("Database insert attempted without " +
                "anything specified to insert");
        }

        DatabaseMap dbMap = Torque.getDatabaseMap( criteria.getDbName() );
        TableMap tableMap = dbMap.getTable(tableName);
        Object keyInfo = tableMap.getPrimaryKeyMethodInfo();
        IdGenerator keyGen = tableMap.getIdGenerator();

        ColumnMap pk = getPrimaryKey(criteria);
        // only get a new key value if you need to
        // the reason is that a primary key might be defined
        // but you are still going to set its value. for example:
        // a join table where both keys are primary and you are
        // setting both columns with your own values
        boolean info = false;

        // pk will be null if there is no primary key defined for the table
        // we're inserting into.
        if (pk != null && !criteria.containsKey(pk.getFullyQualifiedName()))
        {
            if (keyGen == null)
            {
                throw new TorqueException("IdGenerator for table '" +
                                          tableName + "' is null");
            }
            // If the keyMethod is SEQUENCE or IDBROKERTABLE, get the id
            // before the insert.

            if (keyGen.isPriorToInsert())
            {
                try
                {
                    if ( pk.getType() instanceof Number )
                    {
                        id = new NumberKey(keyGen.getIdAsBigDecimal
                                           (dbCon.getConnection(), keyInfo));
                    }
                    else
                    {
                        id = new StringKey(keyGen.getIdAsString
                                           (dbCon.getConnection(), keyInfo));
                    }
                }
                catch (Exception e)
                {
                    throw new TorqueException(e);
                }
                criteria.add( pk.getFullyQualifiedName(), id );
            }
        }

        // Use Village to perform the insert.
        TableDataSet tds = null;
        try
        {
            tds = new TableDataSet(dbCon.getConnection(), tableName);
            Record rec = tds.addRecord();
            BasePeer.insertOrUpdateRecord(rec, tableName, criteria);
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            if (tds != null)
            {
                try
                {
                    tds.close();
                }
                catch (Exception e)
                {
                    throw new TorqueException(e);
                }
            }
        }

        // If the primary key column is auto-incremented, get the id
        // now.
        if (keyGen != null && keyGen.isPostInsert())
        {
            try
            {
                if ( pk.getType() instanceof Number )
                {
                id = new NumberKey(keyGen.getIdAsBigDecimal
                                   (dbCon.getConnection(), keyInfo));
                }
                else
                {
                    id = new StringKey(keyGen.getIdAsString
                                       (dbCon.getConnection(), keyInfo));
                }
            }
            catch (Exception e)
            {
                throw new TorqueException(e);
            }
        }

        return id;
    }

    /**
     * Grouping of code used in both doInsert() and doUpdate()
     * methods.  Sets up a Record for saving.
     *
     * @param rec A Record.
     * @param tableName Name of table.
     * @param criteria A Criteria.
     * @exception TorqueException
     */
    private static void insertOrUpdateRecord(Record rec,
                                             String tableName,
                                             Criteria criteria)
        throws TorqueException
    {
        DatabaseMap dbMap = Torque.getDatabaseMap( criteria.getDbName() );

        ColumnMap[] columnMaps = dbMap.getTable( tableName ).getColumns();
        boolean shouldSave = false;
        for (int j = 0; j < columnMaps.length; j++)
        {
            ColumnMap colMap = columnMaps[j];
            String key = new StringBuffer(colMap.getTableName())
                .append('.').append(colMap.getColumnName()).toString();
            if ( criteria.containsKey(key) )
            {
                // A village Record.setValue( String, Object ) would
                // be nice here.
                Object obj = criteria.getValue(key);
                if ( obj instanceof SimpleKey )
                {
                    obj = ((SimpleKey) obj).getValue();
                }
                try
                {
                    if (obj == null)
                    {
                        rec.setValueNull(colMap.getColumnName());
                    }
                    else if ( obj instanceof String )
                    {
                        rec.setValue( colMap.getColumnName(),
                                      (String) obj);
                    }
                    else if ( obj instanceof Integer)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      criteria.getInt(key));
                    }
                    else if ( obj instanceof BigDecimal)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      (BigDecimal) obj );
                    }
                    else if ( obj instanceof Boolean)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      criteria.getBoolean(key) ? 1 : 0);
                    }
                    else if ( obj instanceof java.util.Date)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      (java.util.Date) obj );
                    }
                    else if ( obj instanceof Float)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      criteria.getFloat(key) );
                    }
                    else if ( obj instanceof Double)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      criteria.getDouble(key) );
                    }
                    else if ( obj instanceof Byte)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      ((Byte) obj).byteValue() );
                    }
                    else if ( obj instanceof Long)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      criteria.getLong(key) );
                    }
                    else if ( obj instanceof Short)
                    {
                        rec.setValue( colMap.getColumnName(),
                                      ((Short) obj).shortValue() );
                    }
                    else if ( obj instanceof Hashtable )
                    {
                        rec.setValue( colMap.getColumnName(),
                                      hashtableToByteArray( (Hashtable)obj ) );
                    }
                    else if ( obj instanceof byte[])
                    {
                        rec.setValue( colMap.getColumnName(),
                                      (byte[]) obj);
                    }
                }
                catch (Exception e)
                {
                    throw new TorqueException(e);
                }
                shouldSave = true;
             }
        }

        if ( shouldSave )
        {
            try
            {
                rec.save();
            }
            catch (Exception e)
            {
                throw new TorqueException(e);
            }
        }
        else
        {
           throw new TorqueException("No changes to save");
        }
    }

    /**
     * Method to create an SQL query based on values in a Criteria.
     *
     * @param criteria A Criteria.
     * @exception TorqueException Trouble creating the query string.
     */
    public static String createQueryString( Criteria criteria )
            throws TorqueException
    {
        Query query = new Query();
        DB db = Torque.getDB( criteria.getDbName() );
        DatabaseMap dbMap = Torque.getDatabaseMap( criteria.getDbName() );

        StringStack selectModifiers = query.getSelectModifiers();
        StringStack selectClause = query.getSelectClause();
        StringStack fromClause = query.getFromClause();
        StringStack whereClause = query.getWhereClause();
        StringStack orderByClause = query.getOrderByClause();
        StringStack groupByClause = query.getGroupByClause();

        StringStack orderBy = criteria.getOrderByColumns();
        StringStack groupBy = criteria.getGroupByColumns();
        boolean ignoreCase = criteria.isIgnoreCase();
        StringStack select = criteria.getSelectColumns();
        Hashtable aliases = criteria.getAsColumns();
        StringStack modifiers = criteria.getSelectModifiers();

        for (int i = 0; i < modifiers.size(); i++)
        {
            selectModifiers.add( modifiers.get(i) );
        }

        for (int i = 0; i < select.size(); i++)
        {
            String columnName = select.get(i);
            if (columnName.indexOf('.') == -1)
            {
                throwMalformedColumnNameException("select", columnName);
            }
            String tableName = null;
            selectClause.add(columnName);
            int parenPos = columnName.indexOf('(');
            if (parenPos == -1)
            {
                tableName = columnName.substring(0, columnName.indexOf('.'));
            }
            else
            {
                tableName = columnName.substring(parenPos + 1,
                                                 columnName.indexOf('.') );
                // functions may contain qualifiers so only take the last
                // word as the table name.
                int lastSpace = tableName.lastIndexOf(' ');
                if ( lastSpace != -1 )
                {
                    tableName = tableName.substring(lastSpace + 1);
                }
            }
            String tableName2 = criteria.getTableForAlias(tableName);
            if ( tableName2 != null )
            {
                fromClause.add(
                    new StringBuffer(tableName.length() +
                                     tableName2.length() + 1)
                    .append(tableName2).append(' ').append(tableName)
                    .toString() );
            }
            else
            {
                fromClause.add(tableName);
            }
        }

        Iterator it = aliases.keySet().iterator();
        while (it.hasNext())
        {
          String key = (String) it.next();
          selectClause.add((String) aliases.get(key) + " AS " + key);
        }

        Iterator critKeys = criteria.keySet().iterator();
        while (critKeys.hasNext())
        {
            String key = (String) critKeys.next();
            Criteria.Criterion criterion =
                    (Criteria.Criterion) criteria.getCriterion(key);
            Criteria.Criterion[] someCriteria =
                    criterion.getAttachedCriterion();
            String table = null;
            for (int i = 0; i < someCriteria.length; i++)
            {
                String tableName = someCriteria[i].getTable();
                table = criteria.getTableForAlias(tableName);
                if ( table != null )
                {
                    fromClause.add(
                        new StringBuffer(tableName.length() +
                                         table.length() + 1)
                        .append(table).append(' ').append(tableName)
                        .toString() );
                }
                else
                {
                    fromClause.add(tableName);
                    table = tableName;
                }

                boolean ignorCase = ( (criteria.isIgnoreCase() ||
                                       someCriteria[i].isIgnoreCase()) &&
                    (dbMap.getTable(table).getColumn(
                    someCriteria[i].getColumn()).getType() instanceof String));

                someCriteria[i].setIgnoreCase(ignorCase);
            }

            criterion.setDB(db);
            whereClause.add( criterion.toString() );

        }

        List join = criteria.getJoinL();
        if ( join != null)
        {
            for ( int i = 0; i < join.size(); i++ )
            {
                String join1 = (String) join.get(i);
                String join2 = (String) criteria.getJoinR().get(i);
                if (join1.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("join", join1);
                }
                if (join2.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("join", join2);
                }

                String tableName = join1.substring(0, join1.indexOf('.'));
                String table = criteria.getTableForAlias(tableName);
                if ( table != null )
                {
                    fromClause.add(
                        new StringBuffer(tableName.length() +
                                         table.length() + 1)
                        .append(table).append(' ').append(tableName)
                        .toString() );
                }
                else
                {
                    fromClause.add(tableName);
                }

                int dot =  join2.indexOf('.');
                tableName = join2.substring(0, dot);
                table = criteria.getTableForAlias(tableName);
                if ( table != null )
                {
                    fromClause.add(
                        new StringBuffer(tableName.length() +
                                         table.length() + 1)
                        .append(table).append(' ').append(tableName)
                        .toString() );
                }
                else
                {
                    fromClause.add(tableName);
                    table = tableName;
                }

                boolean ignorCase = (criteria.isIgnoreCase() &&
                    (dbMap.getTable(table).getColumn(
                        join2.substring(dot + 1, join2.length()) )
                    .getType() instanceof String));

                whereClause.add(
                    SqlExpression.buildInnerJoin(join1, join2,
                                                 ignorCase, db) );
            }
        }
        
        // need to allow for multiple group bys
        if ( groupBy != null && groupBy.size() > 0)
        {
            for (int i=0; i<groupBy.size(); i++)
            {
                String groupByColumn = groupBy.get(i);
                if (groupByColumn.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("group by",groupByColumn);
                }          
                
                groupByClause.add(groupByColumn);
            }
        }
        
        Criteria.Criterion having = criteria.getHaving();
        if (having != null)
        {
            //String groupByString = null; 
            query.setHaving(having.toString());
        }
        
        if (orderBy != null && orderBy.size() > 0)
        {
            // Check for each String/Character column and apply
            // toUpperCase().
            for (int i = 0; i < orderBy.size(); i++)
            {
                String orderByColumn = orderBy.get(i);
                if (orderByColumn.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("order by",
                                                      orderByColumn);
                }
                String table = orderByColumn.substring(0,orderByColumn.indexOf('.') );
                // See if there's a space (between the column list and sort
                // order in ORDER BY table.column DESC).
                int spacePos = orderByColumn.indexOf(' ');
                String columnName;
                if (spacePos == -1)
                {
                    columnName = orderByColumn
                        .substring(orderByColumn.indexOf('.') + 1);
                }
                else
                {
                    columnName = orderByColumn
                        .substring(orderByColumn.indexOf('.') + 1, spacePos);
                }
                ColumnMap column = dbMap.getTable(table).getColumn(columnName);
                if ( column.getType() instanceof String )
                {
                    if (spacePos == -1)
                    {
                        orderByClause.add(db.ignoreCaseInOrderBy(orderByColumn));
                    }
                    else
                    {
                        orderByClause.add(db.ignoreCaseInOrderBy(
                                orderByColumn.substring(0, spacePos))
                                + orderByColumn.substring(spacePos) );
                    }
                    selectClause.add( db.ignoreCaseInOrderBy(
                            table + '.' + columnName) );
                }
                else
                {
                    orderByClause.add(orderByColumn);
                }
            }
        }

        // Limit the number of rows returned.
        int limit = criteria.getLimit();
        int offset = criteria.getOffset();
        String limitString = null;
        if ( offset > 0 && db.supportsNativeOffset() )
        {
            switch(db.getLimitStyle())
            {
                case DB.LIMIT_STYLE_MYSQL:
                    limitString = new StringBuffer().append(offset)
                                                    .append(", ")
                                                    .append(limit)
                                                    .toString();
                    break;
                case DB.LIMIT_STYLE_POSTGRES:
                    limitString = new StringBuffer().append(limit)
                                                    .append(", ")
                                                    .append(offset)
                                                    .toString();
                    break;
            }

            // Now set the criteria's limit and offset to return the
            // full resultset since the results are limited on the
            // server.
            criteria.setLimit(-1);
            criteria.setOffset(0);
        }
        else if (limit > 0 && db.supportsNativeLimit() )
        {
            limitString = String.valueOf(limit);

            // Now set the criteria's limit to return the full
            // resultset since the results are limited on the server.
            criteria.setLimit(-1);
        }

        if (limitString != null) query.setLimit(limitString);

        String sql = query.toString();
        category.debug(sql);
        return sql;
    }


    /**
     * Returns all results.
     *
     * @param criteria A Criteria.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List doSelect(Criteria criteria)
            throws TorqueException
    {
        List results = null;
        if (criteria.isUseTransaction())
        {
            DBConnection dbCon = beginTransaction(criteria.getDbName());
            try
            {
                results = executeQuery( createQueryString(criteria),
                                        criteria.getOffset(),
                                        criteria.getLimit(),
                                        criteria.isSingleRecord(), dbCon );
                commitTransaction(dbCon);
            }
            catch (Exception e)
            {
                // make sure to return connection
                rollBackTransaction(dbCon);
                throw new TorqueException(e);
            }
        }
        else
        {
            results = executeQuery( createQueryString(criteria),
                                    criteria.getOffset(),
                                    criteria.getLimit(),
                                    criteria.getDbName(),
                                    criteria.isSingleRecord() );
        }
        return results;
    }

    /**
     * Returns all results.
     *
     * @param criteria A Criteria.
     * @param dbCon A DBConnection.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List doSelect(Criteria criteria, DBConnection dbCon)
            throws TorqueException
    {
        return executeQuery( createQueryString(criteria),
                             criteria.isSingleRecord(), dbCon );
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for select statements only.  Use
     * executeStatement for update, insert, and delete operations.
     *
     * @param queryString A String with the sql statement to execute.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List executeQuery(String queryString)
            throws TorqueException
    {
        return executeQuery(queryString, Torque.getDefaultDB(), false);
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for select statements only.  Use
     * executeStatement for update, insert, and delete operations.
     *
     * @param queryString A String with the sql statement to execute.
     * @param dbName The database to connect to.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List executeQuery(String queryString,
                                      String dbName)
            throws TorqueException
    {
        return executeQuery(queryString, dbName, false);
    }

    /**
     * Method for performing a SELECT.  Returns all results.
     *
     * @param queryString A String with the sql statement to execute.
     * @param dbName The database to connect to.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List executeQuery(String queryString,
                                      String dbName,
                                      boolean singleRecord)
            throws TorqueException
    {
        return executeQuery(queryString, 0, -1, dbName, singleRecord);
    }

    /**
     * Method for performing a SELECT.  Returns all results.
     *
     * @param queryString A String with the sql statement to execute.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @param dbCon A DBConnection.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List executeQuery(String queryString,
                                      boolean singleRecord,
                                      DBConnection dbCon)
            throws TorqueException
    {
        return executeQuery(queryString, 0, -1, singleRecord, dbCon);
    }


    /**
     * Method for performing a SELECT.
     *
     * @param queryString A String with the sql statement to execute.
     * @param start The first row to return.
     * @param numberOfResults The number of rows to return.
     * @param dbName The database to connect to.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List executeQuery(String queryString,
                                      int start,
                                      int numberOfResults,
                                      String dbName,
                                      boolean singleRecord)
            throws TorqueException
    {
        DBConnection db = null;
        List results = null;
        try
        {
            // get a connection to the db
            db = Torque.getConnection( dbName );
            // execute the query
            results = executeQuery(queryString, start, numberOfResults,
                                   singleRecord, db);
        }
        finally
        {
            Torque.releaseConnection(db);
        }
        return results;
    }

    /**
     * Method for performing a SELECT.  Returns all results.
     *
     * @param queryString A String with the sql statement to execute.
     * @param start The first row to return.
     * @param numberOfResults The number of rows to return.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @param dbCon A DBConnection.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List executeQuery(String queryString,
                                      int start,
                                      int numberOfResults,
                                      boolean singleRecord,
                                      DBConnection dbCon)
            throws TorqueException
    {
        Connection connection = null;
        try
        {
            connection = dbCon.getConnection();
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }

        QueryDataSet qds = null;
        List results = new ArrayList();
        try
        {
            // execute the query
            long startTime = System.currentTimeMillis();
            qds = new QueryDataSet( connection, queryString );
            category.debug("Elapsed time="
                    + (System.currentTimeMillis() - startTime) + " ms");
            results = getSelectResults(qds, start, numberOfResults,
                    singleRecord);
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            if (qds != null)
            {
                try
                {
                    qds.close();
                }
                catch (Exception ignored)
                {
                }
            }
        }
        return results;
    }

    /**
     * Returns all records in a QueryDataSet as a List of Record
     * objects.  Used for functionality like util.db.LargeSelect.
     *
     * @param qds A QueryDataSet.
     * @return List of Record objects.
     * @exception TorqueException
     */
    public static List getSelectResults( QueryDataSet qds )
            throws TorqueException
    {
        return getSelectResults( qds, 0, -1, false);
    }

    /**
     * Returns all records in a QueryDataSet as a List of Record
     * objects.  Used for functionality like util.db.LargeSelect.
     *
     * @param qds A QueryDataSet.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @exception TorqueException
     */
    public static List getSelectResults(QueryDataSet qds, boolean singleRecord)
            throws TorqueException
    {
        return getSelectResults(qds, 0, -1, singleRecord);
    }

    /**
     * Returns numberOfResults records in a QueryDataSet as a List
     * of Record objects.  Starting at record 0.  Used for
     * functionality like util.db.LargeSelect.
     *
     * @param qds A QueryDataSet.
     * @param numberOfResults The number of results to return.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @exception TorqueException
     */
    public static List getSelectResults( QueryDataSet qds,
                                           int numberOfResults,
                                           boolean singleRecord )
            throws TorqueException
    {
        List results = null;
        if (numberOfResults != 0)
        {
            results = getSelectResults(qds, 0, numberOfResults, singleRecord);
        }
        return results;
    }

    /**
     * Returns numberOfResults records in a QueryDataSet as a List
     * of Record objects.  Starting at record start.  Used for
     * functionality like util.db.LargeSelect.
     *
     * @param qds A QueryDataSet.
     * @param start where to start retrieving Records.
     * @param numberOfResults The number of results to return.
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @exception TorqueException
     */
    public static List getSelectResults( QueryDataSet qds,
                                           int start,
                                           int numberOfResults,
                                           boolean singleRecord )
            throws TorqueException
    {
        List results;
        try
        {
            if ( numberOfResults <= 0 )
            {
                results = new ArrayList();
                qds.fetchRecords();
            }
            else
            {
                results = new ArrayList(numberOfResults);
                qds.fetchRecords(start, numberOfResults);
            }
            if ( qds.size() > 1 && singleRecord )
            {
                handleMultipleRecords(qds);
            }

            // Return a List of Record objects.
            for ( int i = 0; i < qds.size(); i++ )
            {
                Record rec = qds.getRecord(i);
                results.add(rec);
            }
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
        return results;
    }

    /**
     * Helper method which returns the primary key contained
     * in the given Criteria object.
     *
     * @param criteria A Criteria.
     * @return ColumnMap if the Criteria object contains a primary
     *          key, or null if it doesn't.
     * @exception TorqueException
     */
    private static ColumnMap getPrimaryKey(Criteria criteria)
            throws TorqueException
    {
        // Assume all the keys are for the same table.
        String key = (String) criteria.keys().nextElement();

        String table = criteria.getTableName(key);
        ColumnMap pk = null;

        if ( !table.equals("") )
        {
            DatabaseMap dbMap = Torque.getDatabaseMap(criteria.getDbName());
            if (dbMap == null)
            {
                throw new TorqueException("dbMap is null");
            }
            if (dbMap.getTable(table) == null)
            {
                throw new TorqueException("dbMap.getTable() is null");
            }

            ColumnMap[] columns = dbMap.getTable(table).getColumns();

            for (int i = 0; i < columns.length; i++)
            {
                if ( columns[i].isPrimaryKey() )
                {
                    pk = columns[i];
                    break;
                }
            }
        }
        return pk;
    }

    /**
     * Convenience method used to update rows in the DB.  Checks if a
     * <i>single</i> int primary key is specified in the Criteria
     * object and uses it to perform the udpate.  If no primary key is
     * specified an Exception will be thrown.
     * <p>
     * Use this method for performing an update of the kind:
     * <p>
     * "WHERE primary_key_id = an int"
     * <p>
     * To perform an update with non-primary key fields in the WHERE
     * clause use doUpdate(criteria, criteria).
     *
     * @param updateValues A Criteria object containing values used in
     *        set clause.
     * @exception TorqueException
     */
    public static void doUpdate(Criteria updateValues)
            throws TorqueException
    {
        boolean doTransaction = updateValues.isUseTransaction();
        DBConnection db = null;
        try
        {
            // Get a connection to the db.
            if (doTransaction)
            {
                db = beginTransaction(updateValues.getDbName());
            }
            else
            {
                db = Torque.getConnection( updateValues.getDbName() );
            }

            doUpdate(updateValues, db);
        }
        finally
        {
            if (doTransaction)
            {
                commitTransaction(db);
            }
            else
            {
                Torque.releaseConnection(db);
            }
        }
    }

    /**
     * Convenience method used to update rows in the DB.  Checks if a
     * <i>single</i> int primary key is specified in the Criteria
     * object and uses it to perform the udpate.  If no primary key is
     * specified an Exception will be thrown.
     * <p>
     * Use this method for performing an update of the kind:
     * <p>
     * "WHERE primary_key_id = an int"
     * <p>
     * To perform an update with non-primary key fields in the WHERE
     * clause use doUpdate(criteria, criteria).
     *
     * @param updateValues A Criteria object containing values used in
     *        set clause.
     * @param dbCon A DBConnection.
     * @exception TorqueException
     */
    public static void doUpdate(Criteria updateValues, DBConnection dbCon)
            throws TorqueException
    {
        ColumnMap pk = getPrimaryKey(updateValues);
        Criteria selectCriteria = null;

        if ( pk != null &&
             updateValues.containsKey(pk.getFullyQualifiedName()) )
        {
            selectCriteria = new Criteria(2);
            selectCriteria.put(pk.getFullyQualifiedName(),
                    updateValues.remove(pk.getFullyQualifiedName()) );
        }
        else
        {
            throw new TorqueException("No PK specified for database update");
        }

        doUpdate( selectCriteria, updateValues, dbCon );
    }

    /**
     * Method used to update rows in the DB.  Rows are selected based
     * on selectCriteria and updated using values in updateValues.
     * <p>
     * Use this method for performing an update of the kind:
     * <p>
     * WHERE some_column = some value AND could_have_another_column =
     * another value AND so on...
     *
     * @param selectCriteria A Criteria object containing values used in where
     *        clause.
     * @param updateValues A Criteria object containing values used in set
     *        clause.
     * @exception TorqueException
     */
    public static void doUpdate(Criteria selectCriteria, Criteria updateValues)
            throws TorqueException
    {
        boolean doTransaction =
            updateValues.isUseTransaction();
        DBConnection db = null;
        try
        {
            // Get a connection to the db.
            if (doTransaction)
            {
                db = beginTransaction(selectCriteria.getDbName());
            }
            else
            {
                db = Torque.getConnection( selectCriteria.getDbName() );
            }
            doUpdate(selectCriteria, updateValues, db);
        }
        finally
        {
            if (doTransaction)
            {
                commitTransaction(db);
            }
            else
            {
                Torque.releaseConnection(db);
            }
        }
    }

    /**
     * Method used to update rows in the DB.  Rows are selected based
     * on selectCriteria and updated using values in updateValues.
     * <p>
     * Use this method for performing an update of the kind:
     * <p>
     * WHERE some_column = some value AND could_have_another_column =
     * another value AND so on.
     *
     * @param selectCriteria A Criteria object containing values used in where
     *        clause.
     * @param updateValues A Criteria object containing values used in set
     *        clause.
     * @param dbCon A DBConnection.
     * @exception TorqueException
     */
    public static void doUpdate(Criteria selectCriteria,
                                Criteria updateValues,
                                DBConnection dbCon)
            throws TorqueException
    {
        DB db = Torque.getDB( selectCriteria.getDbName() );
        DatabaseMap dbMap =
            Torque.getDatabaseMap( selectCriteria.getDbName() );
        Connection connection = null;
        try
        {
            connection = dbCon.getConnection();
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }

        // Set up a list of required tables.  StringStack.add()
        // only adds element if it is unique.
        StringStack tables = new StringStack();
        Iterator it = selectCriteria.keySet().iterator();
        while (it.hasNext())
        {
            tables.add(selectCriteria.getTableName((String) it.next()) );
        }

        for (int i = 0; i < tables.size(); i++)
        {
            KeyDef kd = new KeyDef();
            StringStack whereClause = new StringStack();
            DatabaseMap tempDbMap = dbMap;

            ColumnMap[] columnMaps =
                tempDbMap.getTable( tables.get(i) ).getColumns();
            for (int j = 0; j < columnMaps.length; j++)
            {
                ColumnMap colMap = columnMaps[j];
                if ( colMap.isPrimaryKey() )
                {
                    kd.addAttrib( colMap.getColumnName() );
                }
                String key = new StringBuffer(colMap.getTableName())
                        .append('.').append(colMap.getColumnName()).toString();
                if ( selectCriteria.containsKey(key) )
                {
                    if ( selectCriteria.getComparison(key).equals(Criteria.CUSTOM) )
                    {
                        whereClause.add( selectCriteria.getString( key ));
                    }
                    else
                    {
                        whereClause.add( SqlExpression.build(
                                colMap.getColumnName(),
                                selectCriteria.getValue( key ),
                                selectCriteria.getComparison( key ),
                                selectCriteria.isIgnoreCase(),
                                db ));
                    }
                }
            }
            TableDataSet tds = null;
            try
            {
                // Get affected records.
                tds = new TableDataSet(connection, tables.get(i), kd );
                String sqlSnippet = whereClause.toString(" AND ");
                category.debug("BasePeer.doUpdate: whereClause=" + sqlSnippet);
                tds.where(sqlSnippet);
                tds.fetchRecords();

                if ( tds.size() > 1 && selectCriteria.isSingleRecord() )
                {
                    handleMultipleRecords(tds);
                }
                for (int j = 0; j < tds.size(); j++)
                {
                    Record rec = tds.getRecord(j);
                    BasePeer
                        .insertOrUpdateRecord(rec, tables.get(i), updateValues);
                }
            }
            catch (Exception e)
            {
                throw new TorqueException(e);
            }
            finally
            {
                if (tds != null)
                {
                    try
                    {
                        tds.close();
                    }
                    catch (Exception e)
                    {
                        throw new TorqueException(e);
                    }
                }
            }
        }
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for update, insert, and delete
     * statements.  Use executeQuery() for selects.
     *
     * @param stmt A String with the sql statement to execute.
     * @return The number of rows affected.
     * @exception TorqueException
     */
    public static int executeStatement(String stmt)
            throws TorqueException
    {
        return executeStatement(stmt, Torque.getDefaultDB());
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for update, insert, and delete
     * statements.  Use executeQuery() for selects.
     *
     * @param stmt A String with the sql statement to execute.
     * @param dbName Name of database to connect to.
     * @return The number of rows affected.
     * @exception TorqueException, a generic exception.
     */
    public static int executeStatement(String stmt, String dbName)
            throws TorqueException
    {
        DBConnection db = null;
        int rowCount = -1;
        try
        {
            // Get a connection to the db.
            db = Torque.getConnection( dbName );

            rowCount = executeStatement( stmt, db );
        }
        finally
        {
            Torque.releaseConnection(db);
        }
        return rowCount;
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for update, insert, and delete
     * statements.  Use executeQuery() for selects.
     *
     * @param stmt A String with the sql statement to execute.
     * @param dbCon A DBConnection.
     * @return The number of rows affected.
     * @exception TorqueException
     */
    public static int executeStatement(String stmt, DBConnection dbCon)
            throws TorqueException
    {
        int rowCount = -1;
        Statement statement = null;
        Connection con = null;
        try
        {
            con = dbCon.getConnection();
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }

        try
        {
            statement = con.createStatement();
            rowCount = statement.executeUpdate( stmt );
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException e)
                {
                    throw new TorqueException(e);
                }
            }
        }
        return rowCount;
    }

    /**
     * If the user specified that (s)he only wants to retrieve a
     * single record and multiple records are retrieved, this method
     * is called to handle the situation.  The default behavior is to
     * throw an exception, but subclasses can override this method as
     * needed.
     *
     * @param ds The DataSet which contains multiple records.
     * @exception TorqueException Couldn't handle multiple records.
     */
    protected static void handleMultipleRecords(DataSet ds)
            throws TorqueException
    {
        throw new TorqueException("Criteria expected single Record and " +
                                  "Multiple Records were selected");
    }

    /**
     * @deprecated Use the better-named handleMultipleRecords() instead.
     */
    protected static void handleMultiple(DataSet ds)
            throws TorqueException
    {
        handleMultipleRecords(ds);
    }

    /**
     * This method returns the MapBuilder specified in the
     * TurbineResources.properties file. By default, this is
     * org.apache.torque.util.db.map.TurbineMapBuilder.
     *
     * @return A MapBuilder.
     */
    public static MapBuilder getMapBuilder()
            throws TorqueException
    {
        return getMapBuilder(DEFAULT_MAP_BUILDER.trim());
    }

    /**
     * This method returns the MapBuilder specified in the name
     * parameter.  You should pass in the full path to the class, ie:
     * org.apache.torque.util.db.map.TurbineMapBuilder.  The
     * MapBuilder instances are cached in this class for speed.
     *
     * @return A MapBuilder, or null (and logs the error) if the
     * MapBuilder was not found.
     */
    public static MapBuilder getMapBuilder(String name)
    {
        try
        {
            MapBuilder mb = (MapBuilder) mapBuilders.get(name);
            // Use the 'double-check pattern' for syncing
            //  caching of the MapBuilder.
            if (mb == null)
            {
                synchronized (mapBuilders)
                {
                    mb = (MapBuilder) mapBuilders.get(name);
                    if (mb == null)
                    {
                        mb = (MapBuilder) Class.forName(name).newInstance();
                        // Cache the MapBuilder before it is built.
                        mapBuilders.put(name, mb);
                    }
                }
            }

            // Build the MapBuilder in its own synchronized block to
            //  avoid locking up the whole Hashtable while doing so.
            // Note that *all* threads need to do a sync check on isBuilt()
            //  to avoid grabing an uninitialized MapBuilder. This, however,
            //  is a relatively fast operation.
            synchronized (mb)
            {
               if (!mb.isBuilt())
               {
                  try
                  {
                     mb.doBuild();
                  }
                  catch ( Exception e )
                  {
                      // need to think about whether we'd want to remove
                      //  the MapBuilder from the cache if it can't be
                      //  built correctly...?  pgo
                      throw e;
                  }
               }
            }
            return mb;
        }
        catch (Exception e)
        {
            // Have to catch possible exceptions because method is
            // used in initialization of Peers.  Log the exception and
            // return null.
            String message = "BasePeer.MapBuilder failed trying to instantiate: "
                    + name;
            if (category == null)
            {
                System.out.println (message);
                e.printStackTrace();
            }
            else
            {
                category.error(message, e);
            }
        }
        return null;
    }

    /**
     * Performs a SQL <code>select</code> using a PreparedStatement.
     *
     * @exception TorqueException Error performing database query.
     */
    public static List doPSSelect(Criteria criteria, DBConnection dbCon)
        throws TorqueException
    {
        List v = null;

        StringBuffer qry = new StringBuffer();
        List params = new ArrayList(criteria.size());

        createPreparedStatement (criteria, qry, params);

        PreparedStatement stmt = null;
        try
        {
            stmt = dbCon.getConnection().prepareStatement(qry.toString());

            for (int i = 0; i < params.size(); i++)
            {
                Object param = params.get(i);
                if (param instanceof java.sql.Date)
                {
                    stmt.setDate(i + 1, (java.sql.Date) param);
                }
                else
                {
                    stmt.setString(i + 1, param.toString());
                }
            }

            QueryDataSet qds = null;
            try
            {
                qds = new QueryDataSet( stmt.executeQuery() );
                v = getSelectResults(qds);
            }
            finally
            {
                if (qds != null)
                {
                    qds.close();
                }
            }
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            if (stmt != null)
            {
                try
                {
                    stmt.close();
                }
                catch (SQLException e)
                {
                    throw new TorqueException(e);
                }
            }
        }

        return v;
    }

    /**
     * Do a Prepared Statement select according to the given criteria
     */
    public static List doPSSelect(Criteria criteria) throws Exception
    {
        DBConnection dbCon = Torque.getConnection( criteria.getDbName() );
        List v = null;

        try
        {
            v = doPSSelect (criteria,dbCon);
        }
        finally
        {
            Torque.releaseConnection( dbCon );
        }

        return v;
    }

    /**
     * Create a new PreparedStatement.  It builds a string representation
     * of a query and a list of PreparedStatement parameters.
     */
    public static void createPreparedStatement(Criteria criteria,
                                               StringBuffer queryString,
                                               List params)
        throws TorqueException
    {
        DB db = Torque.getDB( criteria.getDbName() );
        DatabaseMap dbMap = Torque.getDatabaseMap( criteria.getDbName() );

        Query query = new Query();

        StringStack selectModifiers = query.getSelectModifiers();
        StringStack selectClause = query.getSelectClause();
        StringStack fromClause = query.getFromClause();
        StringStack whereClause = query.getWhereClause();
        StringStack orderByClause = query.getOrderByClause();

        StringStack orderBy = criteria.getOrderByColumns();
        boolean ignoreCase = criteria.isIgnoreCase();
        StringStack select = criteria.getSelectColumns();
        Hashtable aliases = criteria.getAsColumns();
        StringStack modifiers = criteria.getSelectModifiers();

        for (int i = 0; i < modifiers.size(); i++)
        {
            selectModifiers.add( modifiers.get(i) );
        }

        for (int i = 0; i < modifiers.size(); i++)
        {
            selectModifiers.add( modifiers.get(i) );
        }

        for (int i = 0; i < select.size(); i++)
        {
            String columnName = select.get(i);
            if (columnName.indexOf('.') == -1)
            {
                throwMalformedColumnNameException("select", columnName);
            }
            String tableName = null;
            selectClause.add(columnName);
            int parenPos = columnName.indexOf('(');
            if (parenPos == -1)
            {
                tableName = columnName.substring(0, columnName.indexOf('.'));
            }
            else
            {
                tableName = columnName.substring(parenPos + 1,
                                                 columnName.indexOf('.') );
            }
            String tableName2 = criteria.getTableForAlias(tableName);
            if ( tableName2 != null )
            {
                fromClause.add(
                    new StringBuffer(tableName.length() +
                                     tableName2.length() + 1)
                    .append(tableName2).append(' ').append(tableName)
                    .toString() );
            }
            else
            {
                fromClause.add(tableName);
            }
        }

        Iterator it = aliases.keySet().iterator();
        while (it.hasNext())
        {
          String key = (String) it.next();
          selectClause.add((String) aliases.get(key) + " AS " + key);
        }

        Iterator critKeys = criteria.keySet().iterator();
        while (critKeys.hasNext())
        {
            String key = (String) critKeys.next();
            Criteria.Criterion criterion =
                    (Criteria.Criterion) criteria.getCriterion(key);
            Criteria.Criterion[] someCriteria =
                    criterion.getAttachedCriterion();

            String table = null;
            for (int i = 0; i < someCriteria.length; i++)
            {
                String tableName = someCriteria[i].getTable();
                table = criteria.getTableForAlias(tableName);
                if ( table != null )
                {
                    fromClause.add(new StringBuffer(tableName.length()
                            + table.length() + 1)
                            .append(table).append(' ').append(tableName)
                            .toString() );
                }
                else
                {
                    fromClause.add(tableName);
                    table = tableName;
                }

                boolean ignorCase = ((criteria.isIgnoreCase()
                        || someCriteria[i].isIgnoreCase())
                        && (dbMap.getTable(table).getColumn(
                        someCriteria[i].getColumn()).getType()
                        instanceof String));

                someCriteria[i].setIgnoreCase(ignorCase);
            }

            criterion.setDB(db);
            StringBuffer sb = new StringBuffer();
            criterion.appendPsTo(sb,params);
            whereClause.add( sb.toString() );

        }

        List join = criteria.getJoinL();
        if ( join != null)
        {
            for ( int i = 0; i < join.size(); i++ )
            {
                String join1 = (String)join.get(i);
                String join2 = (String)criteria.getJoinR().get(i);
                if (join1.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("join", join1);
                }
                if (join2.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("join", join2);
                }

                String tableName = join1.substring(0, join1.indexOf('.'));
                String table = criteria.getTableForAlias(tableName);
                if ( table != null )
                {
                    fromClause.add(new StringBuffer(tableName.length()
                            + table.length() + 1)
                            .append(table).append(' ').append(tableName)
                            .toString() );
                }
                else
                {
                    fromClause.add(tableName);
                }

                int dot = join2.indexOf('.');
                tableName = join2.substring(0, dot);
                table = criteria.getTableForAlias(tableName);
                if ( table != null )
                {
                    fromClause.add(new StringBuffer(tableName.length()
                            + table.length() + 1)
                            .append(table).append(' ').append(tableName)
                            .toString() );
                }
                else
                {
                    fromClause.add(tableName);
                    table = tableName;
                }

                boolean ignorCase = (criteria.isIgnoreCase()
                        && (dbMap.getTable(table).getColumn(
                        join2.substring(dot + 1, join2.length()) )
                        .getType() instanceof String));

                whereClause.add(SqlExpression.buildInnerJoin(
                        join1, join2, ignorCase, db));
            }
        }

        if ( orderBy != null && orderBy.size() > 0)
        {
            // Check for each String/Character column and apply
            // toUpperCase().
            for (int i = 0; i < orderBy.size(); i++)
            {
                String orderByColumn = orderBy.get(i);
                if (orderByColumn.indexOf('.') == -1)
                {
                    throwMalformedColumnNameException("order by",
                            orderByColumn);
                }
                String table = orderByColumn.substring(0,
                        orderByColumn.indexOf('.'));
                // See if there's a space (between the column list and sort
                // order in ORDER BY table.column DESC).
                int spacePos = orderByColumn.indexOf(' ');
                String columnName;
                if (spacePos == -1)
                {
                    columnName = orderByColumn
                        .substring(orderByColumn.indexOf('.') + 1);
                }
                else
                {
                    columnName = orderByColumn
                        .substring(orderByColumn.indexOf('.') + 1, spacePos);
                }
                ColumnMap column = dbMap.getTable(table).getColumn(columnName);
                if ( column.getType() instanceof String )
                {
                    if (spacePos == -1)
                    {
                        orderByClause.add(db.ignoreCaseInOrderBy(
                                orderByColumn));
                    }
                    else
                    {
                        orderByClause.add(db.ignoreCaseInOrderBy(
                                orderByColumn.substring(0, spacePos))
                                + orderByColumn.substring(spacePos) );
                    }
                    selectClause.add( db.ignoreCaseInOrderBy(table + '.'
                            + columnName) );
                }
                else
                {
                    orderByClause.add(orderByColumn);
                }
            }
        }

        // Limit the number of rows returned.
        int limit = criteria.getLimit();
        int offset = criteria.getOffset();
        String limitString = null;
        if ( offset > 0 && db.supportsNativeOffset() )
        {
            switch(db.getLimitStyle())
            {
                case DB.LIMIT_STYLE_MYSQL:
                    limitString = new StringBuffer().append(offset)
                                                    .append(", ")
                                                    .append(limit)
                                                    .toString();
                    break;
                case DB.LIMIT_STYLE_POSTGRES:
                    limitString = new StringBuffer().append(limit)
                                                    .append(", ")
                                                    .append(offset)
                                                    .toString();
                    break;
            }

            // Now set the criteria's limit and offset to return the
            // full resultset since the results are limited on the
            // server.
            criteria.setLimit(-1);
            criteria.setOffset(0);
        }
        else if (limit > 0 && db.supportsNativeLimit() )
        {
            limitString = String.valueOf(limit);

            // Now set the criteria's limit to return the full
            // resultset since the results are limited on the server.
            criteria.setLimit(-1);
        }

        if (limitString != null)
        {
            switch (db.getLimitStyle())
            {
            case DB.LIMIT_STYLE_ORACLE:
                whereClause.add("rownum <= " + limitString);
                break;
            /* Don't have a Sybase install to validate this against. (dlr)
            case DB.LIMIT_STYLE_SYBASE:
                query.setRowcount(limitString);
                break;
            */
            default:
                query.setLimit(limitString);
            }
        }

        String sql = query.toString();
        category.debug(sql);
        queryString.append (sql);
    }

    /**
     * Throws a TorqueException with the malformed column name error
     * message.  The error message looks like this:<p>
     *
     * <code>
     *     Malformed column name in Criteria [criteriaPhrase]:
     *     '[columnName]' is not of the form 'table.column'
     * </code>
     *
     * @param criteriaPhrase a String, one of "select", "join", or "order by"
     * @param columnName a String containing the offending column name
     */
    private static void throwMalformedColumnNameException
        (String criteriaPhrase, String columnName)
        throws TorqueException
    {
        throw new TorqueException("Malformed column name in Criteria " +
                                  criteriaPhrase + ": '" + columnName +
                                  "' is not of the form 'table.column'");
    }
}
