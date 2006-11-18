package org.apache.torque.avalon;

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

import java.io.File;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.commons.lang.StringUtils;
import org.apache.torque.TorqueInstance;

/**
 * Avalon component for Torque.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public class TorqueComponent
        extends TorqueInstance
        implements Torque,
                   LogEnabled,
                   Configurable,
                   Initializable,
                   Contextualizable,
                   Startable,
                   ThreadSafe
{
    /** The Avalon Application Root */
    private String appRoot = null;

    /** The Avalon Logger */
    private Logger logger = null;

    /** The configuration file name. */
    private String configFile = null;


    /**
     * Creates a new instance.  Default constructor used by Avalon.
     */
    public TorqueComponent()
    {
        super();

        // Provide the singleton instance to the static accessor
        org.apache.torque.Torque.setInstance(this);
    }

    /*
     * ========================================================================
     *
     * Avalon Component Interfaces
     *
     * ========================================================================
     */

    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger aLogger)
    {
        this.logger = aLogger;
    }

    /**
     * Convenience method to provide the Avalon logger the way AbstractLogEnabled does.
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @see
     * org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration configuration)
            throws ConfigurationException
    {
        getLogger().debug("configure(" + configuration + ")");

        String configurationFile
                = configuration.getChild("configfile").getValue();

        if (StringUtils.isNotEmpty(appRoot))
        {
            if (configurationFile.startsWith("/"))
            {
                configurationFile = configurationFile.substring(1);
                getLogger().debug("Config File changes to "
                        + configurationFile);
            }

            StringBuffer sb = new StringBuffer();
            sb.append(appRoot);
            sb.append(File.separator);
            sb.append(configurationFile);

            configurationFile = sb.toString();
        }

        getLogger().debug("Config File is " + configurationFile);

        this.configFile = configurationFile;
    }

    /**
     * @see org.apache.avalon.framework.context.Contextualizable
     */
    public void contextualize(Context context)
            throws ContextException
    {
        // check context Merlin and YAAFI style
        try
        {
            appRoot = ((File) context.get("urn:avalon:home")).getAbsolutePath();
        }
        catch (ContextException ce)
        {
            appRoot = null;
        }

        if (appRoot == null)
        {
            // check context old ECM style, let exception flow if not available
            appRoot = (String) context.get("componentAppRoot");
        }

        if (StringUtils.isNotEmpty(appRoot))
        {
            if (appRoot.endsWith("/"))
            {
                appRoot = appRoot.substring(0, appRoot.length() - 1);
                getLogger().debug("Application Root changed to " + appRoot);
            }
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
            throws Exception
    {
        getLogger().debug("initialize()");
        init(configFile);
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
        try
        {
            shutdown();
        }
        catch (Exception e)
        {
            getLogger().error("Error while stopping Torque", e);
        }
    }
}
