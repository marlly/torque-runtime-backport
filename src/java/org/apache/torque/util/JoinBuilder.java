package org.apache.torque.util;

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

import java.io.Serializable;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.adapter.DB;
import org.apache.torque.map.DatabaseMap;

/**
 * Factored out code that is used to generate Join Code. This code comes
 * from BasePeer and is put here to reduce complexity in the BasePeer class.
 * You should not use the methods here directly!
 *
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public abstract class JoinBuilder
        implements Serializable
{
    /**
     * adds the Joins from the criteria to the query
     * @param criteria the criteria from which the Joins are taken
     * @param query the query to which the Joins should be added
     * @throws TorqueException if the Joins can not be processed
     */
    public static final void processJoins(
            final DB db,
            final DatabaseMap dbMap,
            final Criteria criteria,
            final Query query)
            throws TorqueException
    {
        List criteriaJoins = criteria.getJoins();

        if (criteriaJoins == null)
        {
            return;
        }

        UniqueList queryFromClause = query.getFromClause();
        UniqueList queryWhereClause = query.getWhereClause();

        for (int i = 0; i < criteriaJoins.size(); i++)
        {
            Criteria.Join join = (Criteria.Join) criteriaJoins.get(i);
            String leftColumn = join.getLeftColumn();
            String rightColumn = join.getRightColumn();

            // check if the column names make sense
            if (leftColumn.indexOf('.') == -1)
            {
                SQLBuilder.throwMalformedColumnNameException("join", leftColumn);
            }
            if (rightColumn.indexOf('.') == -1)
            {
                SQLBuilder.throwMalformedColumnNameException("join", rightColumn);
            }

            // get the table names
            // (and the alias names for them if necessary))
            // Also check whether a case insensitive comparison is needed
            int dot = leftColumn.lastIndexOf('.');
            String leftTableName = leftColumn.substring(0, dot);

            leftTableName =
                    SQLBuilder.getTableNameForFromClause(leftTableName, criteria);

            dot = rightColumn.lastIndexOf('.');
            String rightTableName = rightColumn.substring(0, dot);
            String dbTableName
                    = criteria.getTableForAlias(rightTableName);

            if (dbTableName == null)
            {
                dbTableName = rightTableName;
            }

            String columnName = rightColumn.substring(
                    dot + 1,
                    rightColumn.length());

            boolean ignoreCase = (criteria.isIgnoreCase()
                    && (dbMap
                            .getTable(dbTableName)
                            .getColumn(columnName)
                            .getType()
                            instanceof String));

            rightTableName = SQLBuilder.getTableNameForFromClause(
                    rightTableName, criteria);

            // now check the join type and add the join to the
            // appropriate places in the query
            SqlEnum joinType  = join.getJoinType();

            if (joinType == null)
            {
                // Do not treat join as explicit join, but add
                // the join condition to the where clauses
                if (!SQLBuilder.fromClauseContainsTableName(
                            queryFromClause,
                            leftTableName))
                {
                    Query.FromElement fromElement
                            = new Query.FromElement(
                                    leftTableName, null, null);
                    queryFromClause.add(fromElement);
                }
                if (!SQLBuilder.fromClauseContainsTableName(
                            queryFromClause,
                            rightTableName))
                {
                    Query.FromElement fromElement
                            = new Query.FromElement(
                                    rightTableName, null, null);
                    queryFromClause.add(fromElement);
                }
                queryWhereClause.add(
                        SqlExpression.buildInnerJoin(
                                leftColumn, rightColumn, ignoreCase, db));
            }
            else
            {
                // check whether the order of the join must be "reversed"
                // This if the case if the fromClause already contains
                // rightTableName

                if (!SQLBuilder.fromClauseContainsTableName(
                            queryFromClause,
                            rightTableName))
                {
                    if (!SQLBuilder.fromClauseContainsTableName(
                                queryFromClause,
                                leftTableName))
                    {
                        Query.FromElement fromElement
                                = new Query.FromElement(
                                        leftTableName, null, null);
                        queryFromClause.add(fromElement);
                    }

                    Query.FromElement fromElement
                            = new Query.FromElement(
                                    rightTableName, joinType,
                                    SqlExpression.buildInnerJoin(
                                            leftColumn, rightColumn,
                                            ignoreCase, db));
                    queryFromClause.add(fromElement);
                }
                else
                {
                    if (SQLBuilder.fromClauseContainsTableName(
                                queryFromClause,
                                leftTableName))
                    {
                        // We cannot add an explicit join if both tables
                        // are alredy present in the from clause
                        throw new TorqueException(
                                "Unable to create a " + joinType
                                + "because both table names "
                                + leftTableName + " and " + rightTableName
                                + " are already in use. "
                                + "Try to create an(other) alias.");
                    }
                    // now add the join in reverse order
                    // rightTableName must not be added
                    // because it is already present
                    Query.FromElement fromElement
                            = new Query.FromElement(
                                    leftTableName, reverseJoinType(joinType),
                                    SqlExpression.buildInnerJoin(
                                            rightColumn, leftColumn,
                                            ignoreCase, db));
                    queryFromClause.add(fromElement);
                }
            }
        }
    }

    /**
     * returns the reversed Join type, i.e. the join type which would produce
     * the same result if also the joined tables were exchanged:
     * Example:<br />
     * table_a left join table_b <br />
     * produces the same result as  <br />
     * table_b right join table_a<br />
     * So "left join" is the reverse of "right join"
     * @param joinType the join type to be reversed
     * @return the reversed join type
     */
    private static final SqlEnum reverseJoinType(
            final SqlEnum joinType)
    {
        if (SqlEnum.LEFT_JOIN.equals(joinType))
        {
            return SqlEnum.RIGHT_JOIN;
        }
        else if (SqlEnum.RIGHT_JOIN.equals(joinType))
        {
            return SqlEnum.LEFT_JOIN;
        }
        else
        {
            return joinType;
        }
    }
}
