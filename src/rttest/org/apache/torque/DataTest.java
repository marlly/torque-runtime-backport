package org.apache.torque;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import org.apache.torque.om.StringKey;
import org.apache.torque.test.Author;
import org.apache.torque.test.Book;
import org.apache.torque.test.BookPeer;
import org.apache.torque.test.BooleanCheck;
import org.apache.torque.test.BooleanCheckPeer;
import org.apache.torque.test.NullValueTable;
import org.apache.torque.util.Criteria;
import org.apache.torque.test.MultiPk;

/**
 * Runtime tests.
 *
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class DataTest extends BaseTestCase
{
    /**
     * Creates a new instance.
     */
    public DataTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        super.setUp();
    }

    /**
     * does some inserts.
     */
    public void testInsertData()
    {
        try
        {
            for (int i = 1; i <= 10; i++)
            {
                Author author = new Author();
                author.setName("Author " + i);
                author.save();

                for (int j = 1; j <= 10; j++)
                {
                    Book book = new Book();
                    book.setAuthor(author);
                    book.setTitle("Book " + j + " - Author " + i);
                    book.setIsbn("unknown");
                    book.save();
                }
            }
            BooleanCheck bc = new BooleanCheck();
            bc.setTestKey("t1");
            bc.setBintValue(true);
            bc.setBcharValue(true);
            bc.save();
            bc = new BooleanCheck();
            bc.setTestKey("f1");
            bc.setBintValue(false);
            bc.setBcharValue(false);
            bc.save();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * multiple pk test (TRQ12)
     */
    public void testMultiplePk()
    {
        try
        {
            MultiPk mpk = new MultiPk();
            mpk.setPrimaryKey("Svarchar:N5:Schar:");
            mpk.save();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static final String[] validTitles = {
        "Book 7 - Author 8", "Book 6 - Author 8", "Book 7 - Author 7", 
        "Book 6 - Author 7", "Book 7 - Author 6", "Book 6 - Author 6",
        "Book 7 - Author 5", "Book 6 - Author 5", "Book 7 - Author 4",
        "Book 6 - Author 4"};

    /**
     * test limit/offset which was broken for oracle (TRQ47)
     */
    public void testLimitOffset()
    {
        Map titleMap = new HashMap();
        for (int j = 0; j < validTitles.length; j++) 
        {
            titleMap.put(validTitles[j], null);
        }

        try
        {
            Criteria crit = new Criteria();
            Criteria.Criterion c = crit.getNewCriterion(BookPeer.TITLE, 
                    (Object) "Book 6 - Author 1", Criteria.GREATER_EQUAL);
            c.and(crit.getNewCriterion(BookPeer.TITLE, 
                    (Object) "Book 8 - Author 3", Criteria.LESS_EQUAL));
            crit.add(c);
            crit.addDescendingOrderByColumn(BookPeer.BOOK_ID);
            crit.setLimit(10);
            crit.setOffset(5);
            List books = BookPeer.doSelect(crit);
            assertTrue("List should have 10 books, not " + books.size(), 
                       books.size() == 10);
            for (Iterator i = books.iterator(); i.hasNext();) 
            {
                String title = ((Book) i.next()).getTitle();
                assertTrue("Incorrect title: " + title, 
                           titleMap.containsKey(title));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    
    public void testDataDump()
    {
		try
		{
	    	NullValueTable nvt = new NullValueTable();
    		nvt.setNumber1(1);
	    	nvt.setNumber3(3);
    		nvt.setText1("text");
    		nvt.setNumberObj1(new Integer(1));
    		nvt.save();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
    }
    
    /**
     * test boolean values
     */
    public void testBooleanValues()
    {
        try
        {
            BooleanCheck bc = BooleanCheckPeer.retrieveByPK(new StringKey("t1"));
            assertTrue("BOOLEANINT should be true but is: " 
                    + bc.getBintValue(), bc.getBintValue());
            assertTrue("BOOLEANCHAR should be true but is: " 
                    + bc.getBcharValue(), bc.getBcharValue());
            bc = BooleanCheckPeer.retrieveByPK(new StringKey("f1"));
            assertFalse("BOOLEANINT should be false but is: " 
                    + bc.getBintValue(), bc.getBintValue());
            assertFalse("BOOLEANCHAR should be false but is: " 
                    + bc.getBcharValue(), bc.getBcharValue());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    
}
