package org.apache.torque.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;
import org.apache.torque.om.DateKey;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.StringKey;


/**
 * This class represents a part of an SQL query found in the <code>WHERE</code>
 * section.  For example:
 * <pre>
 * table_a.column_a = table_b.column_a
 * column LIKE 'F%'
 * table.column < 3
 * </pre>
 * This class is used primarily by {@link org.apache.torque.util.BasePeer}.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:fedor@apache.org">Fedor Karpelevitch</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public final class SqlExpression
{
    /** escaped single quote */
    private static final char SINGLE_QUOTE = '\'';
    /** escaped backslash */
    private static final char BACKSLASH = '\\';

    /**
     * Private constructor to prevent instantiation.
     *
     * Class contains only static method ans should therefore not be
     * instantiated.
     */
    private SqlExpression()
    {
    }

    /**
     * Used to specify a join on two columns.
     *
     * @param column A column in one of the tables to be joined.
     * @param relatedColumn The column in the other table to be joined.
     * @return A join expression, e.g. UPPER(table_a.column_a) =
     *         UPPER(table_b.column_b).
     */
    public static String buildInnerJoin(String column, String relatedColumn)
    {
        // 'db' can be null because 'ignoreCase' is false.
        return buildInnerJoin(column, relatedColumn, false, null);
    }

    /**
     * Used to specify a join on two columns.
     *
     * @param column A column in one of the tables to be joined.
     * @param relatedColumn The column in the other table to be joined.
     * @param ignoreCase If true and columns represent Strings, the appropriate
     *        function defined for the database will be used to ignore
     *        differences in case.
     * @param db Represents the database in use for vendor-specific functions.
     * @return A join expression, e.g. UPPER(table_a.column_a) =
     *         UPPER(table_b.column_b).
     */
    public static String buildInnerJoin(String column,
                                         String relatedColumn,
                                         boolean ignoreCase,
                                         DB db)
    {
        int addlength = (ignoreCase) ? 25 : 1;
        StringBuffer sb = new StringBuffer(column.length()
                + relatedColumn.length() + addlength);
        buildInnerJoin(column, relatedColumn, ignoreCase, db, sb);
        return sb.toString();
    }

    /**
     * Used to specify a join on two columns.
     *
     * @param column A column in one of the tables to be joined.
     * @param relatedColumn The column in the other table to be joined.
     * @param ignoreCase If true and columns represent Strings, the appropriate
     *        function defined for the database will be used to ignore
     *        differences in case.
     * @param db Represents the database in use for vendor-specific functions.
     * @param whereClause A StringBuffer to which the sql expression will be
     *        appended.
     */
    public static void buildInnerJoin(String column,
                                       String relatedColumn,
                                       boolean ignoreCase,
                                       DB db,
                                       StringBuffer whereClause)
    {
        if (ignoreCase)
        {
            whereClause.append(db.ignoreCase(column))
                    .append('=')
                    .append(db.ignoreCase(relatedColumn));
        }
        else
        {
            whereClause.append(column)
                    .append('=')
                    .append(relatedColumn);
        }
    }


    /**
     * Builds a simple SQL expression.
     *
     * @param columnName A column.
     * @param criteria The value to compare the column against.
     * @param comparison One of =, &lt;, &gt;, ^lt;=, &gt;=, &lt;&gt;,
     *        !=, LIKE, etc.
     * @return A simple SQL expression, e.g. UPPER(table_a.column_a)
     *         LIKE UPPER('ab%c').
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static String build(String columnName,
                                Object criteria,
                                SqlEnum comparison)
        throws TorqueException
    {
        // 'db' can be null because 'ignoreCase' is null
        return build(columnName, criteria, comparison, false, null);
    }

    /**
     * Builds a simple SQL expression.
     *
     * @param columnName A column.
     * @param criteria The value to compare the column against.
     * @param comparison One of =, &lt;, &gt;, ^lt;=, &gt;=, &lt;&gt;,
     *        !=, LIKE, etc.
     * @param ignoreCase If true and columns represent Strings, the appropriate
     *        function defined for the database will be used to ignore
     *        differences in case.
     * @param db Represents the database in use, for vendor specific functions.
     * @return A simple sql expression, e.g. UPPER(table_a.column_a)
     *         LIKE UPPER('ab%c').
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static String build(String columnName,
                                Object criteria,
                                SqlEnum comparison,
                                boolean ignoreCase,
                                DB db)
        throws TorqueException
    {
        int addlength = (ignoreCase ? 40 : 20);
        StringBuffer sb = new StringBuffer(columnName.length() + addlength);
        build(columnName, criteria, comparison, ignoreCase, db, sb);
        return sb.toString();
    }

    /**
     * Builds a simple SQL expression.
     *
     * @param columnName A column.
     * @param criteria The value to compare the column against.
     * @param comparison One of =, &lt;, &gt;, ^lt;=, &gt;=, &lt;&gt;,
     *        !=, LIKE, etc.
     * @param ignoreCase If true and columns represent Strings, the appropriate
     *        function defined for the database will be used to ignore
     *        differences in case.
     * @param db Represents the database in use, for vendor specific functions.
     * @param whereClause A StringBuffer to which the sql expression will be
     *        appended.
     */
    public static void build(String columnName,
                              Object criteria,
                              SqlEnum comparison,
                              boolean ignoreCase,
                              DB db,
                              StringBuffer whereClause)
            throws TorqueException
    {
        // Allow null criteria
        // This will result in queries like
        // insert into table (name, parent) values ('x', null);
        //

        /* Check to see if the criteria is an ObjectKey
         * and if the value of that ObjectKey is null.
         * In that case, criteria should be null.
         */

        if (criteria != null && criteria instanceof ObjectKey)
        {
            if (((ObjectKey) criteria).getValue() == null)
            {
                criteria = null;
            }
        }
        /*  If the criteria is null, check to see comparison
         *  is an =, <>, or !=.  If so, replace the comparison
         *  with the proper IS or IS NOT.
         */

        if (criteria == null)
        {
            criteria = "null";
            if (comparison.equals(Criteria.EQUAL))
            {
                comparison = Criteria.ISNULL;
            }
            else if (comparison.equals(Criteria.NOT_EQUAL))
            {
                comparison = Criteria.ISNOTNULL;
            }
            else if (comparison.equals(Criteria.ALT_NOT_EQUAL))
            {
                comparison = Criteria.ISNOTNULL;
            }
        }
        else
        {
            if (criteria instanceof String || criteria instanceof StringKey)
            {
                criteria = quoteAndEscapeText(criteria.toString(), db);
            }
            else if (criteria instanceof Date)
            {
                Date dt = (Date) criteria;
                criteria = db.getDateString(dt);
            }
            else if (criteria instanceof DateKey)
            {
                Date dt = (Date) ((DateKey) criteria).getValue();
                criteria = db.getDateString(dt);
            }
            else if (criteria instanceof Boolean)
            {
                criteria = db.getBooleanString((Boolean) criteria);
            }
            else if (criteria instanceof Criteria)
            {
                 Query subquery = SQLBuilder.buildQueryClause(
                        (Criteria) criteria,
                        null,
                        new SQLBuilder.QueryCallback() {
                            public String process(
                                    Criteria.Criterion criterion,
                                    List params)
                            {
                                return criterion.toString();
                            }
                });
                if (comparison.equals(Criteria.IN)
                        || comparison.equals(Criteria.NOT_IN))
                {
                    // code below takes care of adding brackets
                    criteria = subquery.toString();
                }
                else
                {
                    criteria = "(" + subquery.toString() + ")";
                }
            }
        }

        if (comparison.equals(Criteria.LIKE)
                || comparison.equals(Criteria.NOT_LIKE)
                || comparison.equals(Criteria.ILIKE)
                || comparison.equals(Criteria.NOT_ILIKE))
        {
            buildLike(columnName, (String) criteria, comparison,
                       ignoreCase, db, whereClause);
        }
        else if (comparison.equals(Criteria.IN)
                || comparison.equals(Criteria.NOT_IN))
        {
            buildIn(columnName, criteria, comparison,
                     ignoreCase, db, whereClause);
        }
        else
        {
            // Do not put the upper/lower keyword around IS NULL
            //  or IS NOT NULL
            if (comparison.equals(Criteria.ISNULL)
                    || comparison.equals(Criteria.ISNOTNULL))
            {
                whereClause.append(columnName)
                        .append(comparison);
            }
            else
            {
                String columnValue = criteria.toString();
                if (ignoreCase && db != null)
                {
                    columnName = db.ignoreCase(columnName);
                    columnValue = db.ignoreCase(columnValue);
                }
                whereClause.append(columnName)
                        .append(comparison)
                        .append(columnValue);
            }
        }
    }

    /**
     * Takes a columnName and criteria and builds an SQL phrase based
     * on whether wildcards are present and the state of the
     * ignoreCase flag.  Multicharacter wildcards % and * may be used
     * as well as single character wildcards, _ and ?.  These
     * characters can be escaped with \.
     *
     * e.g. criteria = "fre%" -> columnName LIKE 'fre%'
     *                        -> UPPER(columnName) LIKE UPPER('fre%')
     *      criteria = "50\%" -> columnName = '50%'
     *
     * @param columnName A column.
     * @param criteria The value to compare the column against.
     * @param comparison Whether to do a LIKE or a NOT LIKE
     * @param ignoreCase If true and columns represent Strings, the
     * appropriate function defined for the database will be used to
     * ignore differences in case.
     * @param db Represents the database in use, for vendor specific functions.
     * @return An SQL expression.
     */
    static String buildLike(String columnName,
                             String criteria,
                             SqlEnum comparison,
                             boolean ignoreCase,
                             DB db)
    {
        StringBuffer whereClause = new StringBuffer();
        buildLike(columnName, criteria, comparison, ignoreCase, db,
                   whereClause);
        return whereClause.toString();
    }

    /**
     * Takes a columnName and criteria and builds an SQL phrase based
     * on whether wildcards are present and the state of the
     * ignoreCase flag.  Multicharacter wildcards % and * may be used
     * as well as single character wildcards, _ and ?.  These
     * characters can be escaped with \.
     *
     * e.g. criteria = "fre%" -> columnName LIKE 'fre%'
     *                        -> UPPER(columnName) LIKE UPPER('fre%')
     *      criteria = "50\%" -> columnName = '50%'
     *
     * @param columnName A column name.
     * @param criteria The value to compare the column against.
     * @param comparison Whether to do a LIKE or a NOT LIKE
     * @param ignoreCase If true and columns represent Strings, the
     * appropriate function defined for the database will be used to
     * ignore differences in case.
     * @param db Represents the database in use, for vendor specific functions.
     * @param whereClause A StringBuffer to which the sql expression
     * will be appended.
     */
    static void buildLike(String columnName,
                           String criteria,
                           SqlEnum comparison,
                           boolean ignoreCase,
                           DB db,
                           StringBuffer whereClause)
    {
        // If selection criteria contains wildcards use LIKE otherwise
        // use = (equals).  Wildcards can be escaped by prepending
        // them with \ (backslash). However, if we switch from
        // like to equals, we need to remove the escape characters.
        // from the wildcards.
        // So we need two passes: The first replaces * and ? by % and _,
        // and checks whether we switch to equals,
        // the second removes escapes if we have switched to equals.
        int position = 0;
        StringBuffer sb = new StringBuffer();
        boolean replaceWithEquals = true;
        while (position < criteria.length())
        {
            char checkWildcard = criteria.charAt(position);

            switch (checkWildcard)
            {
            case BACKSLASH:
                // if text is escaped, all backslashes are already escaped,
                // so the next character after the backslash is the doubled
                // backslash from escaping.
                int charsToProceed = db.escapeText() ? 2 : 1;
                if (position + charsToProceed >= criteria.length())
                {
                    charsToProceed = criteria.length() - position - 1;
                }
                else if (criteria.charAt(position + charsToProceed) == BACKSLASH
                        && db.escapeText())
                {
                    // the escaped backslash is also escaped,
                    // so we need to proceed another character
                    charsToProceed += 1;
                }
                sb.append(criteria.substring(
                        position,
                        position + charsToProceed));
                position += charsToProceed;
                // code below copies escaped character into sb
                checkWildcard = criteria.charAt(position);
                break;
            case '%':
            case '_':
                replaceWithEquals = false;
                break;
            case '*':
                replaceWithEquals = false;
                checkWildcard = '%';
                break;
            case '?':
                replaceWithEquals = false;
                checkWildcard = '_';
                break;
            }

            sb.append(checkWildcard);
            position++;
        }
        criteria = sb.toString();

        if (ignoreCase)
        {
            if (db.useIlike() && !replaceWithEquals)
            {
                if (SqlEnum.LIKE.equals(comparison))
                {
                    comparison = SqlEnum.ILIKE;
                }
                else if (SqlEnum.NOT_LIKE.equals(comparison))
                {
                    comparison = SqlEnum.NOT_ILIKE;
                }
            }
            else
            {
                // no native case insensitive like is offered by the DB,
                // or the LIKE was replaced with equals.
                // need to ignore case manually.
                columnName = db.ignoreCase(columnName);
            }
        }
        whereClause.append(columnName);

        if (replaceWithEquals)
        {
            if (comparison.equals(Criteria.NOT_LIKE)
                    || comparison.equals(Criteria.NOT_ILIKE))
            {
                whereClause.append(" ").append(Criteria.NOT_EQUAL).append(" ");
            }
            else
            {
                whereClause.append(" ").append(Criteria.EQUAL).append(" ");
            }

            // remove escape backslashes from String
            position = 0;
            sb = new StringBuffer();
            while (position < criteria.length())
            {
                char checkWildcard = criteria.charAt(position);

                if (checkWildcard == BACKSLASH)
                {
                    // if text is escaped, all backslashes are already escaped,
                    // so the next character after the backslash is the doubled
                    // backslash from escaping.
                    int charsToSkip = db.escapeText() ? 2 : 1;
                    if (position + charsToSkip >= criteria.length())
                    {
                        charsToSkip = criteria.length() - position - 1;
                    }
                    else if (criteria.charAt(position + charsToSkip)
                                == BACKSLASH
                            && db.escapeText())
                    {
                        // the escaped backslash is also escaped,
                        // so we need to skip another character
                        // but add the escaped backslash to sb
                        // so that the escaping remains.
                        sb.append(BACKSLASH);
                        charsToSkip += 1;
                    }
                    position += charsToSkip;
                    // code below copies escaped character into sb
                    checkWildcard = criteria.charAt(position);
                }
                sb.append(checkWildcard);
                position++;
            }
            criteria = sb.toString();
       }
        else
        {
            whereClause.append(comparison);
        }

        // If selection is case insensitive use SQL UPPER() function
        // on criteria.
        if (ignoreCase && (!(db.useIlike()) || replaceWithEquals))
        {
            criteria = db.ignoreCase(criteria);
        }
        whereClause.append(criteria);

        if (!replaceWithEquals && db.useEscapeClauseForLike())
        {
            whereClause.append(SqlEnum.ESCAPE)
                       .append("'\\'");
        }
    }

    /**
     * Takes a columnName and criteria (which must be an array) and
     * builds a SQL 'IN' expression taking into account the ignoreCase
     * flag.
     *
     * @param columnName A column.
     * @param criteria The value to compare the column against.
     * @param comparison Either " IN " or " NOT IN ".
     * @param ignoreCase If true and columns represent Strings, the
     * appropriate function defined for the database will be used to
     * ignore differences in case.
     * @param db Represents the database in use, for vendor specific functions.
     * @return An SQL expression.
     */
    static String buildIn(String columnName,
                          Object criteria,
                          SqlEnum comparison,
                          boolean ignoreCase,
                          DB db)
    {
        StringBuffer whereClause = new StringBuffer();
        buildIn(columnName, criteria, comparison,
                ignoreCase, db, whereClause);
        return whereClause.toString();
    }

    /**
     * Takes a columnName and criteria (which must be an array) and
     * builds a SQL 'IN' expression taking into account the ignoreCase
     * flag.
     *
     * @param columnName A column.
     * @param criteria The value to compare the column against.
     * @param comparison Either " IN " or " NOT IN ".
     * @param ignoreCase If true and columns represent Strings, the
     * appropriate function defined for the database will be used to
     * ignore differences in case.
     * @param db Represents the database in use, for vendor specific functions.
     * @param whereClause A StringBuffer to which the sql expression
     * will be appended.
     */
    static void buildIn(String columnName,
                        Object criteria,
                        SqlEnum comparison,
                        boolean ignoreCase,
                        DB db,
                        StringBuffer whereClause)
    {
        if (ignoreCase)
        {
            whereClause.append(db.ignoreCase(columnName));
        }
        else
        {
            whereClause.append(columnName);
        }

        whereClause.append(comparison);
        HashSet inClause = new HashSet();
        if (criteria instanceof List)
        {
            Iterator iter = ((List) criteria).iterator();
            while (iter.hasNext())
            {
                Object value = iter.next();

                // The method processInValue() quotes the string
                // and/or wraps it in UPPER().
                inClause.add(processInValue(value, ignoreCase, db));
            }
        }
        else if (criteria instanceof String)
        {
            // subquery
            inClause.add(criteria);
        }
        else
        {
            // Assume array.
            for (int i = 0; i < Array.getLength(criteria); i++)
            {
                Object value = Array.get(criteria, i);

                // The method processInValue() quotes the string
                // and/or wraps it in UPPER().
                inClause.add(processInValue(value, ignoreCase, db));
            }
        }
        whereClause.append('(')
                   .append(StringUtils.join(inClause.iterator(), ","))
                   .append(')');
    }

    /**
     * Creates an appropriate string for an 'IN' clause from an
     * object.  Adds quoting and/or UPPER() as appropriate.  This is
     * broken out into a seperate method as it is used in two places
     * in buildIn, depending on whether an array or List is being
     * looped over.
     *
     * @param value The value to process.
     * @param ignoreCase Coerce the value suitably for ignoring case.
     * @param db Represents the database in use for vendor specific functions.
     * @return Processed value as String.
     */
    static String processInValue(Object value,
                                 boolean ignoreCase,
                                 DB db)
    {
        String ret = null;
        if (value instanceof String)
        {
            ret = quoteAndEscapeText((String) value, db);
        }
        else
        {
            ret = value.toString();
        }
        if (ignoreCase)
        {
            ret = db.ignoreCase(ret);
        }
        return ret;
    }

    /**
     * Quotes and escapes raw text for placement in a SQL expression.
     * For simplicity, the text is assumed to be neither quoted nor
     * escaped.
     *
     * @param rawText The <i>unquoted</i>, <i>unescaped</i> text to process.
     * @param db the db
     * @return Quoted and escaped text.
     */
    public static String quoteAndEscapeText(String rawText, DB db)
    {
        StringBuffer buf = new StringBuffer((int) (rawText.length() * 1.1));

        // Some databases do not need escaping.
        String escapeString;
        if (db != null && !db.escapeText())
        {
            escapeString = String.valueOf(BACKSLASH);
        }
        else
        {
            escapeString = String.valueOf(BACKSLASH)
                    + String.valueOf(BACKSLASH);
        }

        char[] data = rawText.toCharArray();
        buf.append(SINGLE_QUOTE);
        for (int i = 0; i < data.length; i++)
        {
            switch (data[i])
            {
            case SINGLE_QUOTE:
                buf.append(SINGLE_QUOTE).append(SINGLE_QUOTE);
                break;
            case BACKSLASH:
                buf.append(escapeString);
                break;
            default:
                buf.append(data[i]);
            }
        }
        buf.append(SINGLE_QUOTE);

        return buf.toString();
    }
}
