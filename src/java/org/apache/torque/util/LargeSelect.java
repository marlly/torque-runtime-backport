package org.apache.torque.util;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Turbine" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.DataSetException;

/**
 * This class can be used to retrieve a large result set from a database query.
 * The query is started and then rows are returned a page at a time.  The <code>
 * LargeSelect</code> is meant to be placed into the Session or User.Temp, so
 * that it can be used in response to several related requests.  Note that in
 * order to use <code>LargeSelect</code> you need to be willing to accept the
 * fact that the result set may become inconsistent with the database if updates
 * are processed subsequent to the queries being executed.  Specifying a memory
 * page limit of 1 will give you a consistent view of the records but the totals
 * may not be accurate and the performance will be terrible.  In most cases
 * the potential for inconsistencies data should not cause any serious problems
 * and performance should be pretty good (but read on for further warnings).
 *
 * <p>The idea here is that the full query result would consume too much memory
 * and if displayed to a user the page would be too long to be useful.  Rather
 * than loading the full result set into memory, a window of data (the memory
 * limit) is loaded and retrieved a page at a time.  If a request occurs for
 * data that falls outside the currently loaded window of data then a new query
 * is executed to fetch the required data.  Performance is optimized by
 * starting a thread to execute the database query and fetch the results.  This
 * will perform best when paging forwards through the data, but a minor
 * optimization where the window is moved backwards by two rather than one page
 * is included for when a user pages past the beginning of the window.
 *
 * <p>As the query is performed in in steps, it is often the case that the total
 * number of records and pages of data is unknown.  <code>LargeSelect</code>
 * provides various methods for indicating how many records and pages it is
 * currently aware of and for presenting this information to users.
 *
 * <p><code>LargeSelect</code> utilises the <code>Criteria</code> methods
 * <code>setOffset()</code> and <code>setLimit()</code> to limit the amount of
 * data retrieved from the database - these values are either passed through to
 * the DBMS when supported (efficient with the caveat below) or handled by
 * the Village API when it is not (not so efficient).  At time of writing
 * <code>Criteria</code> will only pass the offset and limit through to MySQL
 * and PostgreSQL (with a few changes to <code>DBOracle</code> and <code>
 * BasePeer</code> Oracle support can be implemented by utilising the <code>
 * rownum</code> pseudo column).
 *
 * <p>As <code>LargeSelect</code> must re-execute the query each time the user
 * pages out of the window of loaded data, you should consider the impact of
 * non-index sort orderings and other criteria that will require the DBMS to
 * execute the entire query before filtering down to the offset and limit either
 * internally or via Village.
 *
 * <p>The memory limit defaults to 5 times the page size you specify, but
 * alternative constructors and the class method <code>setMemoryPageLimit()
 * </code> allow you to override this for a specific instance of
 * <code>LargeSelect</code> or future instances respectively.
 *
 * <p>Some of the constructors allow you to specify the name of the class to use
 * to build the returnd rows.  This works by using reflection to find <code>
 * addSelectColumns(Criteria)</code> and <code>populateObjects(List)</code>
 * methods to add the necessary select columns to the criteria (only if it
 * doesn't already contain any) and to convert query results from Village
 * <code>Record</code> objects to a class defined within the builder class.
 * This allows you to use any of the Torque generated Peer classes, but also
 * makes it fairly simple to construct business object classes that can be used
 * for this purpose (simply copy and customise the <code>addSelectColumns()
 * </code>, <code>populateObjects()</code>, <code>row2Object()</code> and <code>
 * populateObject()</code> methods from an existing Peer class).
 *
 * <p>Typically you will create a <code>LargeSelect</code> using your <code>
 * Criteria</code> (perhaps created from the results of a search parameter
 * page), page size, memory page limit and return class name (for which you may
 * have defined a business object class before hand) and place this in user.Temp
 * thus:
 *
 * <pre>
 *     data.getUser().setTemp("someName", largeSelect);
 * </pre>
 *
 * <p>In your template you will then use something along the lines of:
 *
 * <pre>
 *    #set ($largeSelect = $data.User.getTemp("someName"))
 *    #set ($searchop = $data.Parameters.getString("searchop"))
 *    #if ($searchop.equals("prev"))
 *      #set ($recs = $largeSelect.PreviousResults)
 *    #else
 *      #if ($searchop.equals("goto"))
 *        #set ($recs
 *                = $largeSelect.getPage($data.Parameters.getInt("page", 1)))
 *      #else
 *        #set ($recs = $largeSelect.NextResults)
 *      #end
 *    #end
 * </pre>
 *
 * <p>...to move through the records.  <code>LargeSelect</code> implements a
 * number of convenience methods that make it easy to add all of the necessary
 * bells and whistles to your template.
 *
 * @author <a href="mailto:john.mcnally@clearink.com">John D. McNally</a>
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @version $Id$
 */
