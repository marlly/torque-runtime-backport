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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.DatabaseMap;
import org.apache.torque.util.Criteria.Criterion;

/**
 * Factored out code that is used to process SQL tables. This code comes
 * from BasePeer and is put here to reduce complexity in the BasePeer class.
 * You should not use the methods here directly!
 *
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
public final class SQLBuilder
{
    /** Logging */
    protected static final Log log = LogFactory.getLog(SQLBuilder.class);

    /** Function Characters */
    public static final String[] COLUMN_CHARS = {".", "*"};
    public static final String[] DELIMITERS = {" ", ",", "(", ")", "<", ">"};

    /**
     * Private constructor to prevent instantiation.
     *
     * Class contains only static method ans should therefore not be
     * instantiated.
     */
    private SQLBuilder()
    {
    }

    /**
     * Fully qualify a table name with an optional schema reference
     *
     * @param table The table name to use. If null is passed in, null is returned.
     * @param dbName The name of the database to which this tables belongs.
     *               If null is passed, the default database is used.
     *
     * @return The table name to use inside the SQL statement. If null is passed
     *         into this method, null is returned.
     * @exception TorqueException if an error occurs
     */
    public static String getFullTableName(
            final String table,
            final String dbName)
        throws TorqueException
    {
        if (table != null)
        {
            int dotIndex = table.indexOf(".");

            if (dotIndex == -1) // No schema given
            {
                String targetDBName = (dbName == null)
                        ? Torque.getDefaultDB()
                        : dbName;

                String targetSchema = Torque.getSchema(targetDBName);

                // If we have a default schema, fully qualify the
                // table and return.
                if (StringUtils.isNotEmpty(targetSchema))
                {
                    return new StringBuffer()
                            .append(targetSchema)
                            .append(".")
                            .append(table)
                            .toString();
                }
            }
        }

        return table;
    }

    /**
     * Remove a possible schema name from the table name.
     *
     * @param table The table name to use
     *
     * @return The table name with a possible schema name
     *         stripped off
     */
    public static String getUnqualifiedTableName(final String table)
    {
        if (table != null)
        {
            int dotIndex = table.lastIndexOf("."); // Do we have a dot?

            if (++dotIndex > 0) // Incrementation allows for better test _and_ substring...
            {
                return table.substring(dotIndex);
            }
        }

        return table;
    }

    /**
     * Removes a possible function name or clause from a column name
     *
     * @param name The column name, possibly containing a clause
     *
     * @return The column name
     *
     * @throws TorqueException If the column name was malformed
     */
    private static String removeSQLFunction(final String name)
            throws TorqueException
    {
        // Empty name => return it
        if (StringUtils.isEmpty(name))
        {
            return name;
        }

        // Find Table.Column
        int dotIndex = name.indexOf('.');
        if (dotIndex == -1)
        {
            dotIndex = name.indexOf("*");
        }
        if (dotIndex == -1)
        {
            throw new TorqueException("removeSQLFunction() : Column name "
                    + name
                    + " does not contain a . or a *");
        }
        String pre = name.substring(0, dotIndex);
        String post = name.substring(dotIndex + 1, name.length());
        int startIndex = StringUtils.lastIndexOfAny(pre, DELIMITERS);
        int endIndex = StringUtils.indexOfAny(post, DELIMITERS);
        if (startIndex < 0 && endIndex < 0)
        {
            return name;
        }
        else
        {
            if (endIndex < 0)
            {
                endIndex = post.length();
            }
            // if startIndex == -1 the formula is correct
            return name.substring(startIndex + 1, dotIndex + 1 + endIndex);
        }
    }

    /**
     * Returns a table name from an identifier. Each identifier is to be qualified
     * as [schema.]table.column. This could also contain FUNCTION([schema.]table.column).
     *
     * @param name The (possible fully qualified) identifier name
     *
     * @return the fully qualified table name
     *
     * @throws TorqueException If the identifier name was malformed
     */
    public static String getTableName(final String name, final String dbName)
            throws TorqueException
    {
        final String testName = removeSQLFunction(name);

        if (StringUtils.isEmpty(testName))
        {
            throwMalformedColumnNameException(
                    "getTableName",
                    name);
        }

        // Everything before the last dot is the table name
        int rightDotIndex = testName.lastIndexOf('.');

        if (rightDotIndex < 0)
        {
            if ("*".equals(testName))
            {
                return null;
            }

            throwMalformedColumnNameException(
                    "getTableName",
                    name);
        }

        return getFullTableName(testName.substring(0, rightDotIndex), dbName);
    }



    /**
     * Returns a set of all tables and possible aliases referenced
     * from a criterion. The resulting Set can be directly used to
     * build a WHERE clause
     *
     * @param crit A Criteria object
     * @param tableCallback A Callback Object
     * @return A Set of tables.
     */
    public static Set getTableSet(
            final Criteria crit,
            final TableCallback tableCallback)
    {
        HashSet tables = new HashSet();

        // Loop over all the Criterions
        for (Iterator it = crit.keySet().iterator(); it.hasNext();)
        {
            String key = (String) it.next();
            Criteria.Criterion c = crit.getCriterion(key);
            List tableNames = c.getAllTables();

            // Loop over all Tables referenced in this criterion.
            for (Iterator it2 = tableNames.iterator(); it2.hasNext();)
            {
                String name = (String) it2.next();
                String aliasName = crit.getTableForAlias(name);

                // If the tables have an alias, add an "<xxx> AS <yyy> statement"
                if (StringUtils.isNotEmpty(aliasName))
                {
                    String newName =
                            new StringBuffer(name.length() + aliasName.length() + 4)
                            .append(aliasName)
                            .append(" AS ")
                            .append(name)
                            .toString();
                    name = newName;
                }
                tables.add(name);
            }

            if (tableCallback != null)
            {
                tableCallback.process(tables, key, crit);
            }
        }

        return tables;
    }

    /**
     * Builds a Query clause for Updating and deleting
     *
     * @param crit a <code>Criteria</code> value
     * @param params a <code>List</code> value
     * @param qc a <code>QueryCallback</code> value
     * @return a <code>Query</code> value
     * @exception TorqueException if an error occurs
     */
    public static Query buildQueryClause(final Criteria crit,
            final List params,
            final QueryCallback qc)
            throws TorqueException
    {
        Query query = new Query();

        final String dbName = crit.getDbName();
        final DB db = Torque.getDB(dbName);
        final DatabaseMap dbMap = Torque.getDatabaseMap(dbName);

        JoinBuilder.processJoins(db, dbMap, crit, query);
        processModifiers(crit, query);
        processSelectColumns(crit, query, dbName);
        processAsColumns(crit, query);
        processCriterions(db, dbMap, dbName, crit, query,  params, qc);
        processGroupBy(crit, query);
        processHaving(crit, query);
        processOrderBy(db, dbMap, crit, query);
        processLimits(crit, query);

        if (log.isDebugEnabled())
        {
            log.debug(query.toString());
        }
        return query;
    }


    /**
     * adds the select columns from the criteria to the query
     * @param criteria the criteria from which the select columns are taken
     * @param query the query to which the select columns should be added
     * @throws TorqueException if the select columns can not be processed
     */
    private static void processSelectColumns(
            final Criteria criteria,
            final Query query,
            final String dbName)
        throws TorqueException
    {
        UniqueList selectClause = query.getSelectClause();
        UniqueList select = criteria.getSelectColumns();

        for (int i = 0; i < select.size(); i++)
        {
            String identifier = (String) select.get(i);
            selectClause.add(identifier);
            addTableToFromClause(getTableName(identifier, dbName), criteria, query);
        }
    }

    /**
     * adds the As-columns from the criteria to the query.
     * @param criteria the criteria from which the As-columns are taken
     * @param query the query to which the As-columns should be added
     */
    private static void processAsColumns(
            final Criteria criteria,
            final Query query)
    {
        UniqueList querySelectClause = query.getSelectClause();
        Map criteriaAsColumns = criteria.getAsColumns();

        for (Iterator it = criteriaAsColumns.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            querySelectClause.add(
                    new StringBuffer()
                    .append(entry.getValue())
                    .append(SqlEnum.AS)
                    .append(key)
                    .toString());
        }
    }

    /**
     * adds the Modifiers from the criteria to the query
     * @param criteria the criteria from which the Modifiers are taken
     * @param query the query to which the Modifiers should be added
     */
    private static void processModifiers(
            final Criteria criteria,
            final Query query)
    {
        UniqueList selectModifiers = query.getSelectModifiers();
        UniqueList modifiers = criteria.getSelectModifiers();
        for (int i = 0; i < modifiers.size(); i++)
        {
            selectModifiers.add(modifiers.get(i));
        }
    }

    /**
     * adds the Criterion-objects from the criteria to the query
     * @param criteria the criteria from which the Criterion-objects are taken
     * @param query the query to which the Criterion-objects should be added
     * @param params the parameters if a prepared statement should be built,
     *        or null if a normal statement should be built.
     * @throws TorqueException if the Criterion-objects can not be processed
     */
    private static void processCriterions(
            final DB db,
            final DatabaseMap dbMap,
            final String dbName,
            final Criteria crit,
            final Query query,
            final List params,
            final QueryCallback qc)
        throws TorqueException
    {
        UniqueList whereClause = query.getWhereClause();

        for (Iterator it = crit.keySet().iterator(); it.hasNext();)
        {
            String key = (String) it.next();
            Criteria.Criterion criterion = crit.getCriterion(key);
            Criteria.Criterion[] someCriteria =
                    criterion.getAttachedCriterion();

            String table = null;
            for (int i = 0; i < someCriteria.length; i++)
            {
                String tableName = someCriteria[i].getTable();

                // add the table to the from clause, if it is not already
                // contained there
                // it is important that this piece of code is executed AFTER
                // the joins are processed
                addTableToFromClause(getFullTableName(tableName, dbName), crit, query);

                table = crit.getTableForAlias(tableName);
                if (table == null)
                {
                    table = tableName;
                }

                boolean ignoreCase =  ((crit.isIgnoreCase() || someCriteria[i].isIgnoreCase())
                        && (dbMap.getTable(table)
                                .getColumn(someCriteria[i].getColumn())
                                .getType()
                                instanceof String));

                someCriteria[i].setIgnoreCase(ignoreCase);
            }

            criterion.setDB(db);
            whereClause.add(qc.process(criterion, params));
        }
    }

    /**
     * adds the OrderBy-Columns from the criteria to the query
     * @param criteria the criteria from which the OrderBy-Columns are taken
     * @param query the query to which the OrderBy-Columns should be added
     * @throws TorqueException if the OrderBy-Columns can not be processed
     */
    private static void processOrderBy(
            final DB db,
            final DatabaseMap dbMap,
            final Criteria crit,
            final Query query)
            throws TorqueException
    {
        UniqueList orderByClause = query.getOrderByClause();
        UniqueList selectClause = query.getSelectClause();

        UniqueList orderBy = crit.getOrderByColumns();

        if (orderBy != null && orderBy.size() > 0)
        {
            // Check for each String/Character column and apply
            // toUpperCase().
            for (int i = 0; i < orderBy.size(); i++)
            {
                String orderByColumn = (String) orderBy.get(i);

                String strippedColumnName
                        = removeSQLFunction(orderByColumn);
                int dotPos = strippedColumnName.lastIndexOf('.');
                if (dotPos == -1)
                {
                    // We are not able to look up the table in the
                    // tableMap, as no table name is given. Simply add
                    // the orderBy and hope the user knows what he is
                    // doing.
                    orderByClause.add(orderByColumn);
                    continue;
                }

                String tableName = strippedColumnName.substring(0, dotPos);
                String table = crit.getTableForAlias(tableName);
                if (table == null)
                {
                    table = tableName;
                }

                // See if there's a space (between the column list and sort
                // order in ORDER BY table.column DESC).
                int spacePos = strippedColumnName.indexOf(' ');
                String columnName;
                if (spacePos == -1)
                {
                    columnName =
                            strippedColumnName.substring(dotPos + 1);
                }
                else
                {
                    columnName = strippedColumnName.substring(
                            dotPos + 1,
                            spacePos);
                }
                ColumnMap column = dbMap.getTable(table).getColumn(columnName);

                // only ignore case in order by for string columns
                // which do not have a function around them
                if (column.getType() instanceof String
                        && orderByColumn.indexOf('(') == -1)
                {
                    // find space pos relative to orderByColumn
                    spacePos = orderByColumn.indexOf(' ');
                    if (spacePos == -1)
                    {
                        orderByClause.add(
                                db.ignoreCaseInOrderBy(orderByColumn));
                    }
                    else
                    {
                        orderByClause.add(
                                db.ignoreCaseInOrderBy(
                                        orderByColumn.substring(0, spacePos))
                                + orderByColumn.substring(spacePos));
                    }
                    selectClause.add(
                            db.ignoreCaseInOrderBy(tableName + '.' + columnName));
                }
                else
                {
                    orderByClause.add(orderByColumn);
                }
            }
        }
    }

    /**
     * adds the GroupBy-Columns from the criteria to the query
     * @param criteria the criteria from which the GroupBy-Columns are taken
     * @param query the query to which the GroupBy-Columns should be added
     * @throws TorqueException if the GroupBy-Columns can not be processed
     */
    private static void processGroupBy(
            final Criteria crit,
            final Query query)
            throws TorqueException
    {
        UniqueList groupByClause = query.getGroupByClause();
        UniqueList groupBy = crit.getGroupByColumns();

        // need to allow for multiple group bys
        if (groupBy != null)
        {
            for (int i = 0; i < groupBy.size(); i++)
            {
                String columnName = (String) groupBy.get(i);
                String column = (String) crit.getAsColumns().get(columnName);

                if (column == null)
                {
                    column = columnName;
                }

                if (column.indexOf('.') != -1)
                {
                    groupByClause.add(column);
                }
                else
                {
                    throwMalformedColumnNameException("group by",
                            column);
                }
            }
        }
    }

    /**
     * adds the Having-Columns from the criteria to the query
     * @param criteria the criteria from which the Having-Columns are taken
     * @param query the query to which the Having-Columns should be added
     * @throws TorqueException if the Having-Columns can not be processed
     */
    private static void processHaving(
            final Criteria crit,
            final Query query)
            throws TorqueException
    {
        Criteria.Criterion having = crit.getHaving();
        if (having != null)
        {
            //String groupByString = null;
            query.setHaving(having.toString());
        }
    }

    /**
     * adds a Limit clause to the query if supported by the database
     * @param criteria the criteria from which the Limit and Offset values
     *        are taken
     * @param query the query to which the Limit clause should be added
     * @throws TorqueException if the Database adapter cannot be obtained
     */
    private static void processLimits(
            final Criteria crit,
            final Query query)
            throws TorqueException
    {
        int limit = crit.getLimit();
        int offset = crit.getOffset();

        if (offset > 0 || limit >= 0)
        {
            DB db = Torque.getDB(crit.getDbName());
            db.generateLimits(query, offset, limit);
        }
    }

    /**
     * Throws a TorqueException with the malformed column name error
     * message.  The error message looks like this:<p>
     *
     * <code>
     *     Malformed column name in Criteria [criteriaPhrase]:
     *     '[columnName]' is not of the form 'table.column'
     * </code>
     *
     * @param criteriaPhrase a String, one of "select", "join", or "order by"
     * @param columnName a String containing the offending column name
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public static void throwMalformedColumnNameException(
        final String criteriaPhrase,
        final String columnName)
        throws TorqueException
    {
        StringBuffer sb = new StringBuffer()
                .append("Malformed column name in Criteria ")
                .append(criteriaPhrase)
                .append(": '")
                .append(StringUtils.isEmpty(columnName) ? "<empty>" : columnName)
                .append("' is not of the form 'table.column'");

        throw new TorqueException(sb.toString());
    }

    /**
     * Returns the tablename which can be added to a From Clause.
     * This takes care of any aliases that might be defined.
     * For example, if an alias "a" for the table AUTHOR is defined
     * in the Criteria criteria, getTableNameForFromClause("a", criteria)
     * returns "AUTHOR a".
     * @param tableName the name of a table
     *        or the alias for a table
     * @param criteria a criteria object to resolve a possible alias
     * @return either the tablename itself if tableOrAliasName is not an alias,
     *         or a String of the form "tableName tableOrAliasName"
     *         if tableOrAliasName is an alias for a table name
     */
    public static String getTableNameForFromClause(
            final String tableName,
            final Criteria criteria)
    {
        String shortTableName = getUnqualifiedTableName(tableName);

        // Most of the time, the alias would be for the short name...
        String aliasName = criteria.getTableForAlias(shortTableName);
        if (StringUtils.isEmpty(aliasName))
        {
            // But we should also check the FQN...
            aliasName = criteria.getTableForAlias(tableName);
        }

        if (StringUtils.isNotEmpty(aliasName))
        {
            // If the tables have an alias, add an "<xxx> <yyy> statement"
            // <xxx> AS <yyy> causes problems on oracle
            return new StringBuffer(
                    tableName.length() + aliasName.length() + 1)
                    .append(aliasName)
                    .append(" ")
                    .append(tableName)
                    .toString();
        }

        return tableName;
    }

    /**
     * Checks if the Tablename tableName is already contained in a from clause.
     * If tableName and the tablenames in fromClause are generated by
     * getTablenameForFromClause(String, Criteria), (which they usually are),
     * then different aliases for the same table are treated
     * as different tables: E.g.
     * fromClauseContainsTableName(fromClause, "table_a a") returns false if
     * fromClause contains only another alias for table_a ,
     * e.g. "table_a aa" and the unaliased tablename "table_a".
     * Special case: If tableName is null, true is returned.
     * @param fromClause a list containing only elements of type.
     *        Query.FromElement
     * @param tableName the tablename to check
     * @return if the Tablename tableName is already contained in a from clause.
     *         If tableName is null, true is returned.
     */
    public static boolean fromClauseContainsTableName(
            final UniqueList fromClause,
            final String tableName)
    {
        if (tableName == null)
        {
            // usually this function is called to see if tableName should be
            // added to the fromClause. As null should not be added,
            // true is returned.
            return true;
        }
        for (Iterator it = fromClause.iterator(); it.hasNext();)
        {
            Query.FromElement fromElement
                    = (Query.FromElement) it.next();
            if (tableName.equals(fromElement.getTableName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * adds a table to the from clause of a query, if it is not already
     * contained there.
     * @param tableOrAliasName the name of a table
     *        or the alias for a table
     * @param criteria a criteria object to resolve a possible alias
     * @param query the query where the the tablename should be added
     *        to the from clause
     * @return the table in the from clause which represents the
     *         supplied tableOrAliasName
     */
    private static String addTableToFromClause(
            final String tableName,
            final Criteria criteria,
            Query query)
    {
        String tableNameForFromClause
                = getTableNameForFromClause(tableName, criteria);

        UniqueList queryFromClause = query.getFromClause();

        // it is important that this piece of code is executed AFTER
        // the joins are processed
        if (!fromClauseContainsTableName(
            queryFromClause,
            tableNameForFromClause))
        {
            Query.FromElement fromElement
                    = new Query.FromElement(
                            tableNameForFromClause, null, null);
            queryFromClause.add(fromElement);
        }
        return tableNameForFromClause;
    }

    /**
     * Inner Interface that defines the Callback method for
     * the Table creation loop.
     */
    public interface TableCallback
    {
        /**
         * Callback Method for getTableSet()
         *
         * @param tables The current table name
         * @param key The current criterion key.
         * @param crit The Criteria used in getTableSet()
         */
        void process(Set tables, String key, Criteria crit);
    }

    /**
     * Inner Interface that defines the Callback method for
     * the buildQuery Criterion evaluation
     */
    public interface QueryCallback
    {
        /**
         * The callback for building a query String
         *
         * @param criterion The current criterion
         * @param params The parameter list passed to buildQueryString()
         * @return WHERE SQL fragment for this criterion
         */
        String process(Criterion criterion, List params);
    }

}
