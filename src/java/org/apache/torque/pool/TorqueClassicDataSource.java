package org.apache.torque.pool;

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

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import org.apache.commons.lang.SerializationUtils;

/**
 * Torque's default connection pool DataSource
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class TorqueClassicDataSource
    implements DataSource, Referenceable, Serializable, ObjectFactory
{
    /** Pools keyed by username. */
    private static Map pools = new HashMap();

    /** Counter used to create an internal unique name od the Data Source */
    private static int cpdsCounter;

    /** DataSource Name used to find the ConnectionPoolDataSource */
    private String dataSourceName;

    /** Description */
    private String description;

    /** Login TimeOut in seconds */
    private int loginTimeout;

    /** Pool Data Source that is used to fetch connections */
    private ConnectionPoolDataSource cpds;

    /** Log stream */
    private PrintWriter logWriter;

    /** Environment that may be used to set up a jndi initial context. */
    private Properties jndiEnvironment;

    /** Maximum Number of Connections cached in this Data Source */
    private int defaultMaxConnections;

    /**
     * Maximum Number of Connections for a specified User in this Data
     * Source
     */
    private Properties perUserMaxConnections;

    /** Maximum lifetime of a database connection */
    private int maxExpiryTime;

    /**
     * time to wait when initiating a connection
     * for the database to respond 
     */
    private int connectionWaitTimeout;

    /** Interval (in seconds) that the monitor thread reports the pool state */
    private int logInterval;

    /** Do connections from this pool are auto-committing? */
    private boolean defaultAutoCommit;
    
    /** Are connections from this pool read-only? */
    private boolean defaultReadOnly;

    /**
     * Default no-arg constructor for Serialization
     */
    public TorqueClassicDataSource()
    {
        defaultAutoCommit = true;
    }

    // Properties

    /**
     * Get the number of database connections to cache per user.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.  The default is 1.
     *
     * @return value of maxConnections.
     */
    public int getDefaultMaxConnections()
    {
        return defaultMaxConnections;
    }

    /**
     * Set the number of database connections to cache per user.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.  The default is 1.
     *
     * @param v  Value to assign to maxConnections.
     */
    public void setDefaultMaxConnections(int  v)
    {
        this.defaultMaxConnections = v;
    }


    /**
     * Get the number of database connections to cache per user.  The keys
     * are usernames and the value is the maximum connections.  Any username
     * specified here will override the value of defaultMaxConnections.
     *
     * @return value of perUserMaxConnections.
     */
    public Properties getPerUserMaxConnections()
    {
        return perUserMaxConnections;
    }

    /**
     * Set the number of database connections to cache per user.  The keys
     * are usernames and the value is the maximum connections.  Any username
     * specified here will override the value of defaultMaxConnections.
     *
     * @param v  Value to assign to perUserMaxConnections.
     */
    public void setPerUserMaxConnections(Properties  v)
    {
        this.perUserMaxConnections = v;
    }


    /**
     * Get the amount of time (in seconds) that database connections
     * will be cached.  The default is 3600 (1 hour).
     *
     * @return value of expiryTime.
     */
    public int getMaxExpiryTime()
    {
        return maxExpiryTime;
    }

    /**
     * Set the amount of time (in seconds) that database connections
     * will be cached.  The default is 3600 (1 hour).
     *
     * @param v  Value to assign to expiryTime.
     */
    public void setMaxExpiryTime(int v)
    {
        this.maxExpiryTime = v;
    }


    /**
     * Get the amount of time (in seconds) a connection request will
     * have to wait before a time out occurs and an error is thrown.
     * The default is 10 seconds.
     *
     * @return value of connectionWaitTimeout.
     */
    public int getConnectionWaitTimeout()
    {
        return connectionWaitTimeout;
    }

    /**
     * Eet the amount of time (in seconds) a connection request will
     * have to wait before a time out occurs and an error is thrown.
     * The default is 10 seconds.
     *
     * @param v  Value to assign to connectionWaitTimeout.
     */
    public void setConnectionWaitTimeout(int v)
    {
        this.connectionWaitTimeout = v;
    }


    /**
     * Get the interval (in seconds) between which the ConnectionPool logs
     * the status of it's Connections. Default is 0 which indicates no
     * logging.
     *
     * @return value of logInterval.
     */
    public int getLogInterval()
    {
        return logInterval;
    }

    /**
     * Set the interval (in seconds) between which the ConnectionPool logs
     * the status of it's Connections. Default is 0 which indicates no
     * logging.
     *
     * @param v  Value to assign to logInterval.
     */
    public void setLogInterval(int v)
    {
        this.logInterval = v;
    }

    /**
     * Get the value of defaultAutoCommit, which defines the state of
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setAutoCommit(boolean).
     * The default is true.
     *
     * @return value of defaultAutoCommit.
     */
    public boolean isDefaultAutoCommit()
    {
        return defaultAutoCommit;
    }

    /**
     * Set the value of defaultAutoCommit, which defines the state of
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setAutoCommit(boolean).
     * The default is true.
     *
     * @param v  Value to assign to defaultAutoCommit.
     */
    public void setDefaultAutoCommit(boolean v)
    {
        this.defaultAutoCommit = v;
    }

    /**
     * Get the value of defaultReadOnly, which defines the state of
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setReadOnly(boolean).
     * The default is false.
     *
     * @return value of defaultReadOnly.
     */
    public boolean isDefaultReadOnly()
    {
        return defaultReadOnly;
    }

    /**
     * Set the value of defaultReadOnly, which defines the state of
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setReadOnly(boolean).
     * The default is false.
     *
     * @param v  Value to assign to defaultReadOnly.
     */
    public void setDefaultReadOnly(boolean v)
    {
        this.defaultReadOnly = v;
    }

    /**
     * Get the name of the ConnectionPoolDataSource which backs this pool.
     * This name is used to look up the datasource from a jndi service
     * provider.
     *
     * @return value of dataSourceName.
     */
    public String getDataSourceName()
    {
        return dataSourceName;
    }

    /**
     * Set the name of the ConnectionPoolDataSource which backs this pool.
     * This name is used to look up the datasource from a jndi service
     * provider.
     *
     * @param v  Value to assign to dataSourceName.
     */
    public void setDataSourceName(String v)
    {
        if (getConnectionPoolDataSource() != null)
        {
            throw new IllegalStateException("connectionPoolDataSource property"
                + " already has a value.  Both dataSourceName and "
                + "connectionPoolDataSource properties cannot be set.");
        }

        this.dataSourceName = v;
    }


    /**
     * Get the description.  This property is defined by jdbc as for use with
     * GUI (or other) tools that might deploy the datasource.  It serves no
     * internal purpose.
     *
     * @return value of description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set the description.  This property is defined by jdbc as for use with
     * GUI (or other) tools that might deploy the datasource.  It serves no
     * internal purpose.
     *
     * @param v  Value to assign to description.
     */
    public void setDescription(String v)
    {
        this.description = v;
    }


    /**
     * Get the value of jndiEnvironment which is used when instantiating
     * a jndi InitialContext.  This InitialContext is used to locate the
     * backend ConnectionPoolDataSource.
     *
     * @param key environment key
     * @return value of jndiEnvironment.
     */
    public String getJndiEnvironment(String key)
    {
        String value = null;
        if (jndiEnvironment != null)
        {
            value = jndiEnvironment.getProperty(key);
        }
        return value;
    }

    /**
     * Set the value of jndiEnvironment which is used when instantiating
     * a jndi InitialContext.  This InitialContext is used to locate the
     * backend ConnectionPoolDataSource.
     *
     * @param key environment key
     * @param value  Value to assign to jndiEnvironment.
     */
    public void setJndiEnvironment(String key, String value)
    {
        if (jndiEnvironment == null)
        {
            jndiEnvironment = new Properties();
        }
        jndiEnvironment.setProperty(key, value);
    }


    /**
     * Get the value of connectionPoolDataSource.  This method will return
     * null, if the backing datasource is being accessed via jndi.
     *
     * @return value of connectionPoolDataSource.
     */
    public ConnectionPoolDataSource getConnectionPoolDataSource()
    {
        return cpds;
    }

    /**
     * Set the backend ConnectionPoolDataSource.  This property should not be
     * set if using jndi to access the datasource.
     *
     * @param v  Value to assign to connectionPoolDataSource.
     */
    public void
        setConnectionPoolDataSource(ConnectionPoolDataSource  v)
    {
        if (v == null)
        {
            throw new IllegalArgumentException(
                "Null argument value is not allowed.");
        }
        if (getDataSourceName() != null)
        {
            throw new IllegalStateException("dataSourceName property"
                + " already has a value.  Both dataSourceName and "
                + "connectionPoolDataSource properties cannot be set.");
        }
        this.cpds = v;

        // set the dataSourceName to a unique value
        dataSourceName = v.hashCode() + " internal cpds name " + cpdsCounter++;
    }

    /**
     * Attempt to establish a database connection.
     *
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException
    {
        return getConnection(null, null);
    }

    /**
     * Attempt to establish a database connection.
     *
     * @param username
     * @param password
     * @throws SQLException
     */
    synchronized public Connection getConnection(String username,
                                                 String password)
        throws SQLException
    {
        String key = getKey(username);
        ConnectionPool pool = (ConnectionPool) pools.get(key);
        if (pool == null)
        {
            try
            {
                registerPool(username, password);
                pool = (ConnectionPool) pools.get(key);
            }
            catch (Exception e)
            {
                throw new SQLException(e.getMessage());
            }
        }

        Connection con = pool.getConnection(username, password).getConnection();
        con.setAutoCommit(defaultAutoCommit);
        con.setReadOnly(defaultReadOnly);
        return con;
    }

    /**
     *
     * @param suffix
     * @return
     */
    private String getKey(String suffix)
    {
        String key = getDataSourceName();
        if (key == null)
        {
            throw new IllegalStateException("Attempted to use DataSource "
                + "without a backend ConnectionPoolDataSource defined.");
        }

        if (suffix != null)
        {
            key += suffix;
        }
        return key;
    }

    /**
     *
     * @param username
     * @param password
     * @throws javax.naming.NamingException
     */
    synchronized private void registerPool(String username, String password)
         throws javax.naming.NamingException
    {
        String key = getKey(username);
        if (!pools.containsKey(key))
        {
            ConnectionPoolDataSource cpds = this.cpds;
            if (cpds == null)
            {
                Context ctx = null;
                if (jndiEnvironment == null)
                {
                    ctx = new InitialContext();
                }
                else
                {
                    ctx = new InitialContext(jndiEnvironment);
                }
                cpds = (ConnectionPoolDataSource) ctx.lookup(dataSourceName);
            }

            int maxConnections = getDefaultMaxConnections();
            if (username != null)
            {
                String userMaxCon =
                    (String) getPerUserMaxConnections().get(username);
                if (userMaxCon != null)
                {
                    maxConnections = Integer.parseInt(userMaxCon);
                }
            }

            ConnectionPool pool = new ConnectionPool(cpds, username, password,
                maxConnections, 
                getMaxExpiryTime(),
                getConnectionWaitTimeout(),
                getLogInterval());

            // avoid ConcurrentModificationException
            Map newPools = new HashMap(pools);
            newPools.put(key, pool);
            pools = newPools;
        }
    }

    /**
     * Gets the maximum time in seconds that this data source can wait
     * while attempting to connect to a database.
     */
    public int getLoginTimeout()
    {
        return loginTimeout;
    }

    /**
     * Get the log writer for this data source.
     *
     * @deprecated Use correct debugging and logging code from Log4j
     */
    public PrintWriter getLogWriter()
    {
        if (logWriter == null)
        {
            logWriter = new PrintWriter(System.out);
        }
        return logWriter;
    }

    /**
     * Sets the maximum time in seconds that this data source will wait
     * while attempting to connect to a database. NOT USED.
     */
    public void setLoginTimeout(int seconds)
    {
        loginTimeout = seconds;
    }

    /**
     * Set the log writer for this data source.
     *
     * @deprecated Use correct debugging and logging code from Log4j
     */
    public void setLogWriter(java.io.PrintWriter out)
    {
        logWriter = out;
    }

    /**
     * <CODE>Referenceable</CODE> implementation.
     */
    public Reference getReference() throws NamingException
    {
        String factory = getClass().getName();

        Reference ref = new Reference(getClass().getName(), factory, null);

        ref.add(new StringRefAddr("defaultMaxConnections",
                                  String.valueOf(getDefaultMaxConnections())));
        ref.add(new StringRefAddr("maxExpiryTime",
                                  String.valueOf(getMaxExpiryTime())));
        ref.add(new StringRefAddr("connectionWaitTimeout",
                                  String.valueOf(getConnectionWaitTimeout())));
        ref.add(new StringRefAddr("logInterval",
                                  String.valueOf(getLogInterval())));
        ref.add(new StringRefAddr("dataSourceName", getDataSourceName()));
        ref.add(new StringRefAddr("description", getDescription()));

        byte[] serJndiEnv = null;
        // BinaryRefAddr does not allow null byte[].
        if (jndiEnvironment != null)
        {
            serJndiEnv = SerializationUtils.serialize(jndiEnvironment);
            ref.add(new BinaryRefAddr("jndiEnvironment", serJndiEnv));
        }

        byte[] serPUMC = null;
        // BinaryRefAddr does not allow null byte[].
        if (getPerUserMaxConnections() != null)
        {
            serPUMC = SerializationUtils.serialize(getPerUserMaxConnections());
            ref.add(new BinaryRefAddr("perUserMaxConnections", serPUMC));
        }

        return ref;
    }

    /**
     * implements ObjectFactory to create an instance of this class
     *
     * @param refObj
     * @param name
     * @param context
     * @param env
     * @return
     * @throws Exception
     */
    public Object getObjectInstance(Object refObj, Name name,
                                    Context context, Hashtable env)
        throws Exception
    {
        Reference ref = (Reference) refObj;

        if (ref.getClassName().equals(getClass().getName()))
        {
            setDefaultMaxConnections(Integer.parseInt(
                (String) ref.get("defaultMaxConnections").getContent()));
            setMaxExpiryTime(Integer.parseInt(
                (String) ref.get("maxExpiryTime").getContent()));
            setConnectionWaitTimeout(Integer.parseInt(
                (String) ref.get("connectionWaitTimeout").getContent()));
            setLogInterval(Integer.parseInt(
                (String) ref.get("logInterval").getContent()));
            setDataSourceName((String) ref.get("dataSourceName").getContent());
            setDescription((String) ref.get("description").getContent());

            RefAddr refAddr = ref.get("jndiEnvironment");
            if (refAddr != null)
            {
                byte[] serialized = (byte[]) refAddr.getContent();
                jndiEnvironment = (Properties)
                        SerializationUtils.deserialize(serialized);
            }

            refAddr = ref.get("perUserMaxConnections");
            if (refAddr != null)
            {
                byte[] serialized = (byte[]) refAddr.getContent();
                setPerUserMaxConnections(
                    (Properties) SerializationUtils.deserialize(serialized));
            }

            return this;
        }
        else
        {
            // We can't create an instance of the reference
            return null;
        }
    }
}
