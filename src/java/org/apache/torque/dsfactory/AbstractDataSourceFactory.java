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

import java.io.IOException;
import java.sql.Connection;
import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyEditor;
import java.lang.reflect.Method;
import java.io.File;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.torque.adapter.DB;
import org.apache.torque.adapter.DBFactory;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.oid.IDGeneratorFactory;
import org.apache.torque.oid.IDBroker;
import org.apache.torque.util.BasePeer;
import org.apache.torque.manager.AbstractBaseManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.stratum.lifecycle.Configurable;
import org.apache.stratum.lifecycle.Initializable;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.MappedPropertyDescriptor;

/**
 * A class that contains common functionality of the factories in this
 * package.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
abstract class AbstractDataSourceFactory
{
    /**
     * The logging category.
     */
    protected static Category category =
        Category.getInstance("org.apache.torque");

    protected void setProperty(String property, Configuration c, 
                                    Object ds)
        throws Exception
    {
        String key = property;
        Class dsClass = ds.getClass();
        int dot = property.indexOf('.');
        try
        {
            if ( dot > 0 )
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
                    String subProp = (String)j.next();
                    String propVal = subProps.getString(subProp);
                    Object value = ConvertUtils.convert(propVal, propertyType);
                    PropertyUtils
                        .setMappedProperty(ds, property, subProp, value);
                }
            }
            else 
            {
                Class propertyType = 
                    PropertyUtils.getPropertyType(ds, property);
                Object value = 
                    ConvertUtils.convert(c.getString(property), propertyType);
                PropertyUtils.setSimpleProperty(ds, property, value);
            }
        }
        catch (Exception e)
        {
            category.error("Property: " + property + " value: "
                           + c.getString(key) +
                           " is not supported by DataSource: " + 
                           ds.getClass().getName());
            throw e;
        }
    }

}