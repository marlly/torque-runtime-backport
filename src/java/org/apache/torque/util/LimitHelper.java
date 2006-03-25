package org.apache.torque.util;

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

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;


/**
 * Factored out all the various &quot;How to generate offset and limit
 * for my personal database&quot; from the BasePeer. And tried to get
 * some sense it this.
 *
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */

public abstract class LimitHelper
{

    /**
     * Update the Query object according to the limiting information
     * available in the Criteria
     *
     * @param critera the Criteria to read
     * @param query The query object to update
     */
    public static final void buildLimit(Criteria criteria, Query query)
        throws TorqueException
    {
        int limit = criteria.getLimit();
        int offset = criteria.getOffset();

        DB db = Torque.getDB(criteria.getDbName());

        if (offset > 0 || limit >= 0)
        {
            // If we hit a database type, that is able to do native
            // limiting, we must set the criteria values to -1 and 0
            // afterwards. Reason is, that else theexecuteQuery
            // method tries to do the limiting using Village
            //
            switch (db.getLimitStyle())
            {
            case DB.LIMIT_STYLE_MYSQL :
                LimitHelper.generateMySQLLimits(query, offset, limit);
                break;
            case DB.LIMIT_STYLE_POSTGRES :
                LimitHelper.generatePostgreSQLLimits(query, offset, limit);
                break;
            case DB.LIMIT_STYLE_ORACLE :
                LimitHelper.generateOracleLimits(query, offset, limit);
                break;
            case DB.LIMIT_STYLE_DB2 :
                LimitHelper.generateDB2Limits(query, offset, limit);
                break;
            default:
                if (db.supportsNativeLimit())
                {
                    query.setLimit(String.valueOf(limit));
                }
                break;
            }
        }
    }

    /**
     * Generate a LIMIT offset, limit clause if offset &gt; 0
     * or an LIMIT limit clause if limit is &gt; 0 and offset
     * is 0.
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     */
    private static final void generateMySQLLimits(Query query,
            int offset, int limit)
    {
        StringBuffer limitStringBuffer = new StringBuffer();

        if (offset > 0)
        {
            limitStringBuffer.append(offset)
                    .append(", ")
                    .append(limit);
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
     * Generate a LIMIT limit OFFSET offset clause if offset &gt; 0
     * or an LIMIT limit clause if limit is &gt; 0 and offset
     * is 0.
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     */
    private static final void generatePostgreSQLLimits(Query query,
            int offset, int limit)
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
            if (limit > 0)
            {
                limitStringBuffer.append(limit);
            }
        }

        query.setLimit(limitStringBuffer.toString());
        query.setPreLimit(null);
        query.setPostLimit(null);
    }

    /**
     * Build Oracle-style query with limit or offset.
     * If the original SQL is in variable: query then the requlting
     * SQL looks like this:
     * <pre>
     * SELECT B.* FROM (
     *          SELECT A.*, rownum as TORQUE$ROWNUM FROM (
     *                  query
     *          ) A
     *     ) B WHERE B.TORQUE$ROWNUM > offset AND B.TORQUE$ROWNUM
     *     <= offset + limit
     * </pre>
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     */
    private static final void generateOracleLimits(Query query,
            int offset, int limit)
    {
        StringBuffer preLimit = new StringBuffer()
                .append("SELECT B.* FROM ( ")
                .append("SELECT A.*, rownum AS TORQUE$ROWNUM FROM ( ");

        StringBuffer postLimit = new StringBuffer()
                .append(" ) A ")
                .append(" ) B WHERE ");

        if (offset > 0)
        {
            postLimit.append(" B.TORQUE$ROWNUM > ")
                    .append(offset);

            if (limit > 0)
            {
                postLimit.append(" AND B.TORQUE$ROWNUM <= ")
                        .append(offset + limit);
            }
        }
        else
        {
            postLimit.append(" B.TORQUE$ROWNUM <= ")
                    .append(limit);
        }

        query.setPreLimit(preLimit.toString());
        query.setPostLimit(postLimit.toString());
        query.setLimit(null);
    }

    /**
     * Build DB2 (OLAP) -style query with limit or offset.
     * If the original SQL is in variable: query then the requlting
     * SQL looks like this:
     * <pre>
     * SELECT B.* FROM (
     *          SELECT A.*, row_number() over() as TORQUE$ROWNUM FROM (
     *                  query
     *          ) A
     *     ) B WHERE B.TORQUE$ROWNUM > offset AND B.TORQUE$ROWNUM
     *     <= offset + limit
     * </pre>
     *
     * @param query The query to modify
     * @param offset the offset Value
     * @param limit the limit Value
     */
    private static final void generateDB2Limits(Query query,
            int offset, int limit)
    {
        StringBuffer preLimit = new StringBuffer()
                .append("SELECT B.* FROM ( ")
                .append("SELECT A.*, row_number() over() AS TORQUE$ROWNUM FROM ( ");

        StringBuffer postLimit = new StringBuffer()
                .append(" ) A ")
                .append(" ) B WHERE ");

        if (offset > 0)
        {
            postLimit.append(" B.TORQUE$ROWNUM > ")
                    .append(offset);

            if (limit > 0)
            {
                postLimit.append(" AND B.TORQUE$ROWNUM <= ")
                        .append(offset + limit);
            }
        }
        else
        {
            postLimit.append(" B.TORQUE$ROWNUM <= ")
                    .append(limit);
        }

        query.setPreLimit(preLimit.toString());
        query.setPostLimit(postLimit.toString());
        query.setLimit(null);
    }
}