public class LargeSelect implements Runnable
{
    /** The number of records that a page consists of.  */
    private int pageSize;
    /** The maximum number of records to maintain in memory. */
    private int memoryLimit;

    /** The record number of the first record in memory. */
    private int blockBegin = 0;
    /** The record number of the last record in memory. */
    private int blockEnd;
    /** How much of the memory block is currently occupied with result data. */
    private volatile int currentlyFilledTo = -1;

    /** The SQL query that this <code>LargeSelect</code> represents. */
    private String query;
    /** The database name to get from Torque. */
    private String dbName;
    /** Used to retrieve query results from Village. */
    private QueryDataSet qds = null;

    /** The memory store of records. */
    private List results = null;

    /** The thread that executes the query. */
    private Thread thread = null;
    /**
     * A flag used to kill the thread when the currently executing query is no
     * longer required.
     */
    private volatile boolean killThread = false;
    /**
     * An indication of whether or not the current query has completed
     * processing.
     */
    private volatile boolean queryCompleted = false;
    /**
     * An indication of whether or not the totals (records and pages) are at
     * their final values.
     */
    private boolean totalsFinalized = false;

    /** The cursor position in the result set. */
    private int position;
    /** The total number of pages known to exist. */
    private int totalPages = -1;
    /** The total number of records known to exist. */
    private int totalRecords = 0;
    /** The number of the page that was last retrieved. */
    private int currentPageNumber = 0;

    /** The criteria used for the query. */
    private Criteria criteria = null;
    /** The last page of results that were returned. */
    private List lastResults;

    /**
     * The class that is possibly used to construct the criteria and used
     * to transform the Village Records into the desired OM or business objects.
     */
    private Class returnBuilderClass = null;
    /**
     * A reference to the method in the return builder class that will
     * convert the Village Records to the desired class.
     */
    private Method populateObjectsMethod = null;

    /**
     * The default value ("&gt;") used to indicate that the total number of
     * records or pages is unknown. You can use <code>setMoreIndicator()</code>
     * to change this to whatever value you like (e.g. "more than").
     */
    public static final String DEFAULT_MORE_INDICATOR = "&gt;";

    private static String moreIndicator = DEFAULT_MORE_INDICATOR;

    /**
     * The default value for the maximum number of pages of data to be retained
     * in memory - you can provide your own default value using
     * <code>setMemoryPageLimit()</code>.
     */
    public static final int DEFAULT_MEMORY_LIMIT_PAGES = 5;

    private static int memoryPageLimit = DEFAULT_MEMORY_LIMIT_PAGES;

    /** A place to store search parameters that relate to this query. */
    private Hashtable params = null;

    /** Logging */
    private static Log log = LogFactory.getLog(LargeSelect.class);

    /**
     * Creates a LargeSelect whose results are returned as a <code>List</code>
     * containing a maximum of <code>pageSize</code> Village <code>Record</code>
     * objects at a time, maintaining a maximum of
     * <code>LargeSelect.memoryPageLimit</code> pages of results in memory.
     *
     * @param criteria object used by BasePeer to build the query.  In order to
     * allow this class to utilise database server implemented offsets and
     * limits (when available), the provided criteria must not have any limit or
     * offset defined.
     * @param pageSize number of rows to return in one block.
     * @throws IllegalArgumentException if <code>criteria</code> uses one or
     * both of offset and limit, or if <code>pageSize</code> is less than 1;
     */
    public LargeSelect(Criteria criteria, int pageSize)
            throws IllegalArgumentException
    {
        this(criteria, pageSize, LargeSelect.memoryPageLimit);
    }

