package org.apache.torque.dsfactory;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
