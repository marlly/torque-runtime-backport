package org.apache.torque.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.Database;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.MapBuilder;
import org.apache.torque.map.TableMap;
import org.apache.torque.oid.IdGenerator;
import org.apache.torque.om.NumberKey;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.SimpleKey;
import org.apache.torque.om.StringKey;

import com.workingdogs.village.Column;
import com.workingdogs.village.DataSet;
import com.workingdogs.village.DataSetException;
import com.workingdogs.village.KeyDef;
import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.Schema;
import com.workingdogs.village.TableDataSet;

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
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:vido@ldh.org">Augustin Vidovic</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public abstract class BasePeer
        implements Serializable
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

    /** the log */
    protected static final Log log = LogFactory.getLog(BasePeer.class);

    private static void throwTorqueException(Exception e)
        throws TorqueException
    {
        if (e instanceof TorqueException)
        {
            throw (TorqueException) e;
        }
        else
        {
            throw new TorqueException(e);
        }
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
        return initTableSchema(tableName, Torque.getDefaultDB());
    }

    /**
     * Sets up a Schema for a table.  This schema is then normally
     * used as the argument for initTableColumns
     *
     * @param tableName The propery name for the database in the
     * configuration file.
     * @param dbName The name of the database.
     * @return A Schema.
     */
    public static Schema initTableSchema(String tableName, String dbName)
    {
        Schema schema = null;
        Connection con = null;

        try
        {
            con = Torque.getConnection(dbName);
            schema = new Schema().schema(con, tableName);
        }
        catch (Exception e)
        {
            log.error(e);
            throw new Error("Error in BasePeer.initTableSchema("
                    + tableName
                    + "): "
                    + e.getMessage());
        }
        finally
        {
            Torque.closeConnection(con);
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
            log.error(e);
            throw new Error(
                "Error in BasePeer.initTableColumns(): " + e.getMessage());
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
        String[] columnNames = new String[columns.length];
        for (int i = 0; i < columns.length; i++)
        {
            columnNames[i] = columns[i].name().toUpperCase();
        }
        return columnNames;
    }

    /**
     * Convenience method to create a String array of criteria keys.
     *
     * @param tableName Name of table.
     * @param columnNames A String[].
     * @return A String[].
     */
    public static String[] initCriteriaKeys(
        String tableName,
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
     * Convenience method that uses straight JDBC to delete multiple
     * rows.  Village throws an Exception when multiple rows are
     * deleted.
     *
     * @param con A Connection.
     * @param table The table to delete records from.
     * @param column The column in the where clause.
     * @param value The value of the column.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void deleteAll(
        Connection con,
        String table,
        String column,
        int value)
        throws TorqueException
    {
        Statement statement = null;
        try
        {
            statement = con.createStatement();

            StringBuffer query = new StringBuffer();
            query.append("DELETE FROM ")
                .append(table)
                .append(" WHERE ")
                .append(column)
                .append(" = ")
                .append(value);

            statement.executeUpdate(query.toString());
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void deleteAll(String table, String column, int value)
        throws TorqueException
    {
        Connection con = null;
        try
        {
            // Get a connection to the db.
            con = Torque.getConnection(Torque.getDefaultDB());
            deleteAll(con, table, column, value);
        }
        finally
        {
            Torque.closeConnection(con);
        }
    }

    /**
     * Method to perform deletes based on values and keys in a
     * Criteria.
     *
     * @param criteria The criteria to use.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void doDelete(Criteria criteria) throws TorqueException
    {
        Connection con = null;
        try
        {
            con = Transaction.beginOptional(
                    criteria.getDbName(),
                    criteria.isUseTransaction());
            doDelete(criteria, con);
            Transaction.commit(con);
        }
        catch (TorqueException e)
        {
            Transaction.safeRollback(con);
            throw e;
        }
    }

    /**
     * Method to perform deletes based on values and keys in a Criteria.
     *
     * @param criteria The criteria to use.
     * @param con A Connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void doDelete(Criteria criteria, Connection con)
        throws TorqueException
    {
        String dbName = criteria.getDbName();
        final DatabaseMap dbMap = Torque.getDatabaseMap(dbName);

        // This Callback adds all tables to the Table set which
        // are referenced from a cascading criteria. As a result, all
        // data that is referenced through foreign keys will also be
        // deleted.
        SQLBuilder.TableCallback tc = new SQLBuilder.TableCallback() {
                public void process (Set tables, String key, Criteria crit)
                {
                    if (crit.isCascade())
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
                                // updating, but that is not implemented.
                                if (columnMaps[j].isForeignKey()
                                        && columnMaps[j].isPrimaryKey()
                                        && key.equals(columnMaps[j].getRelatedName()))
                                {
                                    tables.add(tableMaps[i].getName());
                                    crit.add(columnMaps[j].getFullyQualifiedName(),
                                            crit.getValue(key));
                                }
                            }
                        }
                    }
                }
            };

        Set tables = SQLBuilder.getTableSet(criteria, tc);

        try
        {
            processTables(criteria, tables, con, new ProcessCallback() {
                    public void process(String table, String dbName, Record rec)
                        throws Exception
                    {
                        rec.markToBeDeleted();
                        rec.save();
                    }
                });
        }
        catch (Exception e)
        {
            throwTorqueException(e);
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static ObjectKey doInsert(Criteria criteria) throws TorqueException
    {
        Connection con = null;
        ObjectKey id = null;

        try
        {
            con = Transaction.beginOptional(
                    criteria.getDbName(),
                    criteria.isUseTransaction());
            id = doInsert(criteria, con);
            Transaction.commit(con);
        }
        catch (TorqueException e)
        {
            Transaction.safeRollback(con);
            throw e;
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
     * @param con A Connection.
     * @return An Object which is the id of the row that was inserted
     * (if the table has a primary key) or null (if the table does not
     * have a primary key).
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static ObjectKey doInsert(Criteria criteria, Connection con)
        throws TorqueException
    {
        SimpleKey id = null;

        // Get the table name and method for determining the primary
        // key value.
        String table = null;
        Iterator keys = criteria.keySet().iterator();
        if (keys.hasNext())
        {
            table = criteria.getTableName((String) keys.next());
        }
        else
        {
            throw new TorqueException("Database insert attempted without "
                    + "anything specified to insert");
        }

        String dbName = criteria.getDbName();
        Database database = Torque.getDatabase(dbName);
        DatabaseMap dbMap = database.getDatabaseMap();
        TableMap tableMap = dbMap.getTable(table);
        Object keyInfo = tableMap.getPrimaryKeyMethodInfo();
        IdGenerator keyGen
                = database.getIdGenerator(tableMap.getPrimaryKeyMethod());

        ColumnMap pk = getPrimaryKey(criteria);

        // If the keyMethod is SEQUENCE or IDBROKERTABLE, get the id
        // before the insert.
        if (keyGen != null && keyGen.isPriorToInsert())
        {
            // pk will be null if there is no primary key defined for the table
            // we're inserting into.
            if (pk != null && !criteria.containsKey(pk.getFullyQualifiedName()))
            {
                id = getId(pk, keyGen, con, keyInfo);
                criteria.add(pk.getFullyQualifiedName(), id);
            }
        }

        // Use Village to perform the insert.
        TableDataSet tds = null;
        try
        {
            String tableName = SQLBuilder.getFullTableName(table, dbName);
            tds = new TableDataSet(con, tableName);
            Record rec = tds.addRecord();
            // not the fully qualified name, insertOrUpdateRecord wants to use table as an index...
            BasePeer.insertOrUpdateRecord(rec, table, dbName, criteria);
        }
        catch (DataSetException e)
        {
            throwTorqueException(e);
        }
        catch (SQLException e)
        {
            throwTorqueException(e);
        }
        catch (TorqueException e)
        {
            throwTorqueException(e);
        }
        finally
        {
            VillageUtils.close(tds);
        }

        // If the primary key column is auto-incremented, get the id
        // now.
        if (keyGen != null && keyGen.isPostInsert())
        {
            id = getId(pk, keyGen, con, keyInfo);
        }

        return id;
    }

    /**
     * Create an Id for insertion in the Criteria
     *
     * @param pk ColumnMap for the Primary key
     * @param keyGen The Id Generator object
     * @param con The SQL Connection to run the id generation under
     * @param keyInfo KeyInfo Parameter from the Table map
     *
     * @return A simple Key representing the new Id value
     * @throws TorqueException Possible errors get wrapped in here.
     */
    private static SimpleKey getId(ColumnMap pk, IdGenerator keyGen, Connection con, Object keyInfo)
            throws TorqueException
    {
        SimpleKey id = null;

        try
        {
            if (pk != null && keyGen != null)
            {
                if (pk.getType() instanceof Number)
                {
                    id = new NumberKey(
                            keyGen.getIdAsBigDecimal(con, keyInfo));
                }
                else
                {
                    id = new StringKey(keyGen.getIdAsString(con, keyInfo));
                }
            }
        }
        catch (Exception e)
        {
            throwTorqueException(e);
        }
        return id;
    }

    /**
     * Grouping of code used in both doInsert() and doUpdate()
     * methods.  Sets up a Record for saving.
     *
     * @param rec A Record.
     * @param table Name of table.
     * @param criteria A Criteria.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private static void insertOrUpdateRecord(
        Record rec,
        String table,
        String dbName,
        Criteria criteria)
        throws TorqueException
    {
        DatabaseMap dbMap = Torque.getDatabaseMap(dbName);

        ColumnMap[] columnMaps = dbMap.getTable(table).getColumns();
        boolean shouldSave = false;
        for (int j = 0; j < columnMaps.length; j++)
        {
            ColumnMap colMap = columnMaps[j];
            String colName = colMap.getColumnName();
            String key = new StringBuffer(colMap.getTableName())
                    .append('.')
                    .append(colName)
                    .toString();
            if (criteria.containsKey(key))
            {
                try
                {
                    VillageUtils.setVillageValue(criteria, key, rec, colName);
                    shouldSave = true;
                }
                catch (Exception e)
                {
                    throwTorqueException(e);
                }
            }
        }

        if (shouldSave)
        {
            try
            {
                rec.save();
            }
            catch (Exception e)
            {
                throwTorqueException(e);
            }
        }
        else
        {
            throw new TorqueException("No changes to save");
        }
    }

    /**
     * Method to create an SQL query for display only based on values in a
     * Criteria.
     *
     * @param criteria A Criteria.
     * @return the SQL query for display
     * @exception TorqueException Trouble creating the query string.
     */
    static String createQueryDisplayString(Criteria criteria)
        throws TorqueException
    {
        return createQuery(criteria).toString();
    }

    /**
     * Method to create an SQL query for actual execution based on values in a
     * Criteria.
     *
     * @param criteria A Criteria.
     * @return the SQL query for actual execution
     * @exception TorqueException Trouble creating the query string.
     */
    public static String createQueryString(Criteria criteria)
        throws TorqueException
    {
        Query query = createQuery(criteria);
        return query.toString();
    }

    /**
     * Method to create an SQL query based on values in a Criteria.  Note that
     * final manipulation of the limit and offset are performed when the query
     * is actually executed.
     *
     * @param criteria A Criteria.
     * @return the sql query
     * @exception TorqueException Trouble creating the query string.
     */
    static Query createQuery(Criteria criteria)
        throws TorqueException
    {
        return SQLBuilder.buildQueryClause(criteria, null, new SQLBuilder.QueryCallback() {
                public String process(Criteria.Criterion criterion, List params)
                {
                    return criterion.toString();
                }
            });
    }

    /**
     * Returns all results.
     *
     * @param criteria A Criteria.
     * @return List of Record objects.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List doSelect(Criteria criteria) throws TorqueException
    {
        Connection con = null;
        List results = null;

        try
        {
            con = Transaction.beginOptional(
                    criteria.getDbName(),
                    criteria.isUseTransaction());
            results = doSelect(criteria, con);
            Transaction.commit(con);
        }
        catch (TorqueException e)
        {
            Transaction.safeRollback(con);
            throw e;
        }
        return results;
    }

    /**
     * Returns all results.
     *
     * @param criteria A Criteria.
     * @param con A Connection.
     * @return List of Record objects.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List doSelect(Criteria criteria, Connection con)
        throws TorqueException
    {
        Query query = createQuery(criteria);
        DB dbadapter = Torque.getDB(criteria.getDbName());

        // Call Village depending on the capabilities of the DB
        return executeQuery(query.toString(),
                dbadapter.supportsNativeOffset() ? 0 : criteria.getOffset(),
                dbadapter.supportsNativeLimit() ? -1 : criteria.getLimit(),
                criteria.isSingleRecord(),
                con);
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for select statements only.  Use
     * executeStatement for update, insert, and delete operations.
     *
     * @param queryString A String with the sql statement to execute.
     * @return List of Record objects.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List executeQuery(String queryString) throws TorqueException
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List executeQuery(String queryString, String dbName)
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List executeQuery(
        String queryString,
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
     * @param con A Connection.
     * @return List of Record objects.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List executeQuery(
        String queryString,
        boolean singleRecord,
        Connection con)
        throws TorqueException
    {
        return executeQuery(queryString, 0, -1, singleRecord, con);
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List executeQuery(
        String queryString,
        int start,
        int numberOfResults,
        String dbName,
        boolean singleRecord)
        throws TorqueException
    {
        Connection con = null;
        List results = null;
        try
        {
            con = Torque.getConnection(dbName);
            // execute the query
            results = executeQuery(
                    queryString,
                    start,
                    numberOfResults,
                    singleRecord,
                    con);
        }
        finally
        {
            Torque.closeConnection(con);
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
     * @param con A Connection.
     * @return List of Record objects.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List executeQuery(
        String queryString,
        int start,
        int numberOfResults,
        boolean singleRecord,
        Connection con)
        throws TorqueException
    {
        QueryDataSet qds = null;
        List results = Collections.EMPTY_LIST;
        try
        {
            // execute the query
            long startTime = System.currentTimeMillis();
            qds = new QueryDataSet(con, queryString);
            if (log.isDebugEnabled())
            {
                log.debug("Elapsed time="
                        + (System.currentTimeMillis() - startTime) + " ms");
            }
            results = getSelectResults(
                    qds, start, numberOfResults, singleRecord);
        }
        catch (DataSetException e)
        {
            throwTorqueException(e);
        }
        catch (SQLException e)
        {
            throwTorqueException(e);
        }
        finally
        {
            VillageUtils.close(qds);
        }
        return results;
    }

    /**
     * Returns all records in a QueryDataSet as a List of Record
     * objects.  Used for functionality like util.LargeSelect.
     *
     * @see #getSelectResults(QueryDataSet, int, int, boolean)
     * @param qds the QueryDataSet
     * @return a List of Record objects
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List getSelectResults(QueryDataSet qds)
        throws TorqueException
    {
        return getSelectResults(qds, 0, -1, false);
    }

    /**
     * Returns all records in a QueryDataSet as a List of Record
     * objects.  Used for functionality like util.LargeSelect.
     *
     * @see #getSelectResults(QueryDataSet, int, int, boolean)
     * @param qds the QueryDataSet
     * @param singleRecord
     * @return a List of Record objects
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List getSelectResults(QueryDataSet qds, boolean singleRecord)
        throws TorqueException
    {
        return getSelectResults(qds, 0, -1, singleRecord);
    }

    /**
     * Returns numberOfResults records in a QueryDataSet as a List
     * of Record objects.  Starting at record 0.  Used for
     * functionality like util.LargeSelect.
     *
     * @see #getSelectResults(QueryDataSet, int, int, boolean)
     * @param qds the QueryDataSet
     * @param numberOfResults
     * @param singleRecord
     * @return a List of Record objects
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List getSelectResults(
        QueryDataSet qds,
        int numberOfResults,
        boolean singleRecord)
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
     * functionality like util.LargeSelect.
     *
     * @param qds The <code>QueryDataSet</code> to extract results
     * from.
     * @param start The index from which to start retrieving
     * <code>Record</code> objects from the data set.
     * @param numberOfResults The number of results to return (or
     * <code> -1</code> for all results).
     * @param singleRecord Whether or not we want to select only a
     * single record.
     * @return A <code>List</code> of <code>Record</code> objects.
     * @exception TorqueException If any <code>Exception</code> occurs.
     */
    public static List getSelectResults(
        QueryDataSet qds,
        int start,
        int numberOfResults,
        boolean singleRecord)
        throws TorqueException
    {
        List results = null;
        try
        {
            if (numberOfResults < 0)
            {
                results = new ArrayList();
                qds.fetchRecords();
            }
            else
            {
                results = new ArrayList(numberOfResults);
                qds.fetchRecords(start, numberOfResults);
            }

            int startRecord = 0;

            //Offset the correct number of records
            if (start > 0 && numberOfResults <= 0)
            {
                startRecord = start;
            }

            // Return a List of Record objects.
            for (int i = startRecord; i < qds.size(); i++)
            {
                Record rec = qds.getRecord(i);
                results.add(rec);
            }

            if (results.size() > 1 && singleRecord)
            {
                handleMultipleRecords(qds);
            }
        }
        catch (Exception e)
        {
            throwTorqueException(e);
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    private static ColumnMap getPrimaryKey(Criteria criteria)
        throws TorqueException
    {
        // Assume all the keys are for the same table.
        String key = (String) criteria.keys().nextElement();

        String table = criteria.getTableName(key);
        ColumnMap pk = null;

        if (!table.equals(""))
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
                if (columns[i].isPrimaryKey())
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void doUpdate(Criteria updateValues) throws TorqueException
    {
        Connection con = null;
        try
        {
            con = Transaction.beginOptional(
                    updateValues.getDbName(),
                    updateValues.isUseTransaction());
            doUpdate(updateValues, con);
            Transaction.commit(con);
        }
        catch (TorqueException e)
        {
            Transaction.safeRollback(con);
            throw e;
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
     * set clause.
     * @param con A Connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void doUpdate(Criteria updateValues, Connection con)
        throws TorqueException
    {
        ColumnMap pk = getPrimaryKey(updateValues);
        Criteria selectCriteria = null;

        if (pk != null && updateValues.containsKey(pk.getFullyQualifiedName()))
        {
            selectCriteria = new Criteria(2);
            selectCriteria.put(pk.getFullyQualifiedName(),
                updateValues.remove(pk.getFullyQualifiedName()));
        }
        else
        {
            throw new TorqueException("No PK specified for database update");
        }

        doUpdate(selectCriteria, updateValues, con);
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
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void doUpdate(Criteria selectCriteria, Criteria updateValues)
        throws TorqueException
    {
        Connection con = null;
        try
        {
            con = Transaction.beginOptional(
                    selectCriteria.getDbName(),
                    updateValues.isUseTransaction());
            doUpdate(selectCriteria, updateValues, con);
            Transaction.commit(con);
        }
        catch (TorqueException e)
        {
            Transaction.safeRollback(con);
            throw e;
        }
    }

    /**
     * Method used to update rows in the DB.  Rows are selected based
     * on criteria and updated using values in updateValues.
     * <p>
     * Use this method for performing an update of the kind:
     * <p>
     * WHERE some_column = some value AND could_have_another_column =
     * another value AND so on.
     *
     * @param criteria A Criteria object containing values used in where
     *        clause.
     * @param updateValues A Criteria object containing values used in set
     *        clause.
     * @param con A Connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void doUpdate(
        Criteria criteria,
        final Criteria updateValues,
        Connection con)
        throws TorqueException
    {
        Set tables = SQLBuilder.getTableSet(criteria, null);

        try
        {
            processTables(criteria, tables, con, new ProcessCallback() {
                    public void process (String table, String dbName, Record rec)
                        throws Exception
                    {
                        // Callback must be called with table name without Schema!
                        BasePeer.insertOrUpdateRecord(rec, table, dbName, updateValues);
                    }
                });
        }
        catch (Exception e)
        {
            throwTorqueException(e);
        }
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for update, insert, and delete
     * statements.  Use executeQuery() for selects.
     *
     * @param statementString A String with the sql statement to execute.
     * @return The number of rows affected.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static int executeStatement(String statementString) throws TorqueException
    {
        return executeStatement(statementString, Torque.getDefaultDB());
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for update, insert, and delete
     * statements.  Use executeQuery() for selects.
     *
     * @param statementString A String with the sql statement to execute.
     * @param dbName Name of database to connect to.
     * @return The number of rows affected.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static int executeStatement(String statementString, String dbName)
        throws TorqueException
    {
        Connection con = null;
        int rowCount = -1;
        try
        {
            con = Torque.getConnection(dbName);
            rowCount = executeStatement(statementString, con);
        }
        finally
        {
            Torque.closeConnection(con);
        }
        return rowCount;
    }

    /**
     * Utility method which executes a given sql statement.  This
     * method should be used for update, insert, and delete
     * statements.  Use executeQuery() for selects.
     *
     * @param statementString A String with the sql statement to execute.
     * @param con A Connection.
     * @return The number of rows affected.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static int executeStatement(String statementString, Connection con)
        throws TorqueException
    {
        int rowCount = -1;
        Statement statement = null;
        try
        {
            statement = con.createStatement();
            rowCount = statement.executeUpdate(statementString);
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
        throw new TorqueException("Criteria expected single Record and "
                + "Multiple Records were selected");
    }

    /**
     * This method returns the MapBuilder specified in the name
     * parameter.  You should pass in the full path to the class, ie:
     * org.apache.torque.util.db.map.TurbineMapBuilder.  The
     * MapBuilder instances are cached in the TorqueInstance for speed.
     *
     * @param name name of the MapBuilder
     * @return A MapBuilder, not null
     * @throws TorqueException if the Map Builder cannot be instantiated
     * @deprecated Use Torque.getMapBuilder(name) instead
     */
    public static MapBuilder getMapBuilder(String name)
        throws TorqueException
    {
        return Torque.getMapBuilder(name);
    }

    /**
     * Performs a SQL <code>select</code> using a PreparedStatement.
     * Note: this method does not handle null criteria values.
     *
     * @param criteria
     * @param con
     * @return a List of Record objects.
     * @throws TorqueException Error performing database query.
     */
    public static List doPSSelect(Criteria criteria, Connection con)
        throws TorqueException
    {
        List v = null;

        StringBuffer qry = new StringBuffer();
        List params = new ArrayList(criteria.size());

        createPreparedStatement(criteria, qry, params);

        PreparedStatement statement = null;
        try
        {
            statement = con.prepareStatement(qry.toString());

            for (int i = 0; i < params.size(); i++)
            {
                Object param = params.get(i);
                if (param instanceof java.sql.Date)
                {
                    statement.setDate(i + 1, (java.sql.Date) param);
                }
                else if (param instanceof NumberKey)
                {
                    statement.setBigDecimal(i + 1,
                        ((NumberKey) param).getBigDecimal());
                }
                else
                {
                    statement.setString(i + 1, param.toString());
                }
            }

            QueryDataSet qds = null;
            try
            {
                qds = new QueryDataSet(statement.executeQuery());
                v = getSelectResults(qds);
            }
            finally
            {
                VillageUtils.close(qds);
            }
        }
        catch (DataSetException e)
        {
            throwTorqueException(e);
        }
        catch (SQLException e)
        {
            throwTorqueException(e);
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
        return v;
    }

    /**
     * Do a Prepared Statement select according to the given criteria
     *
     * @param criteria
     * @return a List of Record objects.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static List doPSSelect(Criteria criteria) throws TorqueException
    {
        Connection con = Torque.getConnection(criteria.getDbName());
        List v = null;

        try
        {
            v = doPSSelect(criteria, con);
        }
        finally
        {
            Torque.closeConnection(con);
        }
        return v;
    }

    /**
     * Create a new PreparedStatement.  It builds a string representation
     * of a query and a list of PreparedStatement parameters.
     *
     * @param criteria
     * @param queryString
     * @param params
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void createPreparedStatement(
        Criteria criteria,
        StringBuffer queryString,
        List params)
        throws TorqueException
    {
        Query query = SQLBuilder.buildQueryClause(criteria, params, new SQLBuilder.QueryCallback() {
                public String process(Criteria.Criterion criterion, List params)
                {
                    StringBuffer sb = new StringBuffer();
                    criterion.appendPsTo(sb, params);
                    return sb.toString();
                }
            });

        String sql = query.toString();
        log.debug(sql);

        queryString.append(sql);
    }

    /**
     * Checks all columns in the criteria to see whether
     * booleanchar and booleanint columns are queried with a boolean.
     * If yes, the query values are mapped onto values the database
     * does understand, i.e. 0 and 1 for booleanints and N and Y for
     * booleanchar columns.
     *
     * @param criteria The criteria to be checked for booleanint and booleanchar
     *        columns.
     * @param defaultTableMap the table map to be used if the table name is
     *        not given in a column.
     * @throws TorqueException if the database map for the criteria cannot be
     *         retrieved.
     */
    public static void correctBooleans(
            Criteria criteria,
            TableMap defaultTableMap)
        throws TorqueException
    {
        Iterator keyIt = criteria.keySet().iterator();
        while (keyIt.hasNext())
        {
            String key = (String) keyIt.next();
            String columnName;
            TableMap tableMap = null;
            int dotPosition = key.lastIndexOf(".");
            if (dotPosition == -1)
            {
                columnName = key;
                tableMap = defaultTableMap;
            }
            else
            {
                columnName = key.substring(dotPosition + 1);
                String tableName = key.substring(0, dotPosition);
                String databaseName = criteria.getDbName();
                if (databaseName == null)
                {
                    databaseName = Torque.getDefaultDB();
                }
                DatabaseMap databaseMap = Torque.getDatabaseMap(databaseName);
                if (databaseMap != null)
                {
                    tableMap = databaseMap.getTable(tableName);
                }
                if (tableMap == null)
                {
                    // try aliases
                    Map aliases = criteria.getAliases();
                    if (aliases != null && aliases.get(tableName) != null)
                    {
                        tableName = (String) aliases.get(tableName);
                        tableMap = databaseMap.getTable(tableName);
                    }
                }
                if (tableMap == null)
                {
                    // no description of table available, do not modify anything
                    break;
                }
            }

            ColumnMap columnMap = tableMap.getColumn(columnName);
            if (columnMap != null)
            {
                if ("BOOLEANINT".equals(columnMap.getTorqueType()))
                {
                    Criteria.Criterion criterion = criteria.getCriterion(key);
                    replaceBooleanValues(
                            criterion,
                            new Integer(1),
                            new Integer(0));
                }
                else if ("BOOLEANCHAR".equals(columnMap.getTorqueType()))
                {
                    Criteria.Criterion criterion = criteria.getCriterion(key);
                    replaceBooleanValues(criterion, "Y", "N");
                 }
            }
        }
    }

    /**
     * Replaces any Boolean value in the criterion and its attached Criterions
     * by trueValue if the Boolean equals <code>Boolean.TRUE</code>
     * and falseValue if the Boolean equals <code>Boolean.FALSE</code>.
     *
     * @param criterion the criterion to replace Boolean values in.
     * @param trueValue the value by which Boolean.TRUE should be replaced.
     * @param falseValue the value by which Boolean.FALSE should be replaced.
     */
    private static void replaceBooleanValues(
            Criteria.Criterion criterion,
            Object trueValue,
            Object falseValue)
    {
        // attachedCriterions also contains the criterion itself,
        // so no additional treatment is needed for the criterion itself.
        Criteria.Criterion[] attachedCriterions
            = criterion.getAttachedCriterion();
        for (int i = 0; i < attachedCriterions.length; ++i)
        {
            Object criterionValue
                    = attachedCriterions[i].getValue();
            if (criterionValue instanceof Boolean)
            {
                Boolean booleanValue = (Boolean) criterionValue;
                attachedCriterions[i].setValue(
                        Boolean.TRUE.equals(booleanValue)
                                ? trueValue
                                : falseValue);
            }

        }

    }

    /**
     * Process the result of a Table list generation.
     * This runs the statements onto the list of tables and
     * provides a callback hook to add functionality.
     *
     * This method should've been in SQLBuilder, but is uses the handleMultipleRecords callback thingie..
     *
     * @param crit The criteria
     * @param tables A set of Tables to run on
     * @param con The SQL Connection to run the statements on
     * @param pc A ProcessCallback object
     *
     * @throws Exception An Error occured (should be wrapped into TorqueException)
     */
    private static void processTables(Criteria crit, Set tables, Connection con, ProcessCallback pc)
            throws Exception
    {
        String dbName = crit.getDbName();
        DB db = Torque.getDB(dbName);
        DatabaseMap dbMap = Torque.getDatabaseMap(dbName);

        // create the statements for the tables
        for (Iterator it = tables.iterator(); it.hasNext();)
        {
            String table = (String) it.next();
            KeyDef kd = new KeyDef();
            Set whereClause = new HashSet();

            ColumnMap[] columnMaps = dbMap.getTable(table).getColumns();

            for (int j = 0; j < columnMaps.length; j++)
            {
                ColumnMap colMap = columnMaps[j];
                if (colMap.isPrimaryKey())
                {
                    kd.addAttrib(colMap.getColumnName());
                }

                String key = new StringBuffer(colMap.getTableName())
                        .append('.')
                        .append(colMap.getColumnName())
                        .toString();

                if (crit.containsKey(key))
                {
                    if (crit
                            .getComparison(key)
                            .equals(Criteria.CUSTOM))
                    {
                        whereClause.add(crit.getString(key));
                    }
                    else
                    {
                        whereClause.add(
                                SqlExpression.build(
                                        colMap.getColumnName(),
                                        crit.getValue(key),
                                        crit.getComparison(key),
                                        crit.isIgnoreCase(),
                                        db));
                    }
                }
            }

            // Execute the statement for each table
            TableDataSet tds = null;
            try
            {
                String tableName = SQLBuilder.getFullTableName(table, dbName);

                // Get affected records.
                tds = new TableDataSet(con, tableName, kd);
                String sqlSnippet = StringUtils.join(whereClause.iterator(), " AND ");

                if (log.isDebugEnabled())
                {
                    log.debug("BasePeer: whereClause=" + sqlSnippet);
                }

                tds.where(sqlSnippet);
                tds.fetchRecords();

                if (tds.size() > 1 && crit.isSingleRecord())
                {
                    handleMultipleRecords(tds);
                }

                for (int j = 0; j < tds.size(); j++)
                {
                    Record rec = tds.getRecord(j);

                    if (pc != null)
                    {
                        // Table name _without_ schema!
                        pc.process(table, dbName, rec);
                    }
                }
            }
            finally
            {
                VillageUtils.close(tds);
            }
        }
    }

    /**
     * Inner Interface that defines the Callback method for
     * the Record Processing
     */
    protected interface ProcessCallback
    {
        void process (String table, String dbName, Record rec)
                throws Exception;
    }
}
