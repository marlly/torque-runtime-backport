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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Category;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * Refactored begin/commit/rollback transaction methods away from
 * the <code>BasePeer</code>.
 *
 * <p>
 * This can be used to handle cases where transaction support is optional.
 * The second parameter of beginOptionalTransaction will determine with a transaction
 * is used or not. If a transaction is not used, the commit and rollback methods
 * do not have any effect. Instead it simply makes the logic easier to follow
 * by cutting down on the if statements based solely on whether a transaction
 * is needed or not.
 *
 *
 * @author <a href="mailto:stephenh@chase3000.com">Stephen Haberman</a>
 * @version $Id$
 */
public class Transaction
{

    /** The log. */
    private static Category category = Category.getInstance(Transaction.class);

    /**
     * Begin a transaction.  This method will fallback gracefully to
     * return a normal connection, if the database being accessed does
     * not support transactions.
     *
     * @param dbName Name of database.
     * @return The Connection for the transaction.
     * @throws TorqueException
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
     * @throws TorqueException
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
     * @throws TorqueException
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
     * @throws TorqueException
     */
    public static void rollback(Connection con) throws TorqueException
    {
        if (con == null)
        {
            category.error("Connection object was null. "
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
                category.error("An attempt was made to rollback a transaction "
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
     *
     * @param con The Connection for the transaction.
     * @see safeRollback
     */
    public static void safeRollback(Connection con)
    {
        try
        {
            Transaction.rollback(con);
        }
        catch (TorqueException e)
        {
            category.error("An error occured during rollback.", e);
        }
    }
}