    /**
     * Creates a LargeSelect whose results are returned as a <code>List</code>
     * containing a maximum of <code>pageSize</code> Village <code>Record</code>
     * objects at a time, maintaining a maximum of <code>memoryPageLimit</code>
     * pages of results in memory.
     *
     * @param criteria object used by BasePeer to build the query.  In order to
     * allow this class to utilise database server implemented offsets and
     * limits (when available), the provided criteria must not have any limit or
     * offset defined.
     * @param pageSize number of rows to return in one block.
     * @param memoryPageLimit maximum number of pages worth of rows to be held
     * in memory at one time.
     * @throws IllegalArgumentException if <code>criteria</code> uses one or
     * both of offset and limit, or if <code>pageSize</code> or
     * <code>memoryLimitPages</code> are less than 1;
     */
    public LargeSelect(Criteria criteria, int pageSize, int memoryPageLimit)
            throws IllegalArgumentException
    {
        init(criteria, pageSize, memoryPageLimit);
    }

    /**
     * Creates a LargeSelect whose results are returned as a <code>List</code>
     * containing a maximum of <code>pageSize</code> objects of the type
     * defined within the class named <code>returnBuilderClassName</code> at a
     * time, maintaining a maximum of <code>LargeSelect.memoryPageLimit</code>
     * pages of results in memory.
     *
     * @param criteria object used by BasePeer to build the query.  In order to
     * allow this class to utilise database server implemented offsets and
     * limits (when available), the provided criteria must not have any limit or
     * offset defined.  If the criteria does not include the definition of any
     * select columns the <code>addSelectColumns(Criteria)</code> method of
     * the class named as <code>returnBuilderClassName</code> will be used to
     * add them.
     * @param pageSize number of rows to return in one block.
     * @param returnBuilderClassName The name of the class that will be used to
     * build the result records (may implement <code>addSelectColumns(Criteria)
     * </code> and must implement <code>populateObjects(List)</code>).
     * @throws IllegalArgumentException if <code>criteria</code> uses one or
     * both of offset and limit, if <code>pageSize</code> is less than 1, or if
     * problems are experienced locating and invoking either one or both of
     * <code>addSelectColumns(Criteria)</code> and <code> populateObjects(List)
     * </code> in the class named <code>returnBuilderClassName</code>.
     */
    public LargeSelect(
            Criteria criteria,
            int pageSize,
            String returnBuilderClassName)
            throws IllegalArgumentException
    {
        this(
            criteria,
            pageSize,
            LargeSelect.memoryPageLimit,
            returnBuilderClassName);
    }

    /**
     * Creates a LargeSelect whose results are returned as a <code>List</code>
     * containing a maximum of <code>pageSize</code> objects of the type
     * defined within the class named <code>returnBuilderClassName</code> at a
     * time, maintaining a maximum of <code>memoryPageLimit</code> pages of
     * results in memory.
     *
     * @param criteria object used by BasePeer to build the query.  In order to
     * allow this class to utilise database server implemented offsets and
     * limits (when available), the provided criteria must not have any limit or
     * offset defined.  If the criteria does not include the definition of any
     * select columns the <code>addSelectColumns(Criteria)</code> method of
     * the class named as <code>returnBuilderClassName</code> will be used to
     * add them.
     * @param pageSize number of rows to return in one block.
     * @param memoryPageLimit maximum number of pages worth of rows to be held
     * in memory at one time.
     * @param returnBuilderClassName The name of the class that will be used to
     * build the result records (may implement <code>addSelectColumns(Criteria)
     * </code> and must implement <code>populateObjects(List)</code>).
     * @throws IllegalArgumentException if <code>criteria</code> uses one or
     * both of offset and limit, if <code>pageSize</code> or <code>
     * memoryLimitPages</code> are less than 1, or if problems are experienced
     * locating and invoking either one or both of <code>
     * addSelectColumns(Criteria)</code> and <code> populateObjects(List)</code>
     * in the class named <code>returnBuilderClassName</code>.
     */
    public LargeSelect(
            Criteria criteria,
            int pageSize,
            int memoryPageLimit,
            String returnBuilderClassName)
            throws IllegalArgumentException
    {
        try
        {
            this.returnBuilderClass = Class.forName(returnBuilderClassName);

            // Add the select columns if necessary.
            if (criteria.getSelectColumns().size() == 0)
            {
                Class[] argTypes = { Criteria.class };
                Method selectColumnAdder =
                    returnBuilderClass.getMethod("addSelectColumns", argTypes);
                Object[] theArgs = { criteria };
                selectColumnAdder.invoke(returnBuilderClass.newInstance(),
                        theArgs);
            }

            // Locate the populateObjects() method - this will be used later
            Class[] argTypes = { List.class };
            populateObjectsMethod =
                returnBuilderClass.getMethod("populateObjects", argTypes);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(
                    "The class named as returnBuilderClassName does not "
                    + "provide the necessary facilities - see javadoc.");
        }

        init(criteria, pageSize, memoryPageLimit);
    }

