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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.TorqueException;

/**
 * A factory that looks up the DataSource from JNDI.  It is also able
 * to deploy the DataSource based on properties found in the
 * configuration.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public class JndiDataSourceFactory
    extends AbstractDataSourceFactory
    implements DataSourceFactory
{

    /** The log. */
    private static Log log = LogFactory.getLog(JndiDataSourceFactory.class);

    /** The path to get the resource from. */
    private String path;
    /** The context to get the resource from. */
    private Context ctx;

    /**
     * @see org.apache.torque.dsfactory.DataSourceFactory#getDataSource
     */
    public DataSource getDataSource() throws TorqueException
    {
        DataSource ds = null;
        try
        {
            ds = ((DataSource) ctx.lookup(path));
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
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
        initJNDI(configuration);
        initDataSource(configuration);
    }

    /**
     * Initializes JNDI.
     *
     * @param configuration where to read the settings from
     * @throws TorqueException if a property set fails
     */
    private void initJNDI(Configuration configuration) throws TorqueException
    {
        log.debug("Starting initJNDI");
        Hashtable env = null;
        Configuration c = configuration.subset("jndi");
        if (c == null)
        {
            throw new TorqueException(
                "JndiDataSourceFactory requires a jndi "
                    + "path property to lookup the DataSource in JNDI.");
        }
        try
        {
            Iterator i = c.getKeys();
            while (i.hasNext())
            {
                String key = (String) i.next();
                if (key.equals("path"))
                {
                    path = c.getString(key);
                    log.debug("JNDI path: " + path);
                }
                else
                {
                    if (env == null)
                    {
                        env = new Hashtable();
                    }
                    String value = c.getString(key);
                    env.put(key, value);
                    log.debug("Set jndi property: " + key + "=" + value);
                }
            }
            if (env == null)
            {
                ctx = new InitialContext();
            }
            else
            {
                ctx = new InitialContext(env);
            }
            log.debug("Created new InitialContext");
            debugCtx(ctx);
        }
        catch (Exception e)
        {
            log.error("", e);
            throw new TorqueException(e);
        }
    }

    /**
     * Initializes the DataSource.
     *
     * @param configuration where to read the settings from
     * @throws TorqueException if a property set fails
     */
    private void initDataSource(Configuration configuration)
        throws TorqueException
    {
        log.debug("Starting initDataSources");
        Configuration c = configuration.subset("datasource");
        try
        {
            if (c != null)
            {
                Object ds = null;
                Iterator i = c.getKeys();
                while (i.hasNext())
                {
                    String key = (String) i.next();
                    if (key.equals("classname"))
                    {
                        String classname = c.getString(key);
                        log.debug("Datasource class: " + classname);

                        Class dsClass = Class.forName(classname);
                        ds = dsClass.newInstance();
                    }
                    else
                    {
                        log.debug("Setting datasource property: " + key);
                        setProperty(key, c, ds);
                    }
                }

                bindDStoJndi(ctx, path, ds);
            }
        }
        catch (Exception e)
        {
            log.error("", e);
            throw new TorqueException(e);
        }
    }

    /**
     *
     * @param ctx the context
     * @throws NamingException
     */
    private void debugCtx(Context ctx) throws NamingException
    {
        log.debug("InitialContext -------------------------------");
        Map env = ctx.getEnvironment();
        Iterator qw = env.keySet().iterator();
        log.debug("Environment properties:" + env.size());
        while (qw.hasNext())
        {
            Object prop = qw.next();
            log.debug("    " + prop + ": " + env.get(prop));
        }
        log.debug("----------------------------------------------");
    }

    /**
     *
     * @param ctx
     * @param path
     * @param ds
     * @throws Exception
     */
    private void bindDStoJndi(Context ctx, String path, Object ds)
        throws Exception
    {
        debugCtx(ctx);

        // add subcontexts, if not added already
        int start = path.indexOf(':') + 1;
        if (start > 0)
        {
            path = path.substring(start);
        }
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens())
        {
            String subctx = st.nextToken();
            if (st.hasMoreTokens())
            {
                try
                {
                    ctx.createSubcontext(subctx);
                    log.debug("Added sub context: " + subctx);
                }
                catch (NameAlreadyBoundException nabe)
                {
                    // ignore
                }
                catch (NamingException ne)
                {
                    // even though there is a specific exception
                    // for this condition, some implementations
                    // throw the more general one.
                    /*
                      if (ne.getMessage().indexOf("already bound") == -1 )
                      {
                      throw ne;
                      }
                    */
                    // ignore
                }
                ctx = (Context) ctx.lookup(subctx);
            }
            else
            {
                // not really a subctx, it is the ds name
                ctx.bind(subctx, ds);
            }
        }
    }
}
