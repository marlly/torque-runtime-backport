package org.apache.torque.dsfactory;

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

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;

import org.apache.commons.dbcp.datasources.PerUserPoolDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * A factory that looks up the DataSource using the JDBC2 pool methods.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class PerUserPoolDataSourceFactory
    extends AbstractDataSourceFactory
    implements DataSourceFactory
{

    /** The log. */
    private static Log log 
            = LogFactory.getLog(PerUserPoolDataSourceFactory.class);

    /** The wrapped <code>DataSource</code>. */
    private DataSource ds;

    /**
     * @see org.apache.torque.dsfactory.DataSourceFactory#getDataSource
     */
    public DataSource getDataSource()
    {
        return ds;
    }

    /**
     * @see org.apache.torque.dsfactory.DataSourceFactory#initialize
     */
    public void initialize(Configuration configuration) throws TorqueException
    {
        if (configuration == null)
        {
            throw new TorqueException(
                "Torque cannot be initialized without a valid configuration. "
                + "Please check the log files for further details.");
        }

        ConnectionPoolDataSource cpds = initCPDS(configuration);
        PerUserPoolDataSource ds = initJdbc2Pool(configuration);
        ds.setConnectionPoolDataSource(cpds);
        this.ds = ds;
    }

    /**
     * Initializes the Jdbc2PoolDataSource.
     *
     * @param configuration where to read the settings from
     * @throws TorqueException if a property set fails
     * @return a configured <code>Jdbc2PoolDataSource</code>
     */
    private PerUserPoolDataSource initJdbc2Pool(Configuration configuration)
        throws TorqueException
    {
        log.debug("Starting initJdbc2Pool");
        PerUserPoolDataSource ds = new PerUserPoolDataSource();
        Configuration c = Torque.getConfiguration();

        if (c == null)
        {
            log.warn("Global Configuration not set,"
                    + " no Default pool data source configured!");
        }
        else
        {
            Configuration conf = c.subset(DEFAULT_POOL_KEY);
            applyConfiguration(conf, ds);
        }

        Configuration conf = configuration.subset(POOL_KEY);
        applyConfiguration(conf, ds);
        return ds;
    }
}