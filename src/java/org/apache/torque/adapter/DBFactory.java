package org.apache.torque.adapter;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.util.StringUtils;

import org.apache.log4j.Category;

/**
 * This class creates different DB objects based on the database
 * driver that is provided.
 *
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:bmclaugh@algx.net">Brett McLaughlin</a>
 * @author <a href="mailto:ralf@reswi.ruhr.de">Ralf Stranzenbach</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id$
 */
public class DBFactory
{
    /**
     * List of registered drivers.
     */
    private static Hashtable drivers = null;

    private static ExtendedProperties configuration;

    private static Category category;

    public static void setConfiguration(ExtendedProperties c)
    {
        configuration = c;
    }

    public static void setCategory(Category c)
    {
        category = c;
    }

    // This static code creates the list of possible drivers and adds
    // the "NO DATABASE" adapter to this list.  After all the
    // configuration is queried to get a list of JDBC drivers and
    // their associated adapters.
    public static void init()
    {
        drivers = new Hashtable();

        // Add the null db driver.
        registerDriver("", DBNone.class);

        Enumeration adapters =
            configuration.getVector("database.adapter").elements();
        if (!adapters.hasMoreElements())
        {
            // Unfortunately, once upon a time this property was
            // spelled incorrectly.
            adapters = configuration.getVector("database.adaptor").elements();
        }

        while (adapters.hasMoreElements())
        {
            String adapter = (String) adapters.nextElement();
            String driver =
                configuration.getString("database.adapter." + adapter);
            if (!StringUtils.isValid(driver))
            {
                // Also previously spelled incorrectly.
                driver =
                    configuration.getString("database.adaptor." + adapter);
            }

            Class c = null;
            try
            {
                //!! bad bad bad bad hardcoding!!!
                // This needs to be configurable, or set
                // in a hidden property file.
                c = Class.forName(
                    "org.apache.torque.adapter." + adapter);
            }
            catch (ClassNotFoundException ignored)
            {
                // Try adapter name as the fully qualified class name.
                try
                {
                    c = Class.forName(adapter);
                }
                catch (ClassNotFoundException e)
                {
                    category.error(e);
                }
            }

            if (c != null && driver != null)
            {
                registerDriver(driver, c);
            }
        }
    }

    /**
     * Try to register the class of a database driver at the factory.
     * This concept allows for dynamically adding new database drivers
     * using the configuration files instead of changing the codebase.
     *
     * @param driver The fully-qualified name of the JDBC driver to create.
     * @param dc     The named JDBC driver.
     */
    private static void registerDriver(String driver, Class dc)
    {
        if (!drivers.containsKey(driver))
        {
            // Add this new driver class to the list of known drivers.
            drivers.put(driver, dc);
        }
    }

    /**
     * Creates an instance of the Turbine database adapter associated with the
     * specified JDBC driver.
     *
     * NOTE: This method used to be <code>protected</code>.  I'd like to try
     * to get it back that way ASAP.  I had to change its access level since
     * it is called by <code>ConnectionPool</code>, and these two
     * classes are no longer in the same package.  -Daniel <dlr@collab.net>
     *
     * @param driver The fully-qualified name of the JDBC driver to create.
     * @return       An instance of a Turbine database adapter.
     */
    public static DB create(String driver)
        throws InstantiationException
    {
        Class dc = (Class) drivers.get(driver);

        if (dc != null)
        {
            try
            {
                // Create an instantiation of the driver.
                DB retVal = (DB)dc.newInstance();
                retVal.setJDBCDriver(driver);
                return retVal;
            }
            catch (IllegalAccessException e)
            {
                throw new InstantiationException(
                    "Driver " + driver + " not instantiated.");
            }
        }
        else
        {
            throw new InstantiationException(
                "Database type " + driver + " not implemented.");
        }
    }
}
