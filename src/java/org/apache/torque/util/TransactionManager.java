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

import org.apache.torque.TorqueException;

/**
 * Torque's interface to the transaction management system.
 *
 * @version $Id: TransactionManager.java 1448414 2013-02-20 21:06:35Z tfischer $
 */
public interface TransactionManager
{
    /**
     * Begin a transaction by retrieving a connection from the default database
     * connection pool.
     * WARNING: If the database does not support transaction or the pool has set
     * autocommit to true on the connection, the database will commit after
     * every statement, regardless of when a commit or rollback is issued.
     *
     * @return The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    Connection begin() throws TorqueException;

    /**
     * Begin a transaction by retrieving a connection from the named database
     * connection pool.
     * WARNING: If the database does not support transaction or the pool has set
     * autocommit to true on the connection, the database will commit after
     * every statement, regardless of when a commit or rollback is issued.
     *
     * @param dbName Name of database.
     *
     * @return The Connection for the transaction.
     *
     * @throws TorqueException If the connection cannot be retrieved.
     */
    Connection begin(String dbName) throws TorqueException;


    /**
     * Commit a transaction and close the connection.
     * If the connection is in autocommit mode or the database does not support
     * transactions, only a connection close is performed
     *
     * @param con The Connection for the transaction.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    void commit(Connection con) throws TorqueException;

    /**
     * Roll back a transaction and release the connection.
     * In databases that do not support transactions or if autocommit is true,
     * no rollback will be performed, but the connection will be closed anyway.
     *
     * @param con The Connection for the transaction.
     *
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    void rollback(Connection con) throws TorqueException;

    /**
     * Roll back a transaction without throwing errors if they occur.
     * A null Connection argument is logged at the debug level and other
     * errors are logged at warn level.
     *
     * @param con The Connection for the transaction.
     * @see TransactionManagerImpl#rollback(Connection)
     */
    void safeRollback(Connection con);
}
