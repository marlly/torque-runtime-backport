package org.apache.torque.oid;

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

import java.math.BigDecimal;
import java.sql.Connection;
import org.apache.torque.adapter.DB;
import org.apache.torque.util.SQLBuilder;
import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.Value;

/**
 * This generator works with databases that have an sql syntax that
 * allows the retrieval of the last id used to insert a row for a
 * Connection.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public class AutoIncrementIdGenerator implements IdGenerator
{
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
    public AutoIncrementIdGenerator(final DB dbAdapter, final String name)
    {
        this.dbAdapter = dbAdapter;
        this.name = name;
    }

    /**
     * Returns the last ID used by this connection.
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
     * Returns the last ID used by this connection.
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
     * Returns the last ID used by this connection.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A BigDecimal with the last value auto-incremented as a
     * result of an insert.
     * @exception Exception Database error.
     */
    public BigDecimal getIdAsBigDecimal(Connection connection, Object keyInfo)
        throws Exception
    {
        return getIdAsVillageValue(connection, keyInfo).asBigDecimal();
    }


    /**
     * Returns the last ID used by this connection.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A String with the last value auto-incremented as a
     * result of an insert.
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
        return false;
    }

    /**
     * A flag to determine the timing of the id generation
     *
     * @return a <code>boolean</code> value
     */
    public boolean isPostInsert()
    {
        return true;
    }

    /**
     * A flag to determine whether a Connection is required to
     * generate an id.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isConnectionRequired()
    {
        return true;
    }

    /**
     * Returns the last ID used by this connection.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A Village Value with the last value auto-incremented as a
     * result of an insert.
     * @exception Exception Database error.
     */
    private Value getIdAsVillageValue(Connection connection, Object keyInfo)
        throws Exception
    {
        String tableName = SQLBuilder.getFullTableName(String.valueOf(keyInfo), name);
        String idSQL = dbAdapter.getIDMethodSQL(tableName);
        Value id = null;
        QueryDataSet qds = null;
        try
        {
            qds = new QueryDataSet(connection, idSQL);
            qds.fetchRecords(1);
            Record rec = qds.getRecord(0);
            id = rec.getValue(1);
        }
        finally
        {
            if (qds != null)
            {
                qds.close();
            }
        }
        return id;
    }
}
