package org.apache.torque.engine.platform;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
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

import org.apache.torque.engine.database.model.Domain;
import org.apache.torque.engine.database.model.SchemaType;


/**
 * Default implementation for the Platform interface.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class PlatformDefaultImpl implements Platform
{
    private Map schemaDomainMap;
    
    /**
     * Default constructor.
     */
    public PlatformDefaultImpl() 
    {
        initialize();
    }
    
    private void initialize()
    {
        schemaDomainMap = new Hashtable(30);
        Iterator iter = SchemaType.iterator();
        while (iter.hasNext()) 
        {
            SchemaType type = (SchemaType) iter.next();
            schemaDomainMap.put(type, new Domain(type));
        }
        schemaDomainMap.put(SchemaType.BOOLEANCHAR, 
                new Domain(SchemaType.BOOLEANCHAR, "CHAR"));
        schemaDomainMap.put(SchemaType.BOOLEANINT, 
                new Domain(SchemaType.BOOLEANINT, "INTEGER"));
    }

    protected void setSchemaDomainMapping(Domain domain) 
    {
        schemaDomainMap.put(domain.getType(), domain);
    }
    
    /**
     * @see Platform#getMaxColumnNameLength()
     */
    public int getMaxColumnNameLength()
    {
        return 64;
    }

    /**
     * @see Platform#getNativeIdMethod()
     */
    public String getNativeIdMethod()
    {
        return Platform.IDENTITY;
    }

    /**
     * @see Platform#getDomainForJdbcType(SchemaType)
     */
    public Domain getDomainForSchemaType(SchemaType jdbcType) 
    {
        return (Domain) schemaDomainMap.get(jdbcType);
    }

    /**
     * @return Only produces a SQL fragment if null values are
     * disallowed.
     * @see Platform#getNullString(boolean)
     */
    public String getNullString(boolean notNull)
    {
        // TODO: Check whether this is true for all DBs.  Also verify
        // the old Sybase templates.
        return (notNull ? "NOT NULL" : "");
    }

    /**
     * @see Platform#getAutoIncrement()
     */
    public String getAutoIncrement()
    {
        return "IDENTITY";
    }

    /**
     * @see Platform#hasScale(String)
     * TODO collect info for all platforms
     */
    public boolean hasScale(String sqlType)
    {
        return true;
    }

    /**
     * @see Platform#hasSize(String)
     * TODO collect info for all platforms
     */
    public boolean hasSize(String sqlType)
    {
        return true;
    }

}
