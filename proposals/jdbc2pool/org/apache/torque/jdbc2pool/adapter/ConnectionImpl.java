package org.apache.torque.jdbc2pool.adapter;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.util.Map;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.util.EventObject;
import java.util.Vector;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.apache.torque.Torque;

/**
 * This class wraps the JDBC <code>Connection</code> class.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class ConnectionImpl 
    implements Connection
{
    /**
     * The JDBC database connection.
     */
    private Connection connection;

    /**
     * The JDBC PooledConnection (if supported by the JDBC driver). If this
     * is null, then this class uses the classic Connection object to manage
     * connection. Else, the PooledConnection object is used.
     */
     private PooledConnectionImpl pooledConnection;

    /**
     * Marks whether is Connection is still usable.
     */
    boolean isClosed;
    
    /**
     * Creates a <code>TorqueConnection</code>.
     *
     * @param pooledConnection The PooledConnection used in the close method.
     * @param connection The JDBC connection to wrap.
     */
    protected ConnectionImpl(PooledConnectionImpl pooledConnection, 
                           Connection connection)
    {
        this.pooledConnection = pooledConnection;
        this.connection = connection;
        isClosed = false;
    }


    /**
     * The finalizer helps prevent <code>ConnectionPool</code> leakage.
     */
    protected void finalize()
        throws Throwable
    {
        if (!isClosed())
        {
            // If this DBConnection object is finalized while linked
            // to a ConnectionPool, it means that it was taken from a pool
            // and not returned.  We log this fact, close the underlying
            // Connection, and return it to the ConnectionPool.
            Torque.getCategory().warn( "A TorqueConnection was finalized, "
                      + "without being returned "
                      + "to the ConnectionPool it belonged to" );
        }
    }

    // ***********************************************************************
    // java.sql.Connection implementation using wrapped Connection
    // ***********************************************************************

    public void clearWarnings() 
        throws SQLException 
    {
        try
        {
            connection.clearWarnings();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    /**
     * Marks the Connection as closed, and notifies the pool that the
     * pooled connection is available.
     *
     * @exception SQLException The database connection couldn't be closed.
     */
    public void close()
        throws SQLException
    {
        isClosed = true;
        pooledConnection.notifyListeners();
    }

    /**
     * Commit the connection.
     */
    public void commit()
        throws SQLException
    {
        try
        {
            connection.commit();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    /**
     * Create a Java SQL statement for this connection.
     *
     * @return A new <code>Statement</code>.
     */
    public Statement createStatement()
    {
        Statement stmt = null;
        try
        {
            stmt = connection.createStatement();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            Torque.getCategory().error(e);
        }
        return stmt;
    }

    public Statement createStatement(int resultSetType, 
                                     int resultSetConcurrency) 
        throws SQLException 
    {
        Statement stmt = null;
        try
        {
            stmt = connection
                .createStatement(resultSetType, resultSetConcurrency);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return stmt;
    }

    public boolean getAutoCommit() 
        throws SQLException 
    {
        boolean b = false;
        try
        {
            b= connection.getAutoCommit();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return b;
    }

    public String getCatalog() 
        throws SQLException 
    {
        String catalog = null;
        try
        {
            catalog = connection.getCatalog();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return catalog;
    }

    public DatabaseMetaData getMetaData() 
        throws SQLException 
    {
        DatabaseMetaData dmd = null;
        try
        {
            dmd = connection.getMetaData();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return dmd;
    }

    public int getTransactionIsolation() 
        throws SQLException 
    {
        int level = 0;
        try
        {
            level = connection.getTransactionIsolation();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return level;
    }

    public Map getTypeMap() 
        throws SQLException 
    {
        Map map = null;
        try
        {
            map = connection.getTypeMap();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return map;
    }

    public SQLWarning getWarnings() 
        throws SQLException 
    {
        SQLWarning warning = null;
        try
        {
            warning = connection.getWarnings();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return warning;
    }

    public boolean isClosed() 
        throws SQLException 
    {
        return isClosed;
    }

    public boolean isReadOnly() 
        throws SQLException 
    {
        boolean b = false;
        try
        {
            b = connection.isReadOnly();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return b;
    }

    public String nativeSQL(String sql) 
        throws SQLException 
    {
        String nativeSql = null;
        try
        {
            nativeSql = connection.nativeSQL(sql);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return nativeSql;
    }    

    public CallableStatement prepareCall(String sql) 
        throws SQLException 
    {
        CallableStatement stmt = null;
        try
        {
            stmt = connection.prepareCall(sql);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return stmt;
    }

    public CallableStatement prepareCall(String sql, int resultSetType, 
                                         int resultSetConcurrency) 
        throws SQLException 
    {
        CallableStatement stmt = null;
        try
        {
            stmt = connection
                .prepareCall(sql,resultSetType,resultSetConcurrency);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return stmt;
    }

    /**
     * Create a prepared Java SQL statement for this connection.
     *
     * @param sql The SQL statement to prepare.
     * @return A new <code>PreparedStatement</code>.
     */
    public PreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        PreparedStatement stmt = null;
        try
        {
            stmt = connection.prepareStatement(sql);
        }
        catch (SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return stmt;
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, 
                                              int resultSetConcurrency) 
        throws SQLException 
    {
        PreparedStatement stmt = null;
        try
        {
            stmt = connection
                .prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
        return stmt;
    }

    /**
     * Roll back the connection.
     */
    public void rollback()
        throws SQLException
    {
        try
        {
            connection.rollback();
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    /**
     * Set the autocommit flag for the connection.
     *
     * @param b True if autocommit should be set to true.
     */
    public void setAutoCommit(boolean b)
        throws SQLException
    {
        try
        {
            connection.setAutoCommit(b);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    public void setCatalog(String catalog) 
        throws SQLException 
    {
        try
        {
            connection.setCatalog(catalog);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    public void setReadOnly(boolean readOnly) 
        throws SQLException 
    {
        try
        {
            connection.setReadOnly(readOnly);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    public void setTransactionIsolation(int level) 
        throws SQLException 
    {
        try
        {
            connection.setTransactionIsolation(level);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }

    public void setTypeMap(Map map) 
        throws SQLException 
    {
        try
        {
            connection.setTypeMap(map);
        }
        catch(SQLException e)
        {
            Torque.getCategory().error(e);
            throw e;
        }
    }
}
