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

import org.apache.torque.BaseRuntimeTestCase;
import org.apache.torque.TorqueException;
import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;

import java.util.List;

/**
 * Test code for LargeSelect.
 *
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @version $Id$
 */
public class LargeSelectTest extends BaseRuntimeTestCase
{
    private static final int TEST_PAGE_SIZE = 9;
    private static final int TEST_PAGES = 9;
    private static final int TEST_ROWS = TEST_PAGE_SIZE * TEST_PAGES;
    private static final String LARGE_SELECT_AUTHOR = "LargeSelectAuthor";
    private int firstAuthorId = -1;

    private Criteria criteria;

    /**
     * Creates a new instance.
     */
    public LargeSelectTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        super.setUp();

        // Clean up any previous failures
        tearDown();

        // Create some test data
        for (int i = 0; i < TEST_ROWS; i++)
        {
            Author author = new Author();
            author.setName(LARGE_SELECT_AUTHOR);
            try
            {
                author.save();
            }
            catch (Exception e)
            {
                fail("Cannot create test data for LargeSelectTest.");
            }
            if (-1 == firstAuthorId)
            {
                firstAuthorId = author.getAuthorId();
            }
        }
        // Set up the standard criteria for the test.
        criteria = new Criteria();
        criteria.add(AuthorPeer.NAME, LARGE_SELECT_AUTHOR);
    }

    public void tearDown()
    {
        // Delete the test data
        criteria = new Criteria();
        criteria.add(AuthorPeer.NAME, LARGE_SELECT_AUTHOR);
        try
        {
            AuthorPeer.doDelete(criteria);
        }
        catch (TorqueException e)
        {
            fail("Cannot delete test data for LargeSelectTest.");
        }
        criteria = null;
    }

    /**
     * Test the criteria provides the correct number of rows.
     */
    public void testCriteria() throws TorqueException
    {
        List result = null;
        result = AuthorPeer.doSelect(criteria);
        assertEquals("Selected rows", TEST_ROWS, result.size());
    }

    /**
     * Test an invalid criteria - includes a limit.
     */
    public void testBadCriteria11() throws TorqueException
    {
        criteria.setLimit(1);
        try
        {
            new LargeSelect(criteria, TEST_PAGE_SIZE,
                    "org.apache.torque.test.AuthorPeer");
        }
        catch (IllegalArgumentException success)
        {
            // Do nothing
        }
    }

    /**
     * Test an invalid criteria - includes an offset.
     */
    public void testBadCriteria12() throws TorqueException
    {
        criteria.setOffset(1);
        try
        {
            new LargeSelect(criteria, TEST_PAGE_SIZE,
                    "org.apache.torque.test.AuthorPeer");
        }
        catch (IllegalArgumentException success)
        {
            // Do nothing
        }
    }

    /**
     * Test an invalid page size.
     */
    public void testBadPageSize() throws TorqueException
    {
        try
        {
            new LargeSelect(criteria, 0, "org.apache.torque.test.AuthorPeer");
        }
        catch (IllegalArgumentException success)
        {
            // Do nothing
        }
    }

    /**
     * Test an invalid memory limit.
     */
    public void testBadMemoryLimit() throws TorqueException
    {
        try
        {
            new LargeSelect(criteria, TEST_PAGE_SIZE, 0,
                    "org.apache.torque.test.AuthorPeer");
        }
        catch (IllegalArgumentException success)
        {
            // Do nothing
        }
    }

    /**
     * Test an invalid builder class (doesn't provide necessary methods).
     */
    public void testBadClass()
    {
        try
        {
            new LargeSelect(criteria, TEST_PAGE_SIZE,
                    "org.apache.torque.test.Author");
        }
        catch (IllegalArgumentException success)
        {
        }
    }

    /**
     * Test a couple of static methods.
     */
    public void testStaticMethods() throws TorqueException
    {
        assertEquals("Memory page limit", 5, LargeSelect.getMemoryPageLimit());
        LargeSelect.setMemoryPageLimit(10);
        assertEquals("Memory page limit", 10, LargeSelect.getMemoryPageLimit());
        LargeSelect.setMemoryPageLimit(LargeSelect.DEFAULT_MEMORY_LIMIT_PAGES);
        assertEquals("Memory page limit", 5, LargeSelect.getMemoryPageLimit());

        assertEquals("More indicator", "&gt;", LargeSelect.getMoreIndicator());
        String newMoreIndicator = "more than";
        LargeSelect.setMoreIndicator(newMoreIndicator);
        assertEquals("More indicator", newMoreIndicator,
                LargeSelect.getMoreIndicator());
        LargeSelect.setMoreIndicator(LargeSelect.DEFAULT_MORE_INDICATOR);
        assertEquals("More indicator", "&gt;", LargeSelect.getMoreIndicator());
    }

    /**
     * Test a bunch of different methods when everything is set up correctly.
     */
    public void testLargeSelect() throws TorqueException
    {
        LargeSelect ls = new LargeSelect(criteria, TEST_PAGE_SIZE,
                "org.apache.torque.test.AuthorPeer");

        assertEquals("Page size", TEST_PAGE_SIZE, ls.getPageSize());
        assertTrue("Paginated", ls.getPaginated());


        // Page 0
        assertEquals("Current page number", 0, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", 0, ls.getCurrentPageSize());
        assertEquals("First record for page", 0, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 0, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 0, ls.getTotalPages());
        assertEquals("Total records", 0, ls.getTotalRecords());
        assertEquals("Page progress text", "0 of &gt; 0", ls.getPageProgressText());
        assertEquals("Record progress text", "0 - 0 of &gt; 0", ls.getRecordProgressText());

        List results = ls.getNextResults();
        // Page 1
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 1, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 1, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 9, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 5, ls.getTotalPages());
        assertEquals("Total records", 45, ls.getTotalRecords());
        assertEquals("Page progress text", "1 of &gt; 5", ls.getPageProgressText());
        assertEquals("Record progress text", "1 - 9 of &gt; 45", ls.getRecordProgressText());

        results = ls.getPage(5);
        // Page 5
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 5, ls.getCurrentPageNumber());
        assertTrue("Previous results available", ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 37, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 45, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 5, ls.getTotalPages());
        assertEquals("Total records", 45, ls.getTotalRecords());
        assertEquals("Page progress text", "5 of &gt; 5", ls.getPageProgressText());
        assertEquals("Record progress text", "37 - 45 of &gt; 45", ls.getRecordProgressText());

        results = ls.getNextResults();
        // Page 6
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 6, ls.getCurrentPageNumber());
        assertTrue("Previous results available", ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 46, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 54, ls.getLastRecordNoForPage());
        assertTrue("Totals finalised", ls.getTotalsFinalized());
        assertEquals("Total pages", TEST_PAGES, ls.getTotalPages());
        assertEquals("Total records", TEST_ROWS, ls.getTotalRecords());
        assertEquals("Page progress text", "6 of 9", ls.getPageProgressText());
        assertEquals("Record progress text", "46 - 54 of 81", ls.getRecordProgressText());

        results = ls.getNextResults();
        // Page 7
        results = ls.getNextResults();
        // Page 8
        results = ls.getNextResults();
        // Page 9
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 9, ls.getCurrentPageNumber());
        assertTrue("Previous results available", ls.getPreviousResultsAvailable());
        assertEquals("Next results available", false, ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 73, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 81, ls.getLastRecordNoForPage());
        assertTrue("Totals finalised", ls.getTotalsFinalized());
        assertEquals("Total pages", TEST_PAGES, ls.getTotalPages());
        assertEquals("Total records", TEST_ROWS, ls.getTotalRecords());
        assertEquals("Page progress text", "9 of 9", ls.getPageProgressText());
        assertEquals("Record progress text", "73 - 81 of 81", ls.getRecordProgressText());

        results = ls.getPage(2);
        // Page 2
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 2, ls.getCurrentPageNumber());
        assertTrue("Previous results available", ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 10, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 18, ls.getLastRecordNoForPage());
        assertTrue("Totals finalised", ls.getTotalsFinalized());
        assertEquals("Total pages", 9, ls.getTotalPages());
        assertEquals("Total records", 81, ls.getTotalRecords());
        assertEquals("Page progress text", "2 of 9", ls.getPageProgressText());
        assertEquals("Record progress text", "10 - 18 of 81", ls.getRecordProgressText());

        List sameResults = ls.getCurrentPageResults();
        // Page 2
        assertSame("Same results", results, sameResults);

        results = ls.getPreviousResults();
        // Page 1
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 1, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 1, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 9, ls.getLastRecordNoForPage());
        assertTrue("Totals finalised", ls.getTotalsFinalized());
        assertEquals("Total pages", 9, ls.getTotalPages());
        assertEquals("Total records", 81, ls.getTotalRecords());
        assertEquals("Page progress text", "1 of 9", ls.getPageProgressText());
        assertEquals("Record progress text", "1 - 9 of 81", ls.getRecordProgressText());
    }

    /**
     * Test what happens when only one row is returned.
     */
    public void testLargeSelectOneRow() throws Exception
    {
        // Alter criteria to retrieve only one row
        criteria.add(AuthorPeer.AUTHOR_ID, firstAuthorId);

        LargeSelect ls = new LargeSelect(criteria, TEST_PAGE_SIZE,
                "org.apache.torque.test.AuthorPeer");

        // Page 1
        List results = ls.getNextResults();
        assertTrue("Totals finalised", ls.getTotalsFinalized());
        assertEquals("Paginated", false, ls.getPaginated());
        assertEquals("results.size()", 1, results.size());
        assertEquals("Current page number", 1, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertEquals("Next results available", false, ls.getNextResultsAvailable());
        assertEquals("Current page size", 1, ls.getCurrentPageSize());
        assertEquals("First record for page", 1, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 1, ls.getLastRecordNoForPage());
        assertEquals("Total pages", 1, ls.getTotalPages());
        assertEquals("Total records", 1, ls.getTotalRecords());
        assertEquals("Page progress text", "1 of 1", ls.getPageProgressText());
        assertEquals("Record progress text", "1 - 1 of 1", ls.getRecordProgressText());
        assertTrue("Results available", ls.hasResultsAvailable());
    }

    /**
     * Test invalidateResult()
     */
    public void testInvalidateResult() throws Exception
    {
        LargeSelect ls = new LargeSelect(criteria, TEST_PAGE_SIZE,
                "org.apache.torque.test.AuthorPeer");

        assertEquals("Page size", TEST_PAGE_SIZE, ls.getPageSize());
        assertTrue("Paginated", ls.getPaginated());

        // Page 0
        assertEquals("Current page number", 0, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", 0, ls.getCurrentPageSize());
        assertEquals("First record for page", 0, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 0, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 0, ls.getTotalPages());
        assertEquals("Total records", 0, ls.getTotalRecords());
        assertEquals("Page progress text", "0 of &gt; 0", ls.getPageProgressText());
        assertEquals("Record progress text", "0 - 0 of &gt; 0", ls.getRecordProgressText());

        List results = ls.getNextResults();
        // Page 1
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 1, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 1, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 9, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 5, ls.getTotalPages());
        assertEquals("Total records", 45, ls.getTotalRecords());
        assertEquals("Page progress text", "1 of &gt; 5", ls.getPageProgressText());
        assertEquals("Record progress text", "1 - 9 of &gt; 45", ls.getRecordProgressText());

        ls.invalidateResult();

        assertEquals("Page size", TEST_PAGE_SIZE, ls.getPageSize());
        assertTrue("Paginated", ls.getPaginated());

        // Page 0
        assertEquals("Current page number", 0, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", 0, ls.getCurrentPageSize());
        assertEquals("First record for page", 0, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 0, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 0, ls.getTotalPages());
        assertEquals("Total records", 0, ls.getTotalRecords());
        assertEquals("Page progress text", "0 of &gt; 0", ls.getPageProgressText());
        assertEquals("Record progress text", "0 - 0 of &gt; 0", ls.getRecordProgressText());

        results = ls.getNextResults();
        // Page 1
        assertEquals("results.size()", TEST_PAGE_SIZE, results.size());
        assertEquals("Current page number", 1, ls.getCurrentPageNumber());
        assertEquals("Previous results available", false, ls.getPreviousResultsAvailable());
        assertTrue("Next results available", ls.getNextResultsAvailable());
        assertEquals("Current page size", TEST_PAGE_SIZE, ls.getCurrentPageSize());
        assertEquals("First record for page", 1, ls.getFirstRecordNoForPage());
        assertEquals("Last record for page", 9, ls.getLastRecordNoForPage());
        assertEquals("Totals finalised", false, ls.getTotalsFinalized());
        assertEquals("Total pages", 5, ls.getTotalPages());
        assertEquals("Total records", 45, ls.getTotalRecords());
        assertEquals("Page progress text", "1 of &gt; 5", ls.getPageProgressText());
        assertEquals("Record progress text", "1 - 9 of &gt; 45", ls.getRecordProgressText());
    }

    // todo Add a test for getPaginated() - was previously returning false when 6 results and pageSize 5

    // todo Add test for parameter storage
}
