package org.apache.torque.jdbc2pool;

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
 
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.Hashtable;
import  java.io.PrintWriter;
import  java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Referenceable;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

/**
 * Torque's default connection pool DataSource
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class TorqueDataSource
    implements DataSource, Referenceable, Serializable, ObjectFactory
{
    /** DataSource Name */
    private String dataSourceName;
    /** Description */
    private String description;

    /** Login TimeOut in seconds */
    private int loginTimeout;
    /** Log stream */
    private PrintWriter logWriter;


    private Map pools; 

    /**
     * Default no-arg constructor for Serialization
     */
    public TorqueDataSource() 
    {
        pools = new HashMap();
    }

    // Properties
    
    /**
     * Get the value of dataSourceName.
     * @return value of dataSourceName.
     */
    public String getDataSourceName() 
    {
        return dataSourceName;
    }
    
    /**
     * Set the value of dataSourceName.
     * @param v  Value to assign to dataSourceName.
     */
    public void setDataSourceName(String  v) 
    {
        this.dataSourceName = v;
    }
    
    
    /**
     * Get the value of description.
     * @return value of description.
     */
    public String getDescription() 
    {
        return description;
    }
    
    /**
     * Set the value of description.
     * @param v  Value to assign to description.
     */
    public void setDescription(String  v) 
    {
        this.description = v;
    }
    


    /**
     * Attempt to establish a database connection.
     */
    public Connection getConnection() 
        throws SQLException
    {
        return getConnection(null, null);
    }
                     
    /**
     * Attempt to establish a database connection.
     */
    synchronized public Connection getConnection(String username, 
                                                 String password)
        throws SQLException
    {
        String key = getDataSourceName() + username;
        ConnectionPool pool = (ConnectionPool)pools.get(key);
        if ( pool == null ) 
        {
            try
            {
                registerPool(username, password);
            }
            catch (Exception e)
            {
                //ignore for now, should wrap in SQLException, !FIXME!
            }
        }
        
        return pool.getConnection(username, password).getConnection();
    }

    private void registerPool(String username, String password)
         throws javax.naming.NamingException
    {
            Context ctx = new InitialContext();
            ConnectionPoolDataSource cpds = 
                (ConnectionPoolDataSource)ctx.lookup(getDataSourceName());
            ConnectionPool pool = new ConnectionPool(cpds, username, password);
            pools.put(username, pool);
    }
    
    /**
     * Gets the maximum time in seconds that this data source can wait 
     * while attempting to connect to a database..
     */
    public int getLoginTimeout() 
    {
        return loginTimeout;
    }
                           
    /**
     * Get the log writer for this data source.
     */
    public PrintWriter getLogWriter() 
    {
        return logWriter;
    }
                           
    /**
     * Sets the maximum time in seconds that this data source will wait 
     * while attempting to connect to a database.
     */
    public void setLoginTimeout(int seconds)
    {
        loginTimeout = seconds;
    } 
                           
    /**
     * Set the log writer for this data source. 
     */
    public void setLogWriter(java.io.PrintWriter out)
    {
        logWriter = out;
    } 

    /**
     * <CODE>Referenceable</CODE> implementation.
     */
    public Reference getReference() 
        throws NamingException 
    {
        String factory = getClass().getName();
        
        Reference ref = new Reference(getClass().getName(), factory, null);

        ref.add(new StringRefAddr("dataSourceName", getDataSourceName()));
        ref.add(new StringRefAddr("description", getDescription()));

        return ref;
    }


    /**
     * implements ObjectFactory to create an instance of this class
     */ 
    public Object getObjectInstance(Object refObj, Name name, 
                                    Context context, Hashtable env) 
        throws Exception 
    {
        Reference ref = (Reference)refObj;
	
        if (ref.getClassName().equals(getClass().getName())) 
        {   
            setDataSourceName((String)ref.get("dataSourceName").getContent());
            setDescription((String)ref.get("description").getContent());
            return this;
        }
        else 
        { // We can't create an instance of the reference
            return null;
        }
    }




    // Unused properties

    /** Database Name */
    private String databaseName;
    /** Network Protocol */
    private String networkProtocol;
    /** Password */
    private String password;
    /** Port number */
    private int portNumber;
    /** Server Name */
    private String serverName;
    /** User name */
    private String user;

    /**
     * Get the value of databaseName.
     * @return value of databaseName.
     */
    public String getDatabaseName() 
    {
        return databaseName;
    }
    
    /**
     * Set the value of databaseName.
     * @param v  Value to assign to databaseName.
     */
    public void setDatabaseName(String  v) 
    {
        this.databaseName = v;
    }
    
    /**
     * Get the value of networkProtocol.
     * @return value of networkProtocol.
     */
    public String getNetworkProtocol() 
    {
        return networkProtocol;
    }
    
    /**
     * Set the value of networkProtocol.
     * @param v  Value to assign to networkProtocol.
     */
    public void setNetworkProtocol(String  v) 
    {
        this.networkProtocol = v;
    }
       /**
        * Get the value of password.
        * @return value of password.
        */
    public String getPassword() 
    {
        return password;
    }
    
    /**
     * Set the value of password.
     * @param v  Value to assign to password.
     */
    public void setPassword(String  v) 
    {
        this.password = v;
    }
    
    /**
     * Get the value of portNumber.
     * @return value of portNumber.
     */
    public int getPortNumber() 
    {
        return portNumber;
    }
    
    /**
     * Set the value of portNumber.
     * @param v  Value to assign to portNumber.
     */
    public void setPortNumber(int  v) 
    {
        this.portNumber = v;
    }

    /**
     * Get the value of serverName.
     * @return value of serverName.
     */
    public String getServerName() 
    {
        return serverName;
    }
    
    /**
     * Set the value of serverName.
     * @param v  Value to assign to serverName.
     */
    public void setServerName(String  v) 
    {
        this.serverName = v;
    }

    /**
     * Get the value of user.
     * @return value of user.
     */
    public String getUser() 
    {
        return user;
    }
    
    /**
     * Set the value of user.
     * @param v  Value to assign to user.
     */
    public void setUser(String  v) 
    {
        this.user = v;
    }
}
