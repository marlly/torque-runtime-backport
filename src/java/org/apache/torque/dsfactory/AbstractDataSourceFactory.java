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
import org.apache.log4j.Logger;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.MappedPropertyDescriptor;
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

    /**
     * The logging logger.
     */
    private static Logger logger =
        Logger.getLogger(AbstractDataSourceFactory.class);

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

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("setMappedProperty(" 
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

                if (logger.isDebugEnabled())
                {
                    logger.debug("setSimpleProperty(" 
                                   + ds + ", "
                                   + property + ", "
                                   + value 
                                   + ")");
                }
            }
        }
        catch (Exception e)
        {
            logger.error(
                "Property: "
                + property
                + " value: "
                + c.getString(key)
                + " is not supported by DataSource: "
                + ds.getClass().getName());
            throw e;
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
        logger.debug("applyConfiguration(" + c + ", " + o + ")");
        
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
                logger.error(e);
                throw new TorqueException(e);
            }
        }
    }
}
