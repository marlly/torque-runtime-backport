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

import java.util.Iterator;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;
import org.apache.log4j.Category;
import org.apache.torque.TorqueException;
import org.apache.torque.pool.TorqueClassicDataSource;

/**
 * A factory that looks up the DataSource from JNDI.  It is also able
 * to deploy the DataSource based on properties found in the
 * configuration.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public class TorqueDataSourceFactory
    extends AbstractDataSourceFactory
    implements DataSourceFactory
{

    /** The log. */
    private static Category category =
        Category.getInstance(TorqueDataSourceFactory.class);

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
                "Torque cannot be initialized without "
                    + "a valid configuration. Please check the log files "
                    + "for further details.");
        }
        ConnectionPoolDataSource cpds = initCPDS(configuration);
        TorqueClassicDataSource tcds = initTorqueClassic(configuration);
        tcds.setConnectionPoolDataSource(cpds);
        ds = tcds;
    }

    /**
     * Initializes the ConnectionPoolDataSource.
     *
     * @param configuration where to read the settings from
     * @throws TorqueException if a property set fails
     * @return a configured <code>ConnectionPoolDataSource</code>
     */
    protected ConnectionPoolDataSource initCPDS(Configuration configuration)
        throws TorqueException
    {
        category.debug("Starting initCPDS");
        ConnectionPoolDataSource cpds = new DriverAdapterCPDS();
        Configuration c = null;

        c = configuration.subset("connection");
        applyConfiguration(c, cpds);
        return cpds;
    }

    /**
     * Initializes the TorqueClassicDataSource.
     *
     * @param configuration where to read the settings from
     * @throws TorqueException if a property set fails
     * @return a configured <code>TorqueClassicDataSource</code>
     */
    protected TorqueClassicDataSource initTorqueClassic(
        Configuration configuration)
        throws TorqueException
    {
        category.debug("Starting initTorqueClassic");
        TorqueClassicDataSource ds = new TorqueClassicDataSource();

        Configuration c = null;

        c = configuration.subset("pool");
        applyConfiguration(c, ds);
        return ds;
    }


    /**
     * Iterate over a Configuration subset and apply all
     * properties to a passed object which must contain Bean
     * setter and getter
     *
     * @param c The configuration subset
     * @param o The object to apply the properties to
     * @throws TorqueException if a property set fails
     */
    private void applyConfiguration(Configuration c, Object o)
        throws TorqueException
    {
        category.debug("applyConfiguration(" + c + ", " + o + ")");
        
        if(c != null)
        {
            try
            {
                for (Iterator i = c.getKeys(); i.hasNext();)
                {
                    String key = (String) i.next();
                    setProperty(key, c, o);
                }
            }
            catch (Exception e)
            {
                category.error(e);
                throw new TorqueException(e);
            }
        }
    }
}
