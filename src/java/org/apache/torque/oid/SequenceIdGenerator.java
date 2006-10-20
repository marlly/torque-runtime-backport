package org.apache.torque.oid;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.math.BigDecimal;
import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.adapter.DB;
import org.apache.torque.util.SQLBuilder;

import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.Value;

/**
 * This generator works with databases that have an sql syntax for
 * getting an id prior to inserting a row into the database.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class SequenceIdGenerator implements IdGenerator
{
    /** The log. */
    private static Log log = LogFactory.getLog(SequenceIdGenerator.class);

    /** the adapter that knows the correct sql syntax */
    private DB dbAdapter;

    /** The internal name of the Database that this Generator is connected to */
    private String name = null;

    /**
     * Creates an IdGenerator which will work with the specified database.
     *
     * @param dbAdapter the adapter that knows the correct sql syntax.
     * @param name The name of the datasource to find the correct schema
     */
    public SequenceIdGenerator(final DB dbAdapter, final String name)
    {
        this.dbAdapter = dbAdapter;
        this.name = name;
    }

    /**
     * Retrieves an id as an int.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return An int with the value for the id.
     * @exception Exception Database error.
     */
    public int getIdAsInt(Connection connection, Object keyInfo)
        throws Exception
    {
        return getIdAsVillageValue(connection, keyInfo).asInt();
    }

    /**
     * Retrieves an id as an long.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A long with the value for the id.
     * @exception Exception Database error.
     */
    public long getIdAsLong(Connection connection, Object keyInfo)
        throws Exception
    {
        return getIdAsVillageValue(connection, keyInfo).asLong();
    }

    /**
     * Retrieves an id as a BigDecimal.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A BigDecimal id
     * @exception Exception Database error.
     */
    public BigDecimal getIdAsBigDecimal(Connection connection, Object keyInfo)
        throws Exception
    {
        return getIdAsVillageValue(connection, keyInfo).asBigDecimal();
    }

    /**
     * Retrieves an id as an String.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A String id
     * @exception Exception Database error.
     */
    public String getIdAsString(Connection connection, Object keyInfo)
        throws Exception
    {
        return getIdAsVillageValue(connection, keyInfo).asString();
    }

    /**
     * A flag to determine the timing of the id generation
     *
     * @return a <code>boolean</code> value
     */
    public boolean isPriorToInsert()
    {
        return true;
    }

    /**
     * A flag to determine the timing of the id generation
     *
     * @return a <code>boolean</code> value
     */
    public boolean isPostInsert()
    {
        return false;
    }

    /**
     * A flag to determine whether a Connection is required to
     * generate an id.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isConnectionRequired()
    {
        return true;
    }

    /**
     * Retrieves an id as a Village Value.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A Village Value id.
     * @exception Exception Database error.
     */
    private Value getIdAsVillageValue(Connection connection, Object keyInfo)
        throws Exception
    {
        String sequenceName = SQLBuilder.getFullTableName(String.valueOf(keyInfo), name);
        String idSql = dbAdapter.getIDMethodSQL(sequenceName);
        if (log.isDebugEnabled())
        {
            log.debug(idSql);
        }

        // Execute the query.
        QueryDataSet qds = new QueryDataSet(connection, idSql);
        Record rec;
        try
        {
            qds.fetchRecords(1);
            rec = qds.getRecord(0);  // Records are 0 based.
        }
        finally
        {
            qds.close();
        }
        return rec.getValue(1); // Values are 1 based.
    }
}