    /**
     * Called by the constructors to start the query.
     *
     * @param criteria Object used by <code>BasePeer</code> to build the query.
     * In order to allow this class to utilise database server implemented
     * offsets and limits (when available), the provided criteria must not have
     * any limit or offset defined.
     * @param pageSize number of rows to return in one block.
     * @param memoryLimitPages maximum number of pages worth of rows to be held
     * in memory at one time.
     * @throws IllegalArgumentException if <code>criteria</code> uses one or
     * both of offset and limit and if <code>pageSize</code> or
     * <code>memoryLimitPages</code> are less than 1;
     */
    private void init(Criteria criteria, int pageSize, int memoryLimitPages)
            throws IllegalArgumentException
    {
        if (criteria.getOffset() != 0 || criteria.getLimit() != -1)
        {
            throw new IllegalArgumentException(
                    "criteria must not use Offset and/or Limit.");
        }

        if (pageSize < 1)
        {
            throw new IllegalArgumentException(
                    "pageSize must be greater than zero.");
        }

        if (memoryLimitPages < 1)
        {
            throw new IllegalArgumentException(
                    "memoryPageLimit must be greater than zero.");
        }

        this.pageSize = pageSize;
        this.memoryLimit = pageSize * memoryLimitPages;
        this.criteria = criteria;
        dbName = criteria.getDbName();
        blockEnd = blockBegin + memoryLimit - 1;
        startQuery(pageSize);
    }

    /**
     * Retrieve a specific page, if it exists.
     *
     * @param pageNumber the number of the page to be retrieved - must be
     * greater than zero.  An empty <code>List</code> will be returned if
     * <code>pageNumber</code> exceeds the total number of pages that exist.
     * @return a <code>List</code> of query results containing a maximum of
     * <code>pageSize</code> results.
     * @throws IllegalArgumentException when <code>pageNo</code> is not
     * greater than zero.
     * @throws TorqueException if invoking the <code>populateObjects()<code>
     * method runs into problems or a sleep is unexpectedly interrupted.
     */
    public List getPage(int pageNumber) throws TorqueException
    {
        if (pageNumber < 1)
        {
            throw new IllegalArgumentException("pageNumber must be greater "
                    + "than zero.");
        }
        currentPageNumber = pageNumber;
        return getResults((pageNumber - 1) * pageSize);
    }

    /**
     * Gets the next page of rows.
     *
     * @return a <code>List</code> of query results containing a maximum of
     * <code>pageSize</code> reslts.
     * @throws TorqueException if invoking the <code>populateObjects()<code>
     * method runs into problems or a sleep is unexpectedly interrupted.
     */
    public List getNextResults() throws TorqueException
    {
        if (!getNextResultsAvailable())
        {
            return getCurrentPageResults();
        }
        currentPageNumber++;
        return getResults(position);
    }

    /**
     * Provide access to the results from the current page.
     *
     * @return a <code>List</code> of query results containing a maximum of
     * <code>pageSize</code> reslts.
     */
    public List getCurrentPageResults()
    {
        return lastResults;
    }

    /**
     * Gets the previous page of rows.
     *
     * @return a <code>List</code> of query results containing a maximum of
     * <code>pageSize</code> reslts.
     * @throws TorqueException if invoking the <code>populateObjects()<code>
     * method runs into problems or a sleep is unexpectedly interrupted.
     */
    public List getPreviousResults() throws TorqueException
    {
        if (!getPreviousResultsAvailable())
        {
            return getCurrentPageResults();
        }

        int start;
        if (position - 2 * pageSize < 0)
        {
            start = 0;
            currentPageNumber = 1;
        }
        else
        {
            start = position - 2 * pageSize;
            currentPageNumber--;
        }
        return getResults(start);
    }

