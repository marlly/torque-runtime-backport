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

import java.util.Iterator;

import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.MappedPropertyDescriptor;
import org.apache.commons.beanutils.PropertyUtils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * A class that contains common functionality of the factories in this
 * package.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public abstract class AbstractDataSourceFactory
{
    /** "pool" Key for the configuration */
    public static final String POOL_KEY = "pool";

    /** "connection" Key for the configuration */
    public static final String CONNECTION_KEY = "connection";

    /** "default.pool" Key for the configuration */
    public static final String DEFAULT_POOL_KEY = "defaults.pool";

    /** "default.connection" Key for the configuration */
    public static final String DEFAULT_CONNECTION_KEY = "defaults.connection";

    /** The log */
    private static Log log = LogFactory.getLog(AbstractDataSourceFactory.class);

    /**
     * Encapsulates setting configuration properties on
     * <code>DataSource</code> objects.
     *
     * @param property the property to read from the configuration
     * @param c the configuration to read the property from
     * @param ds the <code>DataSource</code> instance to write the property to
     * @throws Exception if anything goes wrong
     */
    protected void setProperty(String property, Configuration c, Object ds)
        throws Exception
    {
        String key = property;
        Class dsClass = ds.getClass();
        int dot = property.indexOf('.');
        try
        {
            if (dot > 0)
            {
                property = property.substring(0, dot);

                MappedPropertyDescriptor mappedPD =
                    new MappedPropertyDescriptor(property, dsClass);
                Class propertyType = mappedPD.getMappedPropertyType();
                Configuration subProps = c.subset(property);
                // use reflection to set properties
                Iterator j = subProps.getKeys();
                while (j.hasNext())
                {
                    String subProp = (String) j.next();
                    String propVal = subProps.getString(subProp);
                    Object value = ConvertUtils.convert(propVal, propertyType);
                    PropertyUtils
                        .setMappedProperty(ds, property, subProp, value);

                    if (log.isDebugEnabled())
                    {
                        log.debug("setMappedProperty("
                                       + ds + ", "
                                       + property + ", "
                                       + subProp + ", "
                                       + value
                                       + ")");
                    }
                }
            }
            else
            {
                Class propertyType =
                    PropertyUtils.getPropertyType(ds, property);
                Object value =
                    ConvertUtils.convert(c.getString(property), propertyType);
                PropertyUtils.setSimpleProperty(ds, property, value);

                if (log.isDebugEnabled())
                {
                    log.debug("setSimpleProperty("
                                   + ds + ", "
                                   + property + ", "
                                   + value
                                   + ")");
                }
            }
        }
        catch (Exception e)
        {
            log.error(
                "Property: "
                + property
                + " value: "
                + c.getString(key)
                + " is not supported by DataSource: "
                + ds.getClass().getName());
        }
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
    protected void applyConfiguration(Configuration c, Object o)
        throws TorqueException
    {
        log.debug("applyConfiguration(" + c + ", " + o + ")");

        if (c != null)
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
                log.error(e);
                throw new TorqueException(e);
            }
        }
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
        log.debug("Starting initCPDS");
        ConnectionPoolDataSource cpds = new DriverAdapterCPDS();
        Configuration c = Torque.getConfiguration();

        if (c == null)
        {
            log.warn("Global Configuration not set,"
                    + " no Default connection pool data source configured!");
        }
        else
        {
            Configuration conf = c.subset(DEFAULT_CONNECTION_KEY);
            applyConfiguration(conf, cpds);
        }
            
        Configuration conf = configuration.subset(CONNECTION_KEY);
        applyConfiguration(conf, cpds);
        
        return cpds;
    }
}
