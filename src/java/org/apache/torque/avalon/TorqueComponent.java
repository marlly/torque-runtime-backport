package org.apache.torque.avalon;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;

import org.apache.commons.lang.StringUtils;

import org.apache.torque.TorqueException;
import org.apache.torque.TorqueInstance;
import org.apache.torque.adapter.DB;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.torque.map.DatabaseMap;

/**
 * Avalon component for Torque.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class TorqueComponent
        extends AbstractLogEnabled
        implements Component,
                   Configurable,
                   Initializable,
                   Contextualizable,
                   Startable,
                   ThreadSafe
{
    /** The Avalon Context */
    private Context context = null;

    /** The instance of Torque used by this component. */
    private TorqueInstance torqueInstance = null;

    /** The configuration file for Torque. */
    private String configFile = null;


    /**
     * Creates a new instance.  Default constructor used by Avalon.
     */
    public TorqueComponent()
    {
        // If we simply do a "new TorqueInstance()" here, we will get
        // into trouble when some internal classes (e.g. the DatasSource Factory)
        // simply calls Torque.<xxx> and gets a different TorqueInstance
        // than the one we configured here. Make sure that we use the
        // same object as the Facade class does.
        this.torqueInstance = org.apache.torque.Torque.getInstance();
    }

    /**
     * Creates a new instance.
     *
     * @param torqueInstance The instance of the Torque core used by
     * this component.
     */
    protected TorqueComponent(TorqueInstance torqueInstance)
    {
        this.torqueInstance = torqueInstance;
    }

    /**
     * @return A reference to our instance of the Torque core.
     */
    private TorqueInstance getTorque()
    {
        return torqueInstance;
    }

    /*
     * ========================================================================
     *
     * Avalon Component Interfaces
     *
     * ========================================================================
     */

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration configuration)
            throws ConfigurationException
    {
        getLogger().debug("configure(" + configuration + ")");

        String configFile = configuration.getChild("configfile").getValue();
        String appRoot = null;

        try
        {
            appRoot = (context == null) 
                    ? null : (String) context.get("componentAppRoot");
        }
        catch (ContextException ce)
        {
            getLogger().error("Could not load Application Root from Context");
        }

        if (StringUtils.isNotEmpty(appRoot))
        {
            if (appRoot.endsWith("/"))
            {
                appRoot = appRoot.substring(0, appRoot.length() - 1);
                getLogger().debug("Application Root changed to " + appRoot);
            }

            if (configFile.startsWith("/"))
            {
                configFile = configFile.substring(1);
                getLogger().debug("Config File changes to " + configFile);
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append(appRoot);
            sb.append('/');
            sb.append(configFile);
            
            configFile = sb.toString();
        }

        getLogger().debug("Config File is " + configFile);

        this.configFile = configFile;
    }

    /**
     * @see org.apache.avalon.framework.context.Contextualizable
     */
    public void contextualize(Context context)
            throws ContextException
    {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
            throws Exception
    {
        getLogger().debug("initialize()");
        getTorque().init(configFile);
    }

    /**
     * @see org.apache.avalon.framework.activity.Startable#start()
     */
    public void start()
    {
        getLogger().debug("start()");
    }

    /**
     * @see org.apache.avalon.framework.activity.Startable#stop()
     */
    public void stop()
    {
        getLogger().debug("stop()");
        getTorque().shutdown();
    }


    /*
     * ========================================================================
     *
     * Torque Methods, accessible from the Component
     *
     * ========================================================================
     */

    /**
     * Determine whether Torque has already been initialized.
     *
     * @return true if Torque is already initialized
     */
    public boolean isInit()
    {
        return getTorque().isInit();
    }

    /**
     * Get the configuration for this component.
     *
     * @return the Configuration
     */
    public org.apache.commons.configuration.Configuration getConfiguration()
    {
        return getTorque().getConfiguration();
    }

    /**
     * This method returns a Manager for the given name.
     *
     * @param name name of the manager
     * @return a Manager
     */
    public AbstractBaseManager getManager(String name)
    {
        return getTorque().getManager(name);
    }

    /**
     * This methods returns either the Manager from the configuration file,
     * or the default one provided by the generated code.
     *
     * @param name name of the manager
     * @param defaultClassName the class to use if name has not been configured
     * @return a Manager
     */
    public AbstractBaseManager getManager(String name,
            String defaultClassName)
    {
        return getTorque().getManager(name, defaultClassName);
    }

    /**
     * Returns the default database map information.
     *
     * @return A DatabaseMap.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DatabaseMap getDatabaseMap()
            throws TorqueException
    {
        return getTorque().getDatabaseMap();
    }

    /**
     * Returns the database map information. Name relates to the name
     * of the connection pool to associate with the map.
     *
     * @param name The name of the database corresponding to the
     *        <code>DatabaseMap</code> to retrieve.
     * @return The named <code>DatabaseMap</code>.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DatabaseMap getDatabaseMap(String name)
            throws TorqueException
    {
        return getTorque().getDatabaseMap(name);
    }

    /**
     * Register a MapBuilder
     *
     * @param className the MapBuilder
     */
    public void registerMapBuilder(String className)
    {
        getTorque().registerMapBuilder(className);
    }

    /**
     * This method returns a Connection from the default pool.
     *
     * @return The requested connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection()
            throws TorqueException
    {
        return getTorque().getConnection();
    }

    /**
     *
     * @param name The database name.
     * @return a database connection
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection(String name)
            throws TorqueException
    {
        return getTorque().getConnection(name);
    }

    /**
     * This method returns a Connecton using the given parameters.
     * You should only use this method if you need user based access to the
     * database!
     *
     * @param name The database name.
     * @param username The name of the database user.
     * @param password The password of the database user.
     * @return A Connection.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public Connection getConnection(String name, String username,
            String password)
            throws TorqueException
    {
        return getTorque().getConnection(name, username, password);
    }
    /**
     * Returns database adapter for a specific connection pool.
     *
     * @param name A pool name.
     * @return The corresponding database adapter.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public DB getDB(String name)
            throws TorqueException
    {
        return getTorque().getDB(name);
    }

    /**
     * Returns the name of the default database.
     *
     * @return name of the default DB
     */
    public String getDefaultDB()
    {
        return getTorque().getDefaultDB();
    }

    /**
     * Closes a connection.
     *
     * @param con A Connection to close.
     */
    public void closeConnection(Connection con)
    {
        getTorque().closeConnection(con);
    }
}
