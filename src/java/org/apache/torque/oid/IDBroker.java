package org.apache.torque.oid;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.util.Transaction;

//!!
// NOTE:
// It would be nice to decouple this from
// Torque. This is a great stand-alone utility.

/**
 * This method of ID generation is used to ensure that code is
 * more database independent.  For example, MySQL has an auto-increment
 * feature while Oracle uses sequences.  It caches several ids to
 * avoid needing a Connection for every request.
 *
 * This class uses the table ID_TABLE defined in
 * conf/master/id-table-schema.xml.  The columns in ID_TABLE are used as
 * follows:<br>
 *
 * ID_TABLE_ID - The PK for this row (any unique int).<br>
 * TABLE_NAME - The name of the table you want ids for.<br>
 * NEXT_ID - The next id returned by IDBroker when it queries the
 *           database (not when it returns an id from memory).<br>
 * QUANTITY - The number of ids that IDBroker will cache in memory.<br>
 * <p>
 * Use this class like this:
 * <pre>
 * int id = dbMap.getIDBroker().getNextIdAsInt(null, "TABLE_NAME");
 *  - or -
 * BigDecimal[] ids = ((IDBroker)dbMap.getIDBroker())
 *     .getNextIds("TABLE_NAME", numOfIdsToReturn);
 * </pre>
 *
 * NOTE: When the ID_TABLE must be updated we must ensure that
 * IDBroker objects running in different JVMs do not overwrite each
 * other.  This is accomplished using using the transactional support
 * occuring in some databases.  Using this class with a database that
 * does not support transactions should be limited to a single JVM.
 *
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class IDBroker implements Runnable, IdGenerator
{
    /**
     * Name of the ID_TABLE = ID_TABLE
     */
    public static final String ID_TABLE = "ID_TABLE";

    /**
     * Fully qualified Table_Name column name
     */
    public static final String TABLE_NAME = ID_TABLE + ".TABLE_NAME";

    /**
     * Fully qualified Next_ID column name
     */
    public static final String NEXT_ID = ID_TABLE + ".NEXT_ID";

    /**
     * Fully qualified Quantity column name
     */
    public static final String QUANTITY = ID_TABLE + ".QUANTITY";

    /** The TableMap referencing the ID_TABLE for this IDBroker. */
    private TableMap tableMap;

    /**
     * The default size of the per-table meta data <code>Hashtable</code>
     * objects.
     */
    private static final int DEFAULT_SIZE = 40;

    /**
     * The cached IDs for each table.
     *
     * Key: String table name.
     * Value: List of Integer IDs.
     */
    private Hashtable ids = new Hashtable(DEFAULT_SIZE);

    /**
     * The quantity of ids to grab for each table.
     *
     * Key: String table name.
     * Value: Integer quantity.
     */
    private Hashtable quantityStore = new Hashtable(DEFAULT_SIZE);

    /**
     * The last time this IDBroker queried the database for ids.
     *
     * Key: String table name.
     * Value: Date of last id request.
     */
    private Hashtable lastQueryTime = new Hashtable(DEFAULT_SIZE);

    /**
     * Amount of time for the thread to sleep
     */
    private static final int SLEEP_PERIOD = 1 * 60000;

    /**
     * The safety Margin
     */
    private static final float SAFETY_MARGIN = 1.2f;

    /**
     * The houseKeeperThread thread
     */
    private Thread houseKeeperThread = null;

    /**
     * Are transactions supported?
     */
    private boolean transactionsSupported = false;

    /**
     * The value of ONE!
     */
    private static final BigDecimal ONE = new BigDecimal("1");

    /** the configuration */
    private Configuration configuration;

    /** property name */
    private static final String DB_IDBROKER_CLEVERQUANTITY =
        "idbroker.clever.quantity";

    /** property name */
    private static final String DB_IDBROKER_PREFETCH =
        "idbroker.prefetch";

    /** property name */
    private static final String DB_IDBROKER_USENEWCONNECTION =
        "idbroker.usenewconnection";

    /** the log */
    private Log log = LogFactory.getLog(IDBroker.class);

    /**
     * Creates an IDBroker for the ID table.
     *
     * @param tMap A TableMap.
     */
    public IDBroker(TableMap tMap)
    {
        this.tableMap = tMap;
        configuration = Torque.getConfiguration();

        // Start the housekeeper thread only if prefetch has not been disabled
        if (configuration.getBoolean(DB_IDBROKER_PREFETCH, true))
        {
            houseKeeperThread = new Thread(this);
            // Indicate that this is a system thread. JVM will quit only when
            // there are no more active user threads. Settings threads spawned
            // internally by Torque as daemons allows commandline applications
            // using Torque terminate in an orderly manner.
            houseKeeperThread.setDaemon(true);
            houseKeeperThread.start();
        }

        // Check for Transaction support.  Give warning message if
        // IDBroker is being used with a database that does not
        // support transactions.
        String dbName = tMap.getDatabaseMap().getName();
        Connection dbCon = null;
        try
        {
            dbCon = Torque.getConnection(dbName);
            transactionsSupported = dbCon.getMetaData().supportsTransactions();
        }
        catch (Exception e)
        {
            transactionsSupported = false;
        }
        finally
        {
            try
            {
                // Return the connection to the pool.
                dbCon.close();
            }
            catch (Exception e)
            {
            }
        }
        if (!transactionsSupported)
        {
            log.warn("IDBroker is being used with db '" + dbName
                    + "', which does not support transactions. IDBroker "
                    + "attempts to use transactions to limit the possibility "
                    + "of duplicate key generation.  Without transactions, "
                    + "duplicate key generation is possible if multiple JVMs "
                    + "are used or other means are used to write to the "
                    + "database.");
        }
    }

    /**
     * Set the configuration
     *
     * @param configuration the configuration
     */
    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Returns an id as a primitive int.  Note this method does not
     * require a Connection, it just implements the KeyGenerator
     * interface.  if a Connection is needed one will be requested.
     * To force the use of the passed in connection set the configuration
     * property torque.idbroker.usenewconnection = false
     *
     * @param connection A Connection.
     * @param tableName an Object that contains additional info.
     * @return An int with the value for the id.
     * @exception Exception Database error.
     */
    public int getIdAsInt(Connection connection, Object tableName)
        throws Exception
    {
        return getIdAsBigDecimal(connection, tableName).intValue();
    }


    /**
     * Returns an id as a primitive long. Note this method does not
     * require a Connection, it just implements the KeyGenerator
     * interface.  if a Connection is needed one will be requested.
     * To force the use of the passed in connection set the configuration
     * property torque.idbroker.usenewconnection = false
     *
     * @param connection A Connection.
     * @param tableName a String that identifies a table.
     * @return A long with the value for the id.
     * @exception Exception Database error.
     */
    public long getIdAsLong(Connection connection, Object tableName)
        throws Exception
    {
        return getIdAsBigDecimal(connection, tableName).longValue();
    }

    /**
     * Returns an id as a BigDecimal. Note this method does not
     * require a Connection, it just implements the KeyGenerator
     * interface.  if a Connection is needed one will be requested.
     * To force the use of the passed in connection set the configuration
     * property torque.idbroker.usenewconnection = false
     *
     * @param connection A Connection.
     * @param tableName a String that identifies a table..
     * @return A BigDecimal id.
     * @exception Exception Database error.
     */
    public BigDecimal getIdAsBigDecimal(Connection connection,
                                        Object tableName)
        throws Exception
    {
        BigDecimal[] id = getNextIds((String) tableName, 1, connection);
        return id[0];
    }

    /**
     * Returns an id as a String. Note this method does not
     * require a Connection, it just implements the KeyGenerator
     * interface.  if a Connection is needed one will be requested.
     * To force the use of the passed in connection set the configuration
     * property torque.idbroker.usenewconnection = false
     *
     * @param connection A Connection should be null.
     * @param tableName a String that identifies a table.
     * @return A String id
     * @exception Exception Database error.
     */
    public String getIdAsString(Connection connection, Object tableName)
        throws Exception
    {
        return getIdAsBigDecimal(connection, tableName).toString();
    }


    /**
     * A flag to determine the timing of the id generation     *
     * @return a <code>boolean</code> value
     */
    public boolean isPriorToInsert()
    {
        return true;
    }

    /**
     * A flag to determine the timing of the id generation
     *
     * @return a <code>boolean</code> value
     */
    public boolean isPostInsert()
    {
        return false;
    }

    /**
     * A flag to determine whether a Connection is required to
     * generate an id.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isConnectionRequired()
    {
        return false;
    }

    /**
     * This method returns x number of ids for the given table.
     *
     * @param tableName The name of the table for which we want an id.
     * @param numOfIdsToReturn The desired number of ids.
     * @return A BigDecimal.
     * @exception Exception Database error.
     */
    public synchronized BigDecimal[] getNextIds(String tableName,
                                                int numOfIdsToReturn)
        throws Exception
    {
        return getNextIds(tableName, numOfIdsToReturn, null);
    }

    /**
     * This method returns x number of ids for the given table.
     * Note this method does not require a Connection.
     * If a Connection is needed one will be requested.
     * To force the use of the passed in connection set the configuration
     * property torque.idbroker.usenewconnection = false
     *
     * @param tableName The name of the table for which we want an id.
     * @param numOfIdsToReturn The desired number of ids.
     * @param connection A Connection.
     * @return A BigDecimal.
     * @exception Exception Database error.
     */
    public synchronized BigDecimal[] getNextIds(String tableName,
                                                int numOfIdsToReturn,
                                                Connection connection)
        throws Exception
    {
        if (tableName == null)
        {
            throw new Exception("getNextIds(): tableName == null");
        }

        // A note about the synchronization:  I (jmcnally) looked at
        // the synchronized blocks to avoid thread issues that were
        // being used in this and the storeId method.  I do not think
        // they were being effective, so I synchronized the method.
        // I have left the blocks that did exist commented in the code
        // to make it easier for others to take a look, because it
        // would be preferrable to avoid the synchronization on the
        // method

        List availableIds = (List) ids.get(tableName);

        if (availableIds == null || availableIds.size() < numOfIdsToReturn)
        {
            if (availableIds == null)
            {
                log.debug("Forced id retrieval - no available list");
            }
            else
            {
                log.debug("Forced id retrieval - " + availableIds.size());
            }
            storeIDs(tableName, true, connection);
            availableIds = (List) ids.get(tableName);
        }

        int size = availableIds.size() < numOfIdsToReturn
                ? availableIds.size() : numOfIdsToReturn;

        BigDecimal[] results = new BigDecimal[size];

        // We assume that availableIds will always come from the ids
        // Hashtable and would therefore always be the same object for
        // a specific table.
        //        synchronized (availableIds)
        //        {
        for (int i = size - 1; i >= 0; i--)
        {
            results[i] = (BigDecimal) availableIds.get(i);
            availableIds.remove(i);
        }
        //        }

        return results;
    }

    /**
     * Describe <code>exists</code> method here.
     *
     * @param tableName a <code>String</code> value that is used to identify
     * the row
     * @return a <code>boolean</code> value
     * @exception TorqueException if an error occurs
     * @exception Exception a generic exception.
     */
    public boolean exists(String tableName)
        throws TorqueException, Exception
    {
        String query = new StringBuffer(100)
            .append("select ")
            .append(TABLE_NAME)
            .append(" where ")
            .append(TABLE_NAME).append("='").append(tableName).append('\'')
            .toString();

        boolean exists = false;
        Connection dbCon = null;
        try
        {
            String databaseName = tableMap.getDatabaseMap().getName();

            dbCon = Torque.getConnection(databaseName);
            Statement statement = dbCon.createStatement();
            ResultSet rs = statement.executeQuery(query);
            exists = rs.next();
            statement.close();
        }
        finally
        {
            // Return the connection to the pool.
            try
            {
                dbCon.close();
            }
            catch (Exception e)
            {
                log.error("Release of connection failed.", e);
            }
        }
        return exists;
    }

    /**
     * A background thread that tries to ensure that when someone asks
     * for ids, that there are already some loaded and that the
     * database is not accessed.
     */
    public void run()
    {
        log.debug("IDBroker thread was started.");

        Thread thisThread = Thread.currentThread();
        while (houseKeeperThread == thisThread)
        {
            try
            {
                houseKeeperThread.sleep(SLEEP_PERIOD);
            }
            catch (InterruptedException exc)
            {
                // ignored
            }

            // logger.info("IDBroker thread checking for more keys.");
            Iterator it = ids.keySet().iterator();
            while (it.hasNext())
            {
                String tableName = (String) it.next();
                if (log.isDebugEnabled())
                {
                    log.debug("IDBroker thread checking for more keys "
                            + "on table: " + tableName);
                }
                List availableIds = (List) ids.get(tableName);
                int quantity = getQuantity(tableName, null).intValue();
                if (quantity > availableIds.size())
                {
                    try
                    {
                        // Second parameter is false because we don't
                        // want the quantity to be adjusted for thread
                        // calls.
                        storeIDs(tableName, false, null);
                        if (log.isDebugEnabled())
                        {
                            log.debug("Retrieved more ids for table: " + tableName);
                        }
                    }
                    catch (Exception exc)
                    {
                        log.error("There was a problem getting new IDs "
                                     + "for table: " + tableName, exc);
                    }
                }
            }
        }
        log.debug("IDBroker thread finished.");
    }

    /**
     * Shuts down the IDBroker thread.
     *
     * Calling this method stops the thread that was started for this
     * instance of the IDBroker. This method should be called during
     * MapBroker Service shutdown.
     */
    public void stop()
    {
        houseKeeperThread = null;
    }

    /**
     * Check the frequency of retrieving new ids from the database.
     * If the frequency is high then we increase the amount (i.e.
     * quantity column) of ids retrieved on each access.  Tries to
     * alter number of keys grabbed so that IDBroker retrieves a new
     * set of ID's prior to their being needed.
     *
     * @param tableName The name of the table for which we want an id.
     */
    private void checkTiming(String tableName)
    {
        // Check if quantity changing is switched on.
        // If prefetch is turned off, changing quantity does not make sense
        if (!configuration.getBoolean(DB_IDBROKER_CLEVERQUANTITY, true)
            || !configuration.getBoolean(DB_IDBROKER_PREFETCH, true))
        {
            return;
        }

        // Get the last id request for this table.
        java.util.Date lastTime = (java.util.Date) lastQueryTime.get(tableName);
        java.util.Date now = new java.util.Date();

        if (lastTime != null)
        {
            long thenLong = lastTime.getTime();
            long nowLong = now.getTime();
            int timeLapse = (int) (nowLong - thenLong);
            if (timeLapse < SLEEP_PERIOD && timeLapse > 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unscheduled retrieval of more ids for table: "
                            + tableName);
                }
                // Increase quantity, so that hopefully this does not
                // happen again.
                float rate = getQuantity(tableName, null).floatValue()
                    / (float) timeLapse;
                quantityStore.put(tableName, new BigDecimal(
                    Math.ceil(SLEEP_PERIOD * rate * SAFETY_MARGIN)));
            }
        }
        lastQueryTime.put(tableName, now);
    }

    /**
     * Grabs more ids from the id_table and stores it in the ids
     * Hashtable.  If adjustQuantity is set to true the amount of id's
     * retrieved for each call to storeIDs will be adjusted.
     *
     * @param tableName The name of the table for which we want an id.
     * @param adjustQuantity True if amount should be adjusted.
     * @param connection a Connection
     * @exception Exception a generic exception.
     */
    private void storeIDs(String tableName,
                          boolean adjustQuantity,
                          Connection connection)
        throws Exception
    {
        BigDecimal nextId = null;
        BigDecimal quantity = null;
        DatabaseMap dbMap = tableMap.getDatabaseMap();

        // Block on the table.  Multiple tables are allowed to ask for
        // ids simultaneously.
        //        TableMap tMap = dbMap.getTable(tableName);
        //        synchronized(tMap)  see comment in the getNextIds method
        //        {
        if (adjustQuantity)
        {
            checkTiming(tableName);
        }

        boolean useNewConnection = (connection == null) || (configuration
                .getBoolean(DB_IDBROKER_USENEWCONNECTION, true));
        try
        {
            if (useNewConnection)
            {
                connection = Transaction.beginOptional(dbMap.getName(),
                    transactionsSupported);
            }

            // Write the current value of quantity of keys to grab
            // to the database, primarily to obtain a write lock
            // on the table/row, but this value will also be used
            // as the starting value when an IDBroker is
            // instantiated.
            quantity = getQuantity(tableName, connection);
            updateQuantity(connection, tableName, quantity);

            // Read the next starting ID from the ID_TABLE.
            BigDecimal[] results = selectRow(connection, tableName);
            nextId = results[0]; // NEXT_ID column

            // Update the row based on the quantity in the
            // ID_TABLE.
            BigDecimal newNextId = nextId.add(quantity);
            updateNextId(connection, tableName, newNextId.toString());

            if (useNewConnection)
            {
                Transaction.commit(connection);
            }
        }
        catch (Exception e)
        {
            if (useNewConnection)
            {
                Transaction.rollback(connection);
            }
            throw e;
        }

        List availableIds = (List) ids.get(tableName);
        if (availableIds == null)
        {
            availableIds = new ArrayList();
            ids.put(tableName, availableIds);
        }

        // Create the ids and store them in the list of available ids.
        int numId = quantity.intValue();
        for (int i = 0; i < numId; i++)
        {
            availableIds.add(nextId);
            nextId = nextId.add(ONE);
        }
        //        }
    }

    /**
     * This method allows you to get the number of ids that are to be
     * cached in memory.  This is either stored in quantityStore or
     * read from the db. (ie the value in ID_TABLE.QUANTITY).
     *
     * Though this method returns a BigDecimal for the quantity, it is
     * unlikey the system could withstand whatever conditions would lead
     * to really needing a large quantity, it is retrieved as a BigDecimal
     * only because it is going to be added to another BigDecimal.
     *
     * @param tableName The name of the table we want to query.
     * @param connection a Connection
     * @return An int with the number of ids cached in memory.
     */
    private BigDecimal getQuantity(String tableName, Connection connection)
    {
        BigDecimal quantity = null;

        // If prefetch is turned off we simply return 1
        if (!configuration.getBoolean(DB_IDBROKER_PREFETCH, true))
        {
            quantity = new BigDecimal(1);
        }
        // Initialize quantity, if necessary.
        else if (quantityStore.containsKey(tableName))
        {
            quantity = (BigDecimal) quantityStore.get(tableName);
        }
        else
        {
            Connection dbCon = null;
            try
            {
                if (connection == null || configuration
                    .getBoolean(DB_IDBROKER_USENEWCONNECTION, true))
                {
                    String databaseName = tableMap.getDatabaseMap().getName();
                    // Get a connection to the db
                    dbCon = Torque.getConnection(databaseName);
                }

                // Read the row from the ID_TABLE.
                BigDecimal[] results = selectRow(dbCon, tableName);

                // QUANTITY column.
                quantity = results[1];
                quantityStore.put(tableName, quantity);
            }
            catch (Exception e)
            {
                quantity = new BigDecimal(10);
            }
            finally
            {
                // Return the connection to the pool.
                try
                {
                    dbCon.close();
                }
                catch (Exception e)
                {
                    log.error("Release of connection failed.", e);
                }
            }
        }
        return quantity;
    }

    /**
     * Helper method to select a row in the ID_TABLE.
     *
     * @param con A Connection.
     * @param tableName The properly escaped name of the table to
     * identify the row.
     * @return A BigDecimal[].
     * @exception Exception a generic exception.
     */
    private BigDecimal[] selectRow(Connection con, String tableName)
        throws Exception
    {
        StringBuffer stmt = new StringBuffer();
        stmt.append("SELECT " + NEXT_ID + ", " + QUANTITY)
            .append(" FROM " + ID_TABLE)
            .append(" WHERE TABLE_NAME = '")
            .append(tableName)
            .append('\'');

        Statement statement = null;

        BigDecimal[] results = new BigDecimal[2];
        try
        {
            statement = con.createStatement();
            ResultSet rs = statement.executeQuery(stmt.toString());

            if (rs.next())
            {
                // work around for MySQL which appears to support
                // getBigDecimal in the source code, but the binary
                // is throwing an NotImplemented exception.
                results[0] = new BigDecimal(rs.getString(1)); // next_id
                results[1] = new BigDecimal(rs.getString(2)); // quantity
            }
            else
            {
                throw new TorqueException("The table " + tableName
                        + " does not have a proper entry in the " + ID_TABLE);
            }
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }
        }

        return results;
    }

    /**
     * Helper method to update a row in the ID_TABLE.
     *
     * @param con A Connection.
     * @param tableName The properly escaped name of the table to identify the
     * row.
     * @param id An int with the value to set for the id.
     * @exception Exception Database error.
     */
    private void updateNextId(Connection con, String tableName, String id)
        throws Exception
    {


        StringBuffer stmt = new StringBuffer(id.length()
                                             + tableName.length() + 50);
        stmt.append("UPDATE " + ID_TABLE)
            .append(" SET NEXT_ID = ")
            .append(id)
            .append(" WHERE TABLE_NAME = '")
            .append(tableName)
            .append('\'');

        Statement statement = null;

        if (log.isDebugEnabled())
        {
            log.debug("updateNextId: " + stmt.toString());
        }

        try
        {
            statement = con.createStatement();
            statement.executeUpdate(stmt.toString());
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }
        }
    }

    /**
     * Helper method to update a row in the ID_TABLE.
     *
     * @param con A Connection.
     * @param tableName The properly escaped name of the table to identify the
     * row.
     * @param quantity An int with the value of the quantity.
     * @exception Exception Database error.
     */
    private void updateQuantity(Connection con, String tableName,
                                BigDecimal quantity)
        throws Exception
    {
        StringBuffer stmt = new StringBuffer(quantity.toString().length()
                                             + tableName.length() + 50);
        stmt.append("UPDATE " + ID_TABLE)
            .append(" SET QUANTITY = ")
            .append(quantity)
            .append(" WHERE TABLE_NAME = '")
            .append(tableName)
            .append('\'');

        Statement statement = null;

        if (log.isDebugEnabled())
        {
            log.debug("updateQuantity: " + stmt.toString());
        }

        try
        {
            statement = con.createStatement();
            statement.executeUpdate(stmt.toString());
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }
        }
    }
}
