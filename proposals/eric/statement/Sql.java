package org.apache.turbine.util.db.statement;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
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

import java.lang.StringBuffer;
import java.util.List;
import java.util.Iterator;

/**
 * <p>
 * This interface defines methods to construct String fragments of SQL
 * statements.  {@link org.apache.turbine.util.db.statement.BaseSql}
 * provides a base implementation of this interface.  Chaining the
 * methods allows the creation of arbitrarily complex statements.
 * </p>
 *
 * <pre>
 *    getInsert       --> insert into <em>left</em> (<em>item1</em>,...,<em>itemN</em>)
 *    getValues       --> values (<em>item1</em>,...,<em>itemN</em>)
 *    getUpdate       --> update <em>left</em> <em>item1</em>,...,<em>itemN</em>
 *    getSet          --> set <em>left</em>=<em>right</em>
 *    getDelete       --> delete from <em>left</em>
 *    getSelect       --> select <em>item1</em>,<em>item2</em>,...,<em>itemN</em>
 *    getFrom         --> from <em>item1</em>,<em>item2</em>,...,<em>itemN</em>
 *    getWhere        --> where <em>tree</em>
 *    getEqual        --> (<em>left</em> = <em>right</em>)
 *    getNotEqual     --> (<em>left</em> != <em>right</em>)
 *    getGreaterThan  --> (<em>left</em> > <em>right</em>)
 *    getGreaterEqual --> (<em>left</em> >= <em>right</em>)
 *    getLessThan     --> (<em>left</em> < <em>right</em>)
 *    getLessEqual    --> (<em>left</em> <= <em>right</em>)
 *    getNull         --> (<em>item</em> is null)
 *    getNotNull      --> (<em>item</em> is not null)
 *    getIn           --> (<em>left</em> in <em>right</em>)
 *    getNotIn        --> (<em>left</em> not in <em>right</em>)
 *    getAnd          --> (<em>left</em> and <em>right</em>)
 *    getOr           --> (<em>left</em> or <em>right</em>)
 *    getNot          --> (not <em>item</em>)
 *    getAscending    --> <em>item</em> ASC
 *    getDescending   --> <em>item</em> DESC
 *    getOrderBy      --> order by <em>item1</em>,<em>item2</em>,...,<em>itemN</em>
 *    getGroupBy      --> group by <em>item1</em>,<em>item2</em>,...,<em>itemN</em>
 *    getHaving       --> having <em>tree</em>
 *    getCount        --> count(<em>item</em>)
 *    getMin          --> min(<em>item</em>)
 *    getMax          --> max(<em>item</em>)
 *    getAvg          --> avg(<em>item</em>)
 *    getSum          --> sum(<em>item</em>)
 *    getUpper        --> upper(<em>item</em>)
 * </pre>
 *
 * @version $Id$
 */
public interface Sql
{
    /**
     * Constructs a logical comparison using the equals operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>=<em>right</em>)
     */
    public String getEquals(String left, String right);
    
    /**
     * Constructs a logical comparison using the not equals operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>!=<em>right</em>)
     */
    public String getNotEquals(String left, String right);

    /**
     * Constructs a logical comparison using the less than operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&lt;<em>right</em>)
     */
    public String getLessThan(String left, String right);
    
    /**
     * Constructs a logical comparison using the greater than operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&gt;<em>right</em>)
     */
    public String getGreaterThan(String left, String right);
    
    /**
     * Constructs a logical comparison using the less than or equal to
     * operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&lt;=<em>right</em>)
     */
    public String getLessEqual(String left, String right);

    /**
     * Constructs a logical comparison using the greater than or equal
     * to operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&gt;=<em>right</em>)
     */
    public String getGreaterEqual(String left, String right);

    /**
     * Constructs an is null fragment.
     *
     * @param left String the left side of the operator
     * @return (<em>left</em> IS NULL)
     */
    public String getIsNull (String left);

    /**
     * Constructs an is not null fragment.
     *
     * @param left String the left side of the operator
     * @return (<em>left</em> IS NOT NULL)
     */
    public String getIsNotNull (String left);

    /**
     * Constructs an in fragment.
     *
     * @param left String the left side of the operator
     * @param list List the list of items on the right side of the operator
     * @return (<em>left</em> IN (<em>item1</em>,...))
     */
    public String getIn (String left, List list);

    /**
     * Constructs an not in fragment.
     *
     * @param left String the left side of the operator
     * @param list List the list of items on the right side of the operator
     * @return (<em>left</em> NOT IN (<em>item1</em>,...))
     */
    public String getNotIn (String left, List list);

    /**
     * Constructs a logical comparison using the like operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em> LIKE <em>right</em>)
     */
    public String getLike(String left, String right);

