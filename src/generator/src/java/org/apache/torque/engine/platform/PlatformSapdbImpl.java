package org.apache.torque.engine.platform;

/*
 * Copyright 2003,2004 The Apache Software Foundation.
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

import org.apache.torque.engine.database.model.Domain;
import org.apache.torque.engine.database.model.SchemaType;

/**
 * SAP DB Platform implementation.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class PlatformSapdbImpl extends PlatformDefaultImpl
{
    /**
     * Default constructor.
     */
    public PlatformSapdbImpl()
    {
        super();
        initialize();
    }

    /**
     * Initializes db specific domain mapping.
     */
    private void initialize()
    {
        setSchemaDomainMapping(new Domain(SchemaType.BIT, "FIXED", "1", "0"));
        setSchemaDomainMapping(new Domain(SchemaType.TINYINT, "CHAR BYTE"));
        setSchemaDomainMapping(new Domain(SchemaType.BIGINT, "FIXED", "38", "0"));
        setSchemaDomainMapping(new Domain(SchemaType.DOUBLE, "DOUBLE PRECISION"));
        setSchemaDomainMapping(new Domain(SchemaType.NUMERIC, "DECIMAL"));
        setSchemaDomainMapping(new Domain(SchemaType.LONGVARCHAR, "LONG VARCHAR"));
        setSchemaDomainMapping(new Domain(SchemaType.BINARY, "CHAR(254) BYTE"));
        setSchemaDomainMapping(new Domain(SchemaType.VARBINARY, "LONG BYTE"));
        setSchemaDomainMapping(new Domain(SchemaType.LONGVARBINARY, "LONG BYTE"));
        setSchemaDomainMapping(new Domain(SchemaType.JAVA_OBJECT, "LONG BYTE"));
        setSchemaDomainMapping(new Domain(SchemaType.BLOB, "LONG BYTE"));
        setSchemaDomainMapping(new Domain(SchemaType.CLOB, "LONG UNICODE"));
        setSchemaDomainMapping(new Domain(SchemaType.BOOLEANINT, "SMALLINT"));
        setSchemaDomainMapping(new Domain(SchemaType.BOOLEANCHAR, "CHAR"));
    }

    /**
     * @see Platform#getNativeIdMethod()
     */
    public String getNativeIdMethod()
    {
        return Platform.SEQUENCE;
    }

    /**
     * @see Platform#getAutoIncrement()
     */
    public String getAutoIncrement()
    {
        return "DEFAULT SERIAL";
    }
    
}