    /**
     * Gets a page of rows starting at a specified row.
     *
     * @param start the starting row.
     * @return a <code>List</code> of query results containing a maximum of
     * <code>pageSize</code> reslts.
     * @throws TorqueException if invoking the <code>populateObjects()<code>
     * method runs into problems or a sleep is unexpectedly interrupted.
     */
    private List getResults(int start) throws TorqueException
    {
        return getResults(start, pageSize);
    }

    /**
     * Gets a block of rows starting at a specified row and containing a
     * specified number of rows.
     *
     * @param start the starting row.
     * @param size the number of rows.
     * @return a <code>List</code> of query results containing a maximum of
     * <code>pageSize</code> reslts.
     * @throws IllegalArgumentException if <code>size &gt; memoryLimit</code> or
     * <code>start</code> and <code>size</code> result in a situation that is
     * not catered for.
     * @throws TorqueException if invoking the <code>populateObjects()<code>
     * method runs into problems or a sleep is unexpectedly interrupted.
     */
    private synchronized List getResults(int start, int size)
            throws IllegalArgumentException, TorqueException
    {
        log.debug("getResults(start: " + start
                + ", size: " + size + ") invoked.");

        if (size > memoryLimit)
        {
            throw new IllegalArgumentException("size (" + size
                    + ") exceeds memory limit (" + memoryLimit + ").");
        }

        // Request was for a block of rows which should be in progess.
        // If the rows have not yet been returned, wait for them to be
        // retrieved.
        if (start >= blockBegin && (start + size - 1) <= blockEnd)
        {
            log.debug("getResults(): Sleeping until "
                    + "start+size-1 (" + (start + size - 1)
                    + ") > currentlyFilledTo (" + currentlyFilledTo
                    + ") && !queryCompleted (!" + queryCompleted + ")");
            while (((start + size - 1) > currentlyFilledTo) && !queryCompleted)
            {
                try
                {
                    Thread.currentThread().sleep(500);
                }
                catch (InterruptedException e)
                {
                    throw new TorqueException("Unexpected interruption", e);
                }
            }
        }

        // Going in reverse direction, trying to limit db hits so assume user
        // might want at least 2 sets of data.
        else if (start < blockBegin && start >= 0)
        {
            log.debug("getResults(): Paging backwards as start (" + start
                    + ") < blockBegin (" + blockBegin + ") && start >= 0");
            stopQuery();
            if (memoryLimit >= 2 * size)
            {
                blockBegin = start - size;
                if (blockBegin < 0)
                {
                    blockBegin = 0;
                }
            }
            else
            {
                blockBegin = start;
            }
            blockEnd = blockBegin + memoryLimit - 1;
            startQuery(size);
            // Re-invoke getResults() to provide the wait processing.
            return getResults(start, size);
        }

        // Assume we are moving on, do not retrieve any records prior to start.
        else if ((start + size - 1) > blockEnd)
        {
            log.debug("getResults(): Paging past end of loaded data as "
                    + "start+size-1 (" + (start + size - 1)
                    + ") > blockEnd (" + blockEnd + ")");
            stopQuery();
            blockBegin = start;
            blockEnd = blockBegin + memoryLimit - 1;
            startQuery(size);
            // Re-invoke getResults() to provide the wait processing.
            return getResults(start, size);
        }

        else
        {
            throw new IllegalArgumentException("Parameter configuration not "
                    + "accounted for.");
        }

        int fromIndex = start - blockBegin;
        int toIndex = fromIndex + Math.min(size, results.size() - fromIndex);
        log.debug("getResults(): Retrieving records from results elements "
                + "start-blockBegin (" + fromIndex + ") through "
                + "fromIndex + Math.min(size, results.size() - fromIndex) ("
                + toIndex + ")");
        List returnResults = results.subList(fromIndex, toIndex);

        if (null != returnBuilderClass)
        {
            // Invoke the populateObjects() method
            Object[] theArgs = { returnResults };
            try
            {
                returnResults =
                    (List) populateObjectsMethod.invoke(
                        returnBuilderClass.newInstance(),
                        theArgs);
            }
            catch (Exception e)
            {
                throw new TorqueException("Unable to populate results", e);
            }
        }
        position = start + size;
        lastResults = returnResults;
        return returnResults;
    }

