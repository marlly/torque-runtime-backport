package org.apache.turbine.util.db.statement;

/*
 * Copyright 2001,2004 The Apache Software Foundation.
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

import java.lang.StringBuffer;
import java.util.List;
import java.util.Iterator;

/**
 * <p>
 * This class contains default methods to construct SQL statements
 * using method calls.  Subclasses should specialize the methods to
 * the SQL dialect of a specific database.
 * </p>
 *
 * <p>
 * This class defines methods that construct String fragments of SQL
 * statements.  Chaining the methods allows the creation of
 * arbitrarily complex statements.
 * </p>
 *
 * <pre>
 *    getInsert       --> insert into <em>left</em> (<em>item1</em>,...,<em>itemN</em>)
 *    getValues       --> values (<em>item1</em>,...,<em>itemN</em>)
 *    getUpdate       --> update <em>left</em> <em>item1</em>,...,<em>itemN</em>
 *    getSet          --> set <em>left</em>=<em>right</em>
 *    getDelete       --> delete from <em>left</em>
 *    getSelect       --> select <em>item1</em>,...,<em>itemN</em>
 *    getSelectDistinct --> select distinct <em>item1</em>,...,<em>itemN</em>
 *    getFrom         --> from <em>item1</em>,<em>item2</em>,...,<em>itemN</em>
 *    getWhere        --> where <em>tree</em>
 *    getEquals       --> (<em>left</em> = <em>right</em>)
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
 *    getOrderBy      --> order by <em>item1</em>...,<em>itemN</em>
 *    getGroupBy      --> group by <em>item1</em>...,<em>itemN</em>
 *    getHaving       --> having <em>tree</em>
 *    getCount        --> count(<em>item</em>)
 *    getMin          --> min(<em>item</em>)
 *    getMax          --> max(<em>item</em>)
 *    getAvg          --> avg(<em>item</em>)
 *    getSum          --> sum(<em>item</em>)
 *    getUpper        --> upper(<em>item</em>)
 * </pre>
 *
 * <p>
 *   Stored procedures can also be implemented.  Extend this class in
 *   your own custom database adaptor.  Add methods to construct SQL
 *   fragments for your stored procedures.  The implementations for
 *   functions (such as getCount(), getSum()...)  would be useful
 *   models to follow for your stored procedures.
 * </p>
 */
