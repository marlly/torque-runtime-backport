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

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import org.apache.commons.lang.StringUtils;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * Avalon component for Torque.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class TorqueComponent
        extends AbstractLogEnabled
        implements Configurable, Initializable, Disposable, Contextualizable
{

    /** The Avalon Context */
    private Context context = null;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration configuration) throws ConfigurationException
    {
        getLogger().debug("configure(" + configuration + ")");

        String configFile = configuration.getChild("configfile").getValue();
        String applicationRoot = null;

        try
        {
            applicationRoot = (context == null) ? null : (String) context.get("componentAppRoot");
        }
        catch (ContextException ce)
        {
            getLogger().error("Could not load Application Root from Context");
        }

        if (StringUtils.isNotEmpty(applicationRoot))
        {
            if (applicationRoot.endsWith("/"))
            {
                applicationRoot = applicationRoot.substring(0, applicationRoot.length() - 1);
                getLogger().debug("Application Root changed to " + applicationRoot);
            }

            if (configFile.startsWith("/"))
            {
                configFile = configFile.substring(1);
                getLogger().debug("Config File changes to " + configFile);
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append(applicationRoot);
            sb.append('/');
            sb.append(configFile);
            
            configFile = sb.toString();
        }

        getLogger().debug("Config File is " + configFile );

        try 
        {
            Torque.init(configFile);
        }
        catch (TorqueException tex)
        {
            throw new ConfigurationException("Torque could not be configured", tex);
        }
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
    public void initialize() throws Exception
    {
        getLogger().debug("initialize()");

        Torque torque = new Torque();
        torque.initialize();        
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose()
    {
        getLogger().debug("dispose()");

        Torque torque = new Torque();
        torque.dispose();        
    }
}
