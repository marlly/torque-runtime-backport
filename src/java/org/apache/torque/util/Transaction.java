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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * Refactored begin/commit/rollback transaction methods away from
 * the <code>BasePeer</code>.
 *
 * <p>
 * This can be used to handle cases where transaction support is optional.
 * The second parameter of beginOptionalTransaction will determine with a
 * transaction is used or not.
 * If a transaction is not used, the commit and rollback methods
 * do not have any effect. Instead it simply makes the logic easier to follow
 * by cutting down on the if statements based solely on whether a transaction
 * is needed or not.
 *
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @version $Id$
 */
public class Transaction
{

    /** The log. */
    private static Log log = LogFactory.getLog(Transaction.class);

    /**
     * Begin a transaction for the default database.
     * This method will fallback gracefully to
     * return a normal connection, if the database being accessed does
     * not support transactions.
     *
     * @return The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection begin() throws TorqueException
    {
        return Transaction.begin(Torque.getDefaultDB());
    }

    /**
     * Begin a transaction.  This method will fallback gracefully to
     * return a normal connection, if the database being accessed does
     * not support transactions.
     *
     * @param dbName Name of database.
     * @return The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection begin(String dbName) throws TorqueException
    {
        return Transaction.beginOptional(dbName, true);
    }

    /**
     * Begin a transaction.  This method will fallback gracefully to
     * return a normal connection, if the database being accessed does
     * not support transactions.
     *
     * @param dbName Name of database.
     * @param useTransaction If false, a transaction won't be used.
     * @return The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static Connection beginOptional(String dbName,
                                           boolean useTransaction)
        throws TorqueException
    {
        Connection con = Torque.getConnection(dbName);
        try
        {
            if (con.getMetaData().supportsTransactions() && useTransaction)
            {
                con.setAutoCommit(false);
            }
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }
        return con;
    }

    /**
     * Commit a transaction.  This method takes care of releasing the
     * connection after the commit.  In databases that do not support
     * transactions, it only returns the connection.
     *
     * @param con The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void commit(Connection con) throws TorqueException
    {
        if (con == null)
        {
            throw new NullPointerException("Connection object was null. "
                    + "This could be due to a misconfiguration of the "
                    + "DataSourceFactory. Check the logs and Torque.properties "
                    + "to better determine the cause.");
        }

        try
        {
            if (con.getMetaData().supportsTransactions()
                && con.getAutoCommit() == false)
            {
                con.commit();
                con.setAutoCommit(true);
            }
        }
        catch (SQLException e)
        {
            throw new TorqueException(e);
        }
        finally
        {
            Torque.closeConnection(con);
        }
    }

    /**
     * Roll back a transaction in databases that support transactions.
     * It also releases the connection. In databases that do not support
     * transactions, this method will log the attempt and release the
     * connection.
     *
     * @param con The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void rollback(Connection con) throws TorqueException
    {
        if (con == null)
        {
            throw new TorqueException("Connection object was null. "
                    + "This could be due to a misconfiguration of the "
                    + "DataSourceFactory. Check the logs and Torque.properties "
                    + "to better determine the cause.");
        }
        else
        {
            try
            {
                if (con.getMetaData().supportsTransactions()
                    && con.getAutoCommit() == false)
                {
                    con.rollback();
                    con.setAutoCommit(true);
                }
            }
            catch (SQLException e)
            {
                log.error("An attempt was made to rollback a transaction "
                        + "but the database did not allow the operation to be "
                        + "rolled back.", e);
                throw new TorqueException(e);
            }
            finally
            {
                Torque.closeConnection(con);
            }
        }
    }

    /**
     * Roll back a transaction without throwing errors if they occur.
     * A null Connection argument is logged at the debug level and other
     * errors are logged at warn level.
     *
     * @param con The Connection for the transaction.
     * @see Transaction#rollback(Connection)
     */
    public static void safeRollback(Connection con)
    {
        if (con == null)
        {
            log.debug("called safeRollback with null argument");
        }
        else
        {
            try
            {
                Transaction.rollback(con);
            }
            catch (TorqueException e)
            {
                log.warn("An error occured during rollback.", e);
            }
        }
    }
}
