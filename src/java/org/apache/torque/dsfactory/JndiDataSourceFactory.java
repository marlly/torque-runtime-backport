package org.apache.torque.dsfactory;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * This factory tries to avoid excessive context lookups to improve speed.
 * The time between two lookups can be configured. The default is 0 (no cache).
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:thomas@vandahl.org">Thomas Vandahl</a>
 * @version $Id$
 */
public class JndiDataSourceFactory
    extends AbstractDataSourceFactory
{

    /** The log. */
    private static Log log = LogFactory.getLog(JndiDataSourceFactory.class);

    /** The path to get the resource from. */
    private String path;
    /** The context to get the resource from. */
    private Context ctx;

    /** A locally cached copy of the DataSource */
    private DataSource ds = null;

    /** Time of last actual lookup action */
    private long lastLookup = 0;

    /** Time between two lookups */
    private long ttl = 0; // ms

    /**
     * @see org.apache.torque.dsfactory.DataSourceFactory#getDataSource
     */
    public DataSource getDataSource() throws TorqueException
    {
        long time = System.currentTimeMillis();

        if (ds == null || time - lastLookup > ttl)
        {
            try
            {
                ds = ((DataSource) ctx.lookup(path));
                lastLookup = time;
            }
            catch (Exception e)
            {
                throw new TorqueException(e);
            }
        }

        return ds;
    }

    /**
     * @see org.apache.torque.dsfactory.DataSourceFactory#initialize
     */
    public void initialize(Configuration configuration) throws TorqueException
    {
        super.initialize(configuration);

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

        Configuration c = configuration.subset("jndi");
        if (c == null || c.isEmpty())
        {
            throw new TorqueException(
                "JndiDataSourceFactory requires a jndi "
                    + "path property to lookup the DataSource in JNDI.");
        }

        try
        {
            Hashtable env = new Hashtable();
            for (Iterator i = c.getKeys(); i.hasNext(); )
            {
                String key = (String) i.next();
                if (key.equals("path"))
                {
                    path = c.getString(key);
                    if (log.isDebugEnabled())
                    {
                        log.debug("JNDI path: " + path);
                    }
                }
                else if (key.equals("ttl"))
                {
                    ttl = c.getLong(key, ttl);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Time between context lookups: " + ttl);
                    }
                }
                else
                {
                    String value = c.getString(key);
                    env.put(key, value);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Set jndi property: " + key + "=" + value);
                    }
                }
            }

            ctx = new InitialContext(env);
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
        log.debug("Starting initDataSource");
        try
        {
            Object ds = null;

            Configuration c = configuration.subset("datasource");
            if (c != null)
            {
                for (Iterator i = c.getKeys(); i.hasNext(); )
                {
                    String key = (String) i.next();
                    if (key.equals("classname"))
                    {
                        String classname = c.getString(key);
                        if (log.isDebugEnabled())
                        {
                            log.debug("Datasource class: " + classname);
                        }

                        Class dsClass = Class.forName(classname);
                        ds = dsClass.newInstance();
                    }
                    else
                    {
                        if (ds != null)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Setting datasource property: " + key);
                            }
                            setProperty(key, c, ds);
                        }
                        else
                        {
                            log.error("Tried to set property " + key + " without Datasource definition!");
                        }
                    }
                }
            }

            if (ds != null)
            {
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
     * Does nothing. We do not want to close a dataSource retrieved from Jndi,
     * because other applications might use it as well.
     */
    public void close()
    {
        // do nothing
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
        Iterator qw = env.entrySet().iterator();
        log.debug("Environment properties:" + env.size());
        while (qw.hasNext())
        {
            Map.Entry entry = (Map.Entry)qw.next(); 
            log.debug("    " + entry.getKey() + ": " + entry.getValue());
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
                     *                      if (ne.getMessage().indexOf("already bound") == -1 )
                     *                      {
                     *                      throw ne;
                     *                      }
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