public class BaseSql
       implements Sql
{
    //
    // leading/trailing spaces are important in the key words
    //
    public static String EMPTY           = "";
    public static String SPACE           = " ";
    public static String INSERT          = "INSERT INTO ";
    public static String VALUES          = " VALUES ";
    public static String UPDATE          = "UPDATE ";
    public static String DELETE          = "DELETE FROM ";
    public static String SELECT          = "SELECT ";
    public static String SELECT_DISTINCT = "SELECT DISTINCT ";
    public static String SET             = " SET ";
    public static String FROM            = " FROM ";
    public static String WHERE           = " WHERE ";
    public static String ORDER_BY        = " ORDER BY ";
    public static String GROUP_BY        = " GROUP BY ";
    public static String HAVING          = " HAVING ";
    public static String ASC             = " ASC";
    public static String DESC            = " DESC";
    public static String OPEN_PAREN      = " (";
    public static String CLOSE_PAREN     = ") ";
    public static String EQUALS          = " = ";
    public static String NOT_EQUALS      = " != ";
    public static String GREATER_THAN    = " > ";
    public static String LESS_THAN       = " < ";
    public static String GREATER_EQUAL   = " >= ";
    public static String LESS_EQUAL      = " <= ";
    public static String IS_NULL         = " IS NULL";
    public static String IS_NOT_NULL     = " IS NOT NULL";
    public static String IN              = " IN ";
    public static String NOT_IN          = " NOT IN ";
    public static String LIKE            = " LIKE ";
    public static String AND             = " AND ";
    public static String OR              = " OR ";
    public static String NOT             = " NOT ";
    public static String COUNT           = "COUNT";
    public static String MIN             = "MIN";
    public static String MAX             = "MAX";
    public static String AVG             = "AVG";
    public static String SUM             = "SUM";
    public static String UPPER           = "UPPER";
    public static String COMMA           = ", ";
    private static final char SINGLE_QUOTE = '\'';
    
    /**
     * The workhorse used by several other methods to construct a
     * String of the form [left][middle][right].
     *
     * this is useful for at least the following SQL fragments:
     * <dl>
     * <table>
     * <tr><td>where (foo_id=25)</td>
     * <td><em>middle:</em> foo_id=25</td></tr>
     * <tr><td>set (foo_id=25)</td>
     * <td><em>middle:</em> foo_id=25</td></tr>
     * <tr><td>table.column asc</td>
     * <td><em>left is EMPTY, middle:</em> "table.column"</td></tr>
     * <tr><td>upper (table.column)</td>
     * <td><em>middle:</em> table.column</td></tr>
     * <tr><td>count(*)</td>
     * <td><em>middle:</em> *</td></tr>
     * <tr><td>(table.column=15)</td>
     * <td><em>middle:</em>table.column=15</td></tr>
     * <tr><td>table.column=15</td>
     * <td><em>middle:</em> =</td></tr>
     * <tr><td>uppercase(table.column) like 'FOO%'</td>
     * <td><em>middle:</em> like</td></tr>
     * <tr><td>(table.column>10) AND (table.column<=20)</td>
     * <td><em>middle:</em> AND</td></tr>
     * <tr><td>set table.column=25</td>
     * <td><em>middle:</em> table.column=25, <em>right is EMPTY</em></td></tr>
     * </table>
     * </dl>
     *
     * @param left the String prefix for the object
     * @param middle String the thing in the middle
     * @param right the String suffix for the object
     * @return [left][middle][right]
     */

    protected String leftMiddleRight(String left, 
                                     String middle,
                                     String right)
    {
        StringBuffer sb = new StringBuffer(left.length()
                                           +right.length()
                                           +middle.length());
        leftMiddleRight(sb, left, middle, right);
        return sb.toString();
    }

    protected String leftMiddleRight(StringBuffer sb, 
                                     String left, 
                                     String middle,
                                     String right)
    {
        sb.append(left).append(middle).append(right);
        return sb.toString();
    }

    /**
     * The workhorse used by several other methods to construct a
     * String of the form
     * [left][item1][connector][item2][connector]...[itemN][right].
     *
     * this is useful for at least the following SQL fragments:
     * insert (columnA, columnB, columnC)
     * values ('one', 'two', 'three')
     * (the following examples use an EMPTY right argument)
     *     select columnA, sum(columnB), avg(columnB)
     *     from table1 t1, table2 t2
     *     order by columnC desc, columnA asc
     *     group by columnA
     *
     * @param left String the left side of the list
     * @param right String the right side of the list
     * @param list List a list containing the items
     * @param connector String the list delimiter
     * @return [left][item1][connector][item2][connector]...[itemN][right]
     */
    protected String leftRightListConnector(String left, 
                                            String right,
                                            List list,
                                            String connector)
    {
        //Fudging the initial size for the string buffer by
        //multiplying the number of items in the list by the
        //length of the connector + 7
        //
        //This could be done precisely by looping through the
        //list and converting each item to a string and summing
        //their lengths, but would that would be any less
        //expensive than letting the StringBuffer resize itself?
        int size = left.length() + right.length() + 
                   ((connector.length() + 7) * list.size());
        StringBuffer sb = new StringBuffer(size);
        leftRightListConnector(sb, left, right, list, connector);
        return sb.toString();
    }

    protected void leftRightListConnector(StringBuffer sb, 
                                          String left, 
                                          String right,
                                          List list,
                                          String connector)
    {
        Iterator listIterator = list.iterator();
        sb.append(left);
        if (listIterator.hasNext())
        {
            sb.append(listIterator.next());
        }
        while (listIterator.hasNext())
        {
            sb.append(connector)
                .append(listIterator.next());
        }
        sb.append(right);
    }

    /**
     * Constructs a logical comparison using the equals operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>=<em>right</em>)
     */
    public String getEquals(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, EQUALS, right),
                               CLOSE_PAREN);
    }

    /**
     * If right is not null, constructs an equals comparison 
     * othwerise, construts a isNull comparison
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>=<em>right</em>)
     */
    public String getSafeEquals(String left, String right)
    {
        if (right != null)
        {
            return getEquals(left, right);
        }
        else
        {
            return getIsNull(left);
        }
    }

    /**
     * Constructs a logical comparison using the not equals operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>!=<em>right</em>)
     */
    public String getNotEquals(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, NOT_EQUALS, right),
                               CLOSE_PAREN);
    }

    /**
     * If right is not null, constructs a not equals comparison 
     * othwerise, construts a isNotNull comparison
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>=<em>right</em>)
     */
    public String getSafeNotEquals(String left, String right)
    {
        if (right != null)
        {
            return getNotEquals(left, right);
        }
        else
        {
            return getIsNotNull(left);
        }
    }

    /**
     * Constructs a logical comparison using the less than operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&lt;<em>right</em>)
     */
    public String getLessThan(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left,  LESS_THAN, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the greater than operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&gt;<em>right</em>)
     */
    public String getGreaterThan(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, GREATER_THAN, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the less than or equal to
     * operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&lt;=<em>right</em>)
     */
    public String getLessEqual(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, LESS_EQUAL, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the greater than or equal
     * to operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em>&gt;=<em>right</em>)
     */
    public String getGreaterEqual(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, GREATER_EQUAL, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs an is null fragment.
     *
     * @param left String the left side of the operator
     * @return (<em>left</em> IS NULL)
     */
    public String getIsNull (String left)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, IS_NULL, EMPTY),
                               CLOSE_PAREN);
    }

    /**
     * Constructs an is not null fragment.
     *
     * @param left String the left side of the operator
     * @return (<em>left</em> IS NOT NULL)
     */
    public String getIsNotNull (String left)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, IS_NOT_NULL, EMPTY),
                               CLOSE_PAREN);
    }

    /**
     * Constructs an in fragment.
     *
     * @param left String the left side of the operator
     * @param list List the list of items on the right side of the operator
     * @return (<em>left</em> IN (<em>item1</em>,...))
     */
    public String getIn (String left, List list)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(
                                   left,
                                   IN,
                                   leftRightListConnector(OPEN_PAREN,
                                                          CLOSE_PAREN,
                                                          list,
                                                          COMMA)),
                               CLOSE_PAREN);
    }

    /**
     * Constructs an not in fragment.
     *
     * @param left String the left side of the operator
     * @param list List the list of items on the right side of the operator
     * @return (<em>left</em> NOT IN (<em>item1</em>,...))
     */
    public String getNotIn (String left, List list)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(
                                   left, 
                                   NOT_IN,
                                   leftRightListConnector(OPEN_PAREN,
                                                          CLOSE_PAREN,
                                                          list,
                                                          COMMA)),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the like operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em> LIKE <em>right</em>)
     */
    public String getLike(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, LIKE, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the and operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em> AND <em>right</em>)
     */
    public String getAnd(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, AND, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the or operator.
     *
     * @param left String the left side of the comparison
     * @param right String the right side of the comparison
     * @return (<em>left</em> OR <em>right</em>)
     */
    public String getOr(String left, String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(left, OR, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs a logical comparison using the not operator.
     *
     * @param right String the right side of the comparison
     * @return (NOT <em>right</em>)
     */
    public String getNot(String right)
    {
        return leftMiddleRight(OPEN_PAREN,
                               leftMiddleRight(EMPTY, NOT, right),
                               CLOSE_PAREN);
    }

    /**
     * Constructs an ascending fragment.
     *
     * @param left String usually a column name
     * @return <em>left</em> ASC
     */
    public String getAscending(String left)
    {
        return leftMiddleRight(left, ASC, EMPTY);
    }

    /**
     * Constructs a descending fragment.
     *
     * @param left String usually a column name
     * @return <em>left</em> DESC
     */
    public String getDescending(String left)
    {
        return leftMiddleRight(left, DESC, EMPTY);
    }

    /**
     * Constructs an insert fragment.
     *
     * @param left String the table to insert values into
     * @param list List the list of column names
     * @return INSERT INTO <em>left</em> (<em>item1</em>, ..., <em>itemN</em>)
     */
    public String getInsert(String left, List list)
    {
        return leftMiddleRight(leftMiddleRight(INSERT,
                                               left,
                                               SPACE),
                               leftRightListConnector(OPEN_PAREN,
                                                      CLOSE_PAREN,
                                                      list,
                                                      COMMA),
                               EMPTY);
    }

    /**
     * Constructs a values fragment.
     *
     * @param list List the list of values
     * @return VALUES (<em>item1</em>, <em>item2</em>, ..., <em>itemN</em>)
     */
    public String getValues(List list)
    {
        return leftMiddleRight(VALUES,
                               leftRightListConnector(OPEN_PAREN,
                                                      CLOSE_PAREN,
                                                      list,
                                                      COMMA),
                               EMPTY);
    }

    /**
     * Constructs an update fragment.
     *
     * @param item String the table to be updated
     * @param list List a list of set statements (see below)
     * @return UPDATE left <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     * where <em>item</em> is a set fragment (see below)
     */
    public String getUpdate(String item, List list)
    {
        return leftMiddleRight(UPDATE,
                               item,
                               leftRightListConnector(SPACE,
                                                      EMPTY,
                                                      list,
                                                      COMMA));
    }

    /**
     * Constructs a set statement.
     *
     * @param left String the column to be set
     * @param right String the value to be assigned
     * @return SET <em>middle</em>=<em>value</em>
     */
    public String getSet(String left, String right)
    {
        return leftMiddleRight(SET,
                               leftMiddleRight(left,
                                               EQUALS,
                                               right),
                               EMPTY);
    }

    /**
     * Constructs a set statement.
     *
     * @param list List the list of items
     * @return SET <em>item1</em>, ... <em>itemN</em>
     */
    public String getSet(List list)
    {
       return leftRightListConnector(SET, EMPTY, list, COMMA);
    }

    /**
     * Constructs a delete fragment.
     *
     * @param left String the table from which rows will be deleted
     * @return DELETE FROM <em>left</em>
     */
    public String getDelete(String left)
    {
        return leftMiddleRight(DELETE, left, EMPTY);
    }

    /**
     * Constructs a select fragment.
     *
     * @param list List the list of items
     * @return SELECT <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getSelect(List list)
    {
        return leftRightListConnector(SELECT, EMPTY, list, COMMA);
    }

    /**
     * Constructs a select distinct fragment.
     *
     * @param list List the list of items
     * @return SELECT DISTINCT <em>item1</em>, ..., <em>itemN</em>
     */
    public String getSelectDistinct(List list)
    {
        return leftRightListConnector(SELECT_DISTINCT, EMPTY, list, COMMA);
    }

    /**
     * Constructs a from fragment.
     *
     * @param list List the list of items
     * @return FROM <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getFrom(List list)
    {
        return leftRightListConnector(FROM, EMPTY, list, COMMA);
    }

    /**
     * Constructs a from fragment.
     *
     * @param tableName the table name from which to select 
     * @return FROM <em>tableName</em>
     */
    public String getFrom(String tableName)
    {
        return leftMiddleRight(FROM, tableName, EMPTY);
    }

    /**
     * Constructs a where fragment.
     *
     * @param tree String comparisons for the where clause.
     * see getAnd(), getOr(), getEquals(), getLessThan(), etc
     * for methods to help construct these comparisons
     * @return WHERE <em>tree</em>
     */
    public String getWhere(String middle)
    {
        return leftMiddleRight(WHERE, middle, EMPTY);
    }

    /**
     * Constructs a having fragment.
     *
     * @param tree String comparisons for the where clause.
     * see getAnd(), getOr(), getEquals(), getLessThan(), etc
     * for methods to help construct these comparisons
     * @return HAVING <em>tree</em>
     */
    public String getHaving(String middle)
    {
        return leftMiddleRight(HAVING, middle, EMPTY);
    }

    /**
     * Constructs an order by fragment.
     *
     * @param list List the list of items
     * @return ORDER BY <em>item1</em>, ..., <em>itemN</em>
     */
    public String getOrderBy(List list)
    {
        return leftRightListConnector(ORDER_BY, EMPTY, list, COMMA);
    }

    /**
     * Constructs an order by fragment.
     *
     * @param fieldName field name of the order by column 
     * @return ORDER BY <em>fieldName</em>
     */
    public String getOrderBy(String fieldName)
    {
        return leftMiddleRight(ORDER_BY, fieldName, EMPTY);
    }

    /**
     * Constructs a group by fragment.
     *
     * @param list List the list of items
     * @return GROUP BY <em>item1</em>, <em>item2</em>, ..., <em>itemN</em>
     */
    public String getGroupBy(List list)
    {
        return leftRightListConnector(GROUP_BY, EMPTY, list, COMMA);
    }

    /**
     * Constructs a count function.
     *
     * @param middle String the column to be counted
     * @return COUNT(<em>middle</em>)
     */
    public String getCount(String middle)
    {
        return leftMiddleRight(COUNT,
                               leftMiddleRight(OPEN_PAREN,
                                               middle,
                                               CLOSE_PAREN),
                               EMPTY);
    }

    /**
     * Constructs a min function.
     *
     * @param middle String the column to be searched for its minimum value
     * @return MIN(<em>middle</em>)
     */
    public String getMin(String middle)
    {
        return leftMiddleRight(MIN,
                               leftMiddleRight(OPEN_PAREN,
                                               middle,
                                               CLOSE_PAREN),
                               EMPTY);
    }

    /**
     * Constructs a max function.
     *
     * @param middle String the column to be searched for its maximum value
     * @return MAX(<em>middle</em>)
     */
    public String getMax(String middle)
    {
        return leftMiddleRight(MAX,
                               leftMiddleRight(OPEN_PAREN,
                                               middle,
                                               CLOSE_PAREN),
                               EMPTY);
    }

    /**
     * Constructs a avg function.
     *
     * @param middle String the column to be averaged
     * @return AVG(<em>middle</em>)
     */
    public String getAvg(String middle)
    {
        return leftMiddleRight(AVG,
                               leftMiddleRight(OPEN_PAREN,
                                               middle,
                                               CLOSE_PAREN),
                               EMPTY);
    }

    /**
     * Constructs a sum function.
     *
     * @param middle String the column to be summed
     * @return SUM(<em>middle</em>)
     */
    public String getSum(String middle)
    {
        return leftMiddleRight(SUM,
                               leftMiddleRight(OPEN_PAREN,
                                               middle,
                                               CLOSE_PAREN),
                               EMPTY);
    }

    /**
     * Constructs an upper function.
     *
     * @param middle String the column to be averaged
     * @return UPPER(<em>middle</em>)
     */
    public String getUpper(String middle)
    {
        return leftMiddleRight(UPPER,
                               leftMiddleRight(OPEN_PAREN,
                                               middle,
                                               CLOSE_PAREN),
                               EMPTY);
    }

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
    public String quoteAndEscapeText(String rawText)
    {
        StringBuffer buf = new StringBuffer( (int)(rawText.length() * 1.1) );

        char[] data = rawText.toCharArray();
        buf.append(SINGLE_QUOTE);
        for (int i = 0; i < data.length; i++)
        {
            switch (data[i])
            {
            case SINGLE_QUOTE:
                buf.append(SINGLE_QUOTE).append(SINGLE_QUOTE);
                break;
            // Some databases need to have backslashes escaped.
            // Subclasses can override this method to include
            // this case if appropriate.
            /*
                case BACKSLASH:
                buf.append(BACKSLASH).append(BACKSLASH);
                break;
            */
            default:
                buf.append(data[i]);
            }
        }
        buf.append(SINGLE_QUOTE);

        return buf.toString();
    }
}
