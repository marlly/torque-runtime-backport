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
import org.apache.log4j.Category;
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
    private static Category category =
        Category.getInstance(JndiDataSourceFactory.class);

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
        category.debug("Starting initJNDI");
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
                    category.debug("JNDI path: " + path);
                }
                else
                {
                    if (env == null)
                    {
                        env = new Hashtable();
                    }
                    String value = c.getString(key);
                    env.put(key, value);
                    category.debug("Set jndi property: " + key + "=" + value);
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
            category.debug("Created new InitialContext");
            debugCtx(ctx);
        }
        catch (Exception e)
        {
            category.error("", e);
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
        category.debug("Starting initDataSources");
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
                        category.debug("Datasource class: " + classname);

                        Class dsClass = Class.forName(classname);
                        ds = dsClass.newInstance();
                    }
                    else
                    {
                        category.debug("Setting datasource property: " + key);
                        setProperty(key, c, ds);
                    }
                }

                bindDStoJndi(ctx, path, ds);
            }
        }
        catch (Exception e)
        {
            category.error("", e);
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
        category.debug("InitialContext -------------------------------");
        Map env = ctx.getEnvironment();
        Iterator qw = env.keySet().iterator();
        category.debug("Environment properties:" + env.size());
        while (qw.hasNext())
        {
            Object prop = qw.next();
            category.debug("    " + prop + ": " + env.get(prop));
        }
        category.debug("----------------------------------------------");
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
                    category.debug("Added sub context: " + subctx);
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
