package org.apache.torque.dsfactory;

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

import javax.sql.DataSource;
import javax.sql.ConnectionPoolDataSource;

import java.util.Iterator;
import org.apache.commons.configuration.Configuration;
import org.apache.torque.TorqueException;
import org.apache.commons.jdbc2pool.Jdbc2PoolDataSource;
import org.apache.commons.jdbc2pool.adapter.DriverAdapterCPDS;

/**
 * A factory that looks up the DataSource from JNDI.  It is also able
 * to deploy the DataSource based on properties found in the 
 * configuration.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public class Jdbc2PoolDataSourceFactory 
    extends AbstractDataSourceFactory
    implements DataSourceFactory
{
    private DataSource ds;

    /**
     *
     */
    public DataSource getDataSource()
    {
        return ds;
    }

    /**
     * initialize
     *
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void initialize(Configuration configuration) 
        throws TorqueException
    {
        if (configuration == null)
        {
            throw new TorqueException("Torque cannot be initialized without " +
                "a valid configuration. Please check the log files " +
                    "for further details.");
        }        
        ConnectionPoolDataSource cpds = initCPDS(configuration);
        Jdbc2PoolDataSource ds = initJdbc2Pool(configuration);
        ds.setConnectionPoolDataSource(cpds);
        this.ds = ds;
    }

    private ConnectionPoolDataSource initCPDS(Configuration configuration)
        throws TorqueException
    {
        category.debug("Starting initCPDS"); 
        ConnectionPoolDataSource cpds = new DriverAdapterCPDS();
        Configuration c = configuration.subset("connection");
        try
        {
            Iterator i = c.getKeys();
            while (i.hasNext())
            {
                String key = (String)i.next();
                category.debug("Setting datasource property: " 
                               + key);
                setProperty(key, c, cpds);
            }
        }            
        catch (Exception e)
        {
            category.error("", e);
            throw new TorqueException(e);
        }
        return cpds;
    }

    private Jdbc2PoolDataSource 
        initJdbc2Pool(Configuration configuration)
        throws TorqueException
    {
        category.debug("Starting initTorqueClassic"); 
        Jdbc2PoolDataSource ds = new Jdbc2PoolDataSource();
        Configuration c = configuration.subset("pool");
        try
        {
            Iterator i = c.getKeys();
            while (i.hasNext())
            {
                String key = (String)i.next();
                category.debug("Setting datasource property: " 
                               + key);
                setProperty(key, c, ds);
            }
        }            
        catch (Exception e)
        {
            category.error("", e);
            throw new TorqueException(e);
        }
        return ds;
    }
}