    /**
     * A background thread that retrieves the rows.
     */
    public void run()
    {
        int size = pageSize;
        /* The connection to the database. */
        Connection conn = null;

        try
        {
            // Add 1 to memory limit to check if the query ends on a page break.
            results = new ArrayList(memoryLimit + 1);

            // Use the criteria to limit the rows that are retrieved to the
            // block of records that fit in the predefined memoryLimit.
            criteria.setOffset(blockBegin);
            // Add 1 to memory limit to check if the query ends on a page break.
            criteria.setLimit(memoryLimit + 1);
            query = BasePeer.createQueryString(criteria);

            // Get a connection to the db.
            conn = Torque.getConnection(dbName);

            // Execute the query.
            log.debug("run(): query = " + query);
            log.debug("run(): memoryLimit = " + memoryLimit);
            log.debug("run(): blockBegin = " + blockBegin);
            log.debug("run(): blockEnd = " + blockEnd);
            qds = new QueryDataSet(conn, query);

            // Continue getting rows one page at a time until the memory limit
            // is reached, all results have been retrieved, or the rest
            // of the results have been determined to be irrelevant.
            while (!killThread
                && !qds.allRecordsRetrieved()
                && currentlyFilledTo + pageSize <= blockEnd)
            {
                // This caters for when memoryLimit is not a multiple of
                //  pageSize which it never is because we always add 1 above.
                if ((currentlyFilledTo + pageSize) >= blockEnd)
                {
                    // Add 1 to check if the query ends on a page break.
                    size = blockEnd - currentlyFilledTo + 1;
                }

                log.debug("run(): Invoking BasePeer.getSelectResults(qds, "
                        + size + ", false)");

                List tempResults
                        = BasePeer.getSelectResults(qds, size, false);

                for (int i = 0, n = tempResults.size(); i < n; i++)
                {
                    results.add(tempResults.get(i));
                }

                currentlyFilledTo += tempResults.size();
                boolean perhapsLastPage = true;

                // If the extra record was indeed found then we know we are not
                // on the last page but we must now get rid of it.
                if (results.size() == memoryLimit + 1)
                {
                    results.remove(currentlyFilledTo--);
                    perhapsLastPage = false;
                }

                if (results.size() > 0
                    && blockBegin + currentlyFilledTo >= totalRecords)
                {
                    // Add 1 because index starts at 0
                    totalRecords = blockBegin + currentlyFilledTo + 1;
                }

                if (qds.allRecordsRetrieved())
                {
                    queryCompleted = true;
                    // The following ugly condition ensures that the totals are
                    // not finalized when a user does something like requesting
                    // a page greater than what exists in the database.
                    if (perhapsLastPage
                        && getCurrentPageNumber() <= getTotalPages())
                    {
                        totalsFinalized = true;
                    }
                }
                qds.clearRecords();
            }

            log.debug("run(): While loop terminated because either:");
            log.debug("run(): 1. qds.allRecordsRetrieved(): "
                    + qds.allRecordsRetrieved());
            log.debug("run(): 2. killThread: " + killThread);
            log.debug("run(): 3. !(currentlyFilledTo + size <= blockEnd): !"
                    + (currentlyFilledTo + pageSize <= blockEnd));
            log.debug("run(): - currentlyFilledTo: " + currentlyFilledTo);
            log.debug("run(): - size: " + pageSize);
            log.debug("run(): - blockEnd: " + blockEnd);
            log.debug("run(): - results.size(): " + results.size());
        }
        catch (TorqueException e)
        {
            log.error(e);
        }
        catch (SQLException e)
        {
            log.error(e);
        }
        catch (DataSetException e)
        {
            log.error(e);
        }
        finally
        {
            try
            {
                if (qds != null)
                {
                    qds.close();
                }
                Torque.closeConnection(conn);
            }
            catch (SQLException e)
            {
                log.error(e);
            }
            catch (DataSetException e)
            {
                log.error(e);
            }
        }
    }