    /**
     * Constructs a logical comparison using the and operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em> AND <em>right</em>)
     */
    public String getAnd(String left, String right);

    /**
     * Constructs a logical comparison using the or operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em> OR <em>right</em>)
     */
    public String getOr(String left, String right);

    /**
     * Constructs a logical comparison using the not operator.
     *
     * @param right String the right side of the comparison
     * @return (NOT <em>right</em>)
     */
    public String getNot(String right);

    /**
     * Constructs an ascending fragment.
     *
     * @param left String usually a column name
     * @return <em>left</em> ASC
     */
    public String getAscending(String left);

    /**
     * Constructs a descending fragment.
     *
     * @param left String usually a column name
     * @return <em>left</em> DESC
     */
    public String getDescending(String left);

    /**
     * Constructs an insert fragment.
     *
     * @param left String the table to insert values into
     * @param list List the list of column names
     * @return INSERT INTO <em>left</em> (<em>item1</em>, <em>item2</em>, ..., <em>itemN</em>)
     */
    public String getInsert(String left, List list);

    /**
     * Constructs a values fragment.
     *
     * @param list List the list of values
     * @return VALUES (<em>item1</em>, <em>item2</em>, ..., <em>itemN</em>)
     */
    public String getValues(List list);

    /**
     * Constructs an update fragment.
     *
     * @param item String the table to be updated
     * @param list List a list of set statements (see below)
     * @return UPDATE left <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     * where <em>item</em> is a set fragment (see below)
     */
    public String getUpdate(String item, List list);

    /**
     * Constructs a set statement.
     *
     * @param left String the column to be set
     * @param right String the value to be assigned
     * @return SET <em>middle</em>=<em>value</em>
     */
    public String getSet(String left, String right);

    /**
     * Constructs a delete fragment.
     *
     * @param left String the table from which rows will be deleted
     * @return DELETE FROM <em>left</em>
     */
    public String getDelete(String left);

    /**
     * Constructs a select fragment.
     *
     * @param list List the list of items
     * @return SELECT <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getSelect(List list);

    /**
     * Constructs a from fragment.
     *
     * @param list List the list of items
     * @return FROM <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getFrom(List list);

    /**
     * Constructs a where fragment.
     *
     * @param tree String comparisons for the where clause.
     * see getAnd(), getOr(), getEqual(), getLessThan(), etc
     * for methods to help construct these comparisons
     * @return WHERE <em>tree</em>
     */
    public String getWhere(String middle);

    /**
     * Constructs a having fragment.
     *
     * @param tree String comparisons for the where clause.
     * see getAnd(), getOr(), getEqual(), getLessThan(), etc
     * for methods to help construct these comparisons
     * @return HAVING <em>tree</em>
     */
    public String getHaving(String middle);

    /**
     * Constructs an order by fragment.
     *
     * @param list List the list of items
     * @return ORDER BY <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getOrderBy(List list);

    /**
     * Constructs a group by fragment.
     *
     * @param list List the list of items
     * @return GROUP BY <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getGroupBy(List list);

    /**
     * Constructs a count function.
     *
     * @param middle String the column to be counted
     * @return COUNT(<em>middle</em)
     */
    public String getCount(String middle);

    /**
     * Constructs a min function.
     *
     * @param middle String the column to be searched for its minimum value
     * @return MIN(<em>middle</em)
     */
    public String getMin(String middle);

    /**
     * Constructs a max function.
     *
     * @param middle String the column to be searched for its maximum value
     * @return MAX(<em>middle</em)
     */
    public String getMax(String middle);

    /**
     * Constructs a avg function.
     *
     * @param middle String the column to be averaged
     * @return AVG(<em>middle</em)
     */
    public String getAvg(String middle);

    /**
     * Constructs a sum function.
     *
     * @param middle String the column to be summed
     * @return SUM(<em>middle</em)
     */
    public String getSum(String middle);

    /**
     * Constructs an upper function.
     *
     * @param middle String the column to be averaged
     * @return UPPER(<em>middle</em)
     */
    public String getUpper(String middle);

    /**
     * Quotes and escapes raw text for placement in a SQL expression.
     * For simplicity, the text is assumed to be neither quoted nor
     * escaped.
     *
     * <p>
     * raw string:  <em>O'Malley's Can't be beat!</em><br/>
     * qutoed and escaped: <em>'O''Malley''s Can''t be beat!'</em><br/>
     * </p>
     *
     * @param rawText The <i>unquoted</i>, <i>unescaped</i> text to process.
     * @return Quoted and escaped text.
     */
    public String quoteAndEscapeText(String rawText);
}
