package org.apache.torque.dsfactory;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import javax.sql.DataSource;
import org.apache.commons.configuration.Configuration;
import org.apache.torque.TorqueException;

/**
 * A factory that returns a DataSource.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public interface DataSourceFactory
{
    
    /**
     * @return the <code>DataSource</code> configured by the factory.
     * @throws TorqueException if the source can't be returned
     */
    DataSource getDataSource() throws TorqueException;

    /**
     * Initialize the factory.
     *
     * @param configuration where to load the factory settings from
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    void initialize(Configuration configuration) 
        throws TorqueException;

    /**
     * Sets the current schema for the database connection
     *
     * @param schema The current schema name
     */
    void setSchema(String schema);

    /**
     * This method returns the current schema for the database connection
     *
     * @return The current schema name. Null means, no schema has been set.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    String getSchema();

}