    /**
     * Starts a new thread to retrieve the result set.
     *
     * @param initialSize the initial size for each block.
     */
    private void startQuery(int initialSize)
    {
        pageSize = initialSize;
        currentlyFilledTo = -1;
        queryCompleted = false;
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Used to stop filling the memory with the current block of results, if it
     * has been determined that they are no longer relevant.
     *
     * @throws TorqueException if a sleep is interrupted.
     */
    private void stopQuery() throws TorqueException
    {
        killThread = true;
        while (thread.isAlive())
        {
            try
            {
                Thread.currentThread().sleep(100);
            }
            catch (InterruptedException e)
            {
                throw new TorqueException("Unexpected interruption", e);
            }
        }
        killThread = false;
    }

    /**
     * Retrieve the number of the current page.
     *
     * @return the current page number.
     */
    public int getCurrentPageNumber()
    {
        return currentPageNumber;
    }

    /**
     * Retrieve the total number of search result records that are known to
     * exist (this will be the actual value when the query has completeted (see
     * <code>getTotalsFinalized()</code>).  The convenience method
     * <code>getRecordProgressText()</code> may be more useful for presenting to
     * users.
     *
     * @return the number of result records known to exist (not accurate until
     * <code>getTotalsFinalized()</code> returns <code>true</code>).
     */
    public int getTotalRecords()
    {
        return totalRecords;
    }

    /**
     * Provide an indication of whether or not paging of results will be
     * required.
     *
     * @return <code>true</code> when multiple pages of results exist.
     */
    public boolean getPaginated()
    {
        // Handle a page memory limit of 1 page.
        if (!getTotalsFinalized())
        {
            return true;
        }
        return blockBegin + currentlyFilledTo + 1 > pageSize;
    }

    /**
     * Retrieve the total number of pages of search results that are known to
     * exist (this will be the actual value when the query has completeted (see
     * <code>getQyeryCompleted()</code>).  The convenience method
     * <code>getPageProgressText()</code> may be more useful for presenting to
     * users.
     *
     * @return the number of pages of results known to exist (not accurate until
     * <code>getTotalsFinalized()</code> returns <code>true</code>).
     */
    public int getTotalPages()
    {
        if (totalPages > -1)
        {
            return totalPages;
        }

        int tempPageCount =
            getTotalRecords() / pageSize
                + (getTotalRecords() % pageSize > 0 ? 1 : 0);

        if (getTotalsFinalized())
        {
            totalPages = tempPageCount;
        }

        return tempPageCount;
    }

    /**
     * Retrieve the page size.
     *
     * @return the number of records returned on each invocation of
     * <code>getNextResults()</code>/<code>getPreviousResults()</code>.
     */
    public int getPageSize()
    {
        return pageSize;
    }

    /**
     * Provide access to indicator that the total values for the number of
     * records and pages are now accurate as opposed to known upper limits.
     *
     * @return <code>true</code> when the totals are known to have been fully
     * computed.
     */
    public boolean getTotalsFinalized()
    {
        return totalsFinalized;
    }

    /**
     * Provide a way of changing the more pages/records indicator.
     *
     * @param moreIndicator the indicator to use in place of the default
     * ("&gt;").
     */
    public static void setMoreIndicator(String moreIndicator)
    {
        LargeSelect.moreIndicator = moreIndicator;
    }

    /**
     * Retrieve the more pages/records indicator.
     */
    public static String getMoreIndicator()
    {
        return LargeSelect.moreIndicator;
    }

    /**
     * Sets the multiplier that will be used to compute the memory limit when a
     * constructor with no memory page limit is used - the memory limit will be
     * this number multiplied by the page size.
     *
     * @param memoryPageLimit the maximum number of pages to be in memory
     * at one time.
     */
    public static void setMemoryPageLimit(int memoryPageLimit)
    {
        LargeSelect.memoryPageLimit = memoryPageLimit;
    }

    /**
     * Retrieves the multiplier that will be used to compute the memory limit
     * when a constructor with no memory page limit is used - the memory limit
     * will be this number multiplied by the page size.
     */
    public static int getMemoryPageLimit()
    {
        return LargeSelect.memoryPageLimit;
    }

    /**
     * A convenience method that provides text showing progress through the
     * selected rows on a page basis.
     *
     * @return progress text in the form of "1 of &gt; 5" where "&gt;" can be
     * configured using <code>setMoreIndicator()</code>.
     */
    public String getPageProgressText()
    {
        StringBuffer result = new StringBuffer();
        result.append(getCurrentPageNumber());
        result.append(" of ");
        if (!totalsFinalized)
        {
            result.append(moreIndicator);
            result.append(" ");
        }
        result.append(getTotalPages());
        return result.toString();
    }

    /**
     * Provides a count of the number of rows to be displayed on the current
     * page - for the last page this may be less than the configured page size.
     *
     * @return the number of records that are included on the current page of
     * results.
     */
    public int getCurrentPageSize()
    {
        if (null == lastResults)
        {
            return 0;
        }
        return lastResults.size();
    }

    /**
     * Provide the record number of the first row included on the current page.
     *
     * @return The record number of the first row of the current page.
     */
    public int getFirstRecordNoForPage()
    {
        if (getCurrentPageNumber() < 1)
        {
            return 0;
        }
        return getCurrentPageNumber() * getPageSize() - getPageSize() + 1;
    }

    /**
     * Provide the record number of the last row included on the current page.
     *
     * @return the record number of the last row of the current page.
     */
    public int getLastRecordNoForPage()
    {
        if (0 == currentPageNumber)
        {
            return 0;
        }
        return (getCurrentPageNumber() - 1) * getPageSize()
                + getCurrentPageSize();
    }

    /**
     * A convenience method that provides text showing progress through the
     * selected rows on a record basis.
     *
     * @return progress text in the form of "26 - 50 of &gt; 250" where "&gt;"
     * can be configured using <code>setMoreIndicator()</code>.
     */
    public String getRecordProgressText()
    {
        StringBuffer result = new StringBuffer();
        result.append(getFirstRecordNoForPage());
        result.append(" - ");
        result.append(getLastRecordNoForPage());
        result.append(" of ");
        if (!totalsFinalized)
        {
            result.append(moreIndicator);
            result.append(" ");
        }
        result.append(getTotalRecords());
        return result.toString();
    }

    /**
     * Indicates if further result pages are available.
     *
     * @return <code>true</code> when further results are available.
     */
    public boolean getNextResultsAvailable()
    {
        if (!totalsFinalized || getCurrentPageNumber() < getTotalPages())
        {
            return true;
        }
        return false;
    }

    /**
     * Indicates if previous results pages are available.
     *
     * @return <code>true</code> when previous results are available.
     */
    public boolean getPreviousResultsAvailable()
    {
        if (getCurrentPageNumber() <= 1)
        {
            return false;
        }
        return true;
    }

    /**
     * Indicates if any results are available.
     *
     * @return <code>true</code> of any results are available.
     */
    public boolean hasResultsAvailable()
    {
        return getTotalRecords() > 0;
    }

    /**
     * Clear the query result so that the query is reexecuted when the next page
     * is retrieved.  You may want to invoke this method if you are returning to
     * a page after performing an operation on an item in the result set.
     *
     * @throws TorqueException if a sleep is interrupted.
     */
    public void invalidateResult() throws TorqueException
    {
        stopQuery();
        blockBegin = 0;
        blockEnd = 0;
        currentlyFilledTo = -1;
        qds = null;
        results = null;
        position = 0;
        totalPages = -1;
        totalRecords = 0;
        // todo Perhaps store the oldPageNumber and immediately restart the
        // query. 
        // oldPageNumber = currentPageNumber;
        currentPageNumber = 0;
        queryCompleted = false;
        totalsFinalized = false;
        lastResults = null;
    }

    /**
     * Retrieve a search parameter.  This acts as a convenient place to store
     * parameters that relate to the LargeSelect to make it easy to get at them
     * in order to repopulate search parameters on a form when the next page of
     * results is retrieved - they in no way effect the operation of
     * LargeSelect.
     *
     * @param name the search parameter key to retrieve.
     * @return the value of the search parameter.
     */
    public String getSearchParam(String name)
    {
        if (null == params)
        {
            return "";
        }
        return (String) params.get(name);
    }

    /**
     * Set a parameter used to retrieve the last set of results.
     *
     * @param name the search parameter key to set.
     * @param value the value of the search parameter to store.
     */
    public void setSearchParam(String name, String value)
    {
        if (null == params)
        {
            params = new Hashtable();
        }
        if (name != null && value != null)
        {
            params.put(name, value);
        }
    }

    /**
     * Provide something useful for debugging purposes.
     * 
     * @return some basic information about this instance of LargeSelect.
     */
    public String toString()
    {
        return "LargeSelect - TotalRecords: " + getTotalRecords() 
                + " TotalsFinalised: " + getTotalsFinalized();
    }

}
