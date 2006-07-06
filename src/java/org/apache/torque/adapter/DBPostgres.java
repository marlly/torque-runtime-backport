package org.apache.torque.adapter;

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

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.torque.util.Query;

/**
 * This is used to connect to PostgresQL databases.
 *
 * <a href="http://www.postgresql.org/">http://www.postgresql.org/</a>
 *
 * @author <a href="mailto:hakan42@gmx.de">Hakan Tandogan</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class DBPostgres extends AbstractDBAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7643304924262475272L;

    /** A specialized date format for PostgreSQL. */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private SimpleDateFormat sdf = null;

    /**
     * Empty constructor.
     */
    protected DBPostgres()
    {
        sdf = new SimpleDateFormat(DATE_FORMAT);
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string to transform to upper case.
     * @return The upper case string.
     */
    public String toUpperCase(String in)
    {
        String s = new StringBuffer("UPPER(").append(in).append(")").toString();
        return s;
    }

    /**
     * This method is used to ignore case.
     *
     * @param in The string whose case to ignore.
     * @return The string in a case that can be ignored.
     */
    public String ignoreCase(String in)
    {
        String s = new StringBuffer("UPPER(").append(in).append(")").toString();
        return s;
    }

    /**
     * @see org.apache.torque.adapter.DB#getIDMethodType()
     */
    public String getIDMethodType()
    {
        return SEQUENCE;
    }

    /**
     * @param name The name of the field (should be of type
     *      <code>String</code>).
     * @return SQL to retreive the next database key.
     * @see org.apache.torque.adapter.DB#getIDMethodSQL(Object)
     */
    public String getIDMethodSQL(Object name)
    {
        return ("select nextval('" + name + "')");
    }

    /**
     * Locks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to lock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void lockTable(Connection con, String table) throws SQLException
    {
    }

    /**
     * Unlocks the specified table.
     *
     * @param con The JDBC connection to use.
     * @param table The name of the table to unlock.
     * @exception SQLException No Statement could be created or executed.
     */
    public void unlockTable(Connection con, String table) throws SQLException
    {
    }

    /**
     * This method is used to chek whether the database supports
     * limiting the size of the resultset.
     *
     * @return LIMIT_STYLE_POSTGRES.
     * @deprecated This should not be exposed to the outside     
     */
    public int getLimitStyle()
    {
        return DB.LIMIT_STYLE_POSTGRES;
    }

    /**
     * Return true for PostgreSQL
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeLimit()
     */
    public boolean supportsNativeLimit()
    {
        return true;
    }

    /**
     * Return true for PostgreSQL
     * @see org.apache.torque.adapter.AbstractDBAdapter#supportsNativeOffset()
     */
    public boolean supportsNativeOffset()
    {
        return true;
    }

    /**
     * Generate a LIMIT limit OFFSET offset clause if offset &gt; 0
     * or an LIMIT limit clause if limit is &gt; 0 and offset
     * is 0.
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     */
    public void generateLimits(Query query, int offset, int limit)
    {
        StringBuffer limitStringBuffer = new StringBuffer();

        if (offset > 0)
        {
            limitStringBuffer.append(limit)
                    .append(" offset ")
                    .append(offset);
        }
        else
        {
            if (limit >= 0)
            {
                limitStringBuffer.append(limit);
            }
        }

        query.setLimit(limitStringBuffer.toString());
        query.setPreLimit(null);
        query.setPostLimit(null);
    }

    /**
     * Override the default behavior to associate b with null?
     *
     * @see DB#getBooleanString(Boolean)
     */
    public String getBooleanString(Boolean b)
    {
        return (b == null) ? "FALSE" : (Boolean.TRUE.equals(b) ? "TRUE" : "FALSE");
    }

    /**
     * This method overrides the JDBC escapes used to format dates
     * using a <code>DateFormat</code>.
     *
     * This generates the timedate format defined in
     * http://www.postgresql.org/docs/7.3/static/datatype-datetime.html
     * which defined PostgreSQL dates as YYYY-MM-DD hh:mm:ss
     * @param date the date to format
     * @return The properly formatted date String.
     */
    public String getDateString(Date date)
    {
        StringBuffer dateBuf = new StringBuffer();
        char delim = getStringDelimiter();
        dateBuf.append(delim);
        dateBuf.append(sdf.format(date));
        dateBuf.append(delim);
        return dateBuf.toString();
    }
}
