package org.apache.torque;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.torque.om.StringKey;
import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;
import org.apache.torque.test.Book;
import org.apache.torque.test.BookPeer;
import org.apache.torque.test.BooleanCheck;
import org.apache.torque.test.BooleanCheckPeer;
import org.apache.torque.test.DateTest;
import org.apache.torque.test.DateTestPeer;
import org.apache.torque.test.MultiPk;
import org.apache.torque.test.MultiPkPeer;
import org.apache.torque.test.NullValueTable;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

import com.workingdogs.village.Record;

/**
 * Runtime tests.
 *
 * @author <a href="mailto:seade@backstagetech.com.au">Scott Eade</a>
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
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
        // clean booleancheck table (because insert uses fixed keys)
        Criteria criteria = new Criteria();
        criteria.add(BooleanCheckPeer.TEST_KEY, (Object) null, Criteria.NOT_EQUAL);
        try 
        {
            BooleanCheckPeer.doDelete(criteria);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            fail("cleaning table : Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
        }
        
        // do tests
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
            fail("Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
        }
    }

    /**
     * multiple pk test (TRQ12)
     */
    public void testMultiplePk()
    {
        // clean table
        Criteria criteria = new Criteria();
        criteria.add(MultiPkPeer.PK1, (Object) null, Criteria.NOT_EQUAL);
        try 
        {
            MultiPkPeer.doDelete(criteria);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            fail("cleaning table : Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
        }
        
        // do test
        try
        {
            MultiPk mpk = new MultiPk();
            mpk.setPrimaryKey("Svarchar:N5:Schar:");
            mpk.save();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            fail("Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
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
            fail("Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
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
            fail("Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
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
            fail("Exception caught : " 
                    + ex.getClass().getName() 
                    + " : " + ex.getMessage());
        }
    }

    /**
     * test whether delete works as expected
     */
    public void testDelete() 
    {
        cleanBookstore();
        Author author = null;
        Book book = null;
        try 
        {
            author = new Author();
            author.setName("Name");
            author.save();

            book = new Book();
            book.setTitle("title");
            book.setAuthor(author);
            book.setIsbn("ISBN");
            book.save();
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("inserting Data : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }


        // delete without matching data
        Criteria criteria = new Criteria();
        criteria.add(
                AuthorPeer.AUTHOR_ID, 
                author.getAuthorId(), 
                Criteria.NOT_EQUAL);
        List authorResult = null;
        try 
        {
            AuthorPeer.doDelete(criteria);
            authorResult = AuthorPeer.doSelect(new Criteria());
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("delete not in : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        assertTrue("deleted too many records", authorResult.size() == 1);

        
        // delete by object
        List bookResult = null;
        try 
        {
            BookPeer.doDelete(book);
            bookResult = BookPeer.doSelect(new Criteria());
            authorResult = AuthorPeer.doSelect(new Criteria());
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("delete by object : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        // check that the book has disappeared
        assertTrue("delete by object failed", 
            bookResult.size() == 0);
        // check that the underlying author has not been deleted
        assertTrue("delete by object deleted in cascade", 
            authorResult.size() == 1);

        
        // delete with matching data
        criteria.clear();
        criteria.add(AuthorPeer.AUTHOR_ID, author.getAuthorId());
        try 
        {
            AuthorPeer.doDelete(criteria);
            authorResult = AuthorPeer.doSelect(new Criteria());
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("delete by object : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        assertTrue("deleted not enough records", 
            authorResult.size() == 0);
    }
    
    /**
     * test special cases in the select clause
     */
    public void testSelectClause() 
    {     
        // test double functions in select columns
        Criteria criteria = new Criteria();
        criteria.addSelectColumn("count(distinct(" + BookPeer.BOOK_ID + "))");
        
        List result;
        try 
        {
            result = BookPeer.doSelectVillageRecords(criteria);
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("count(distinct(...)) : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        
        // test qualifiers in function in select columns
        criteria = new Criteria();
        criteria.addSelectColumn("count(distinct " + BookPeer.BOOK_ID + ")");
        
        try 
        {
            result = BookPeer.doSelectVillageRecords(criteria);
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("count(distinct(...)) : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
    
    /**
     * test joins
     */
    public void testJoins() 
    {
        cleanBookstore();
        try 
        {
            // insert test data
            Author author = new Author();
            author.setName("Author with one book");
            author.save();
            Book book = new Book();
            book.setAuthor(author);
            book.setTitle("Book 1");
            book.setIsbn("unknown");
            book.save();
          
            author = new Author();
            author.setName("Author without book");
            author.save();
    
            author = new Author();
            author.setName("Author with three books");
            author.save();
            for (int bookNr = 2; bookNr <=4; bookNr++) 
            {
                book = new Book();
                book.setAuthor(author);
                book.setTitle("Book " + bookNr);
                book.setIsbn("unknown");
                book.save();
            }
    
            // test left join
            Criteria criteria = new Criteria();
            criteria.addJoin(AuthorPeer.AUTHOR_ID, BookPeer.AUTHOR_ID,
                    Criteria.LEFT_JOIN);
            List authorList = AuthorPeer.doSelect(criteria);
            // Here we get 5 authors:
            // the author with one book, the author without books,
            // and three times the author with three books
            if (authorList.size() != 5) 
            {
                fail("author left join book : "
                         + "incorrect numbers of authors found : " 
                         + authorList.size()
                         + ", should be 5");
            }
          
            // test inner join
            criteria = new Criteria();
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, BookPeer.AUTHOR_ID,
                    Criteria.INNER_JOIN);
            authorList = AuthorPeer.doSelect(criteria);
            // Here we get 4 authors:
            // the author with one book, 
            // and three times the author with three books
            if (authorList.size() != 4) 
            {
                fail("author left join book : "
                         + "incorrect numbers of authors found : " 
                         + authorList.size()
                         + ", should be 4");
            }
            
            // test right join
            criteria = new Criteria();
            criteria.addJoin(
                    BookPeer.AUTHOR_ID, AuthorPeer.AUTHOR_ID,
                    Criteria.RIGHT_JOIN);
            authorList = AuthorPeer.doSelect(criteria);
            // Here we get 4 authors:
            // the author with one book, the author without books,
            // and three times the author with three books
            if (authorList.size() != 5) 
            {
                fail("book right join author "
                         + "incorrect numbers of authors found : " 
                         + authorList.size()
                         + ", should be 5");
            }
            
            // test double join with aliases
            criteria = new Criteria();
            criteria.addAlias("b", BookPeer.TABLE_NAME);
            criteria.addJoin(
                    BookPeer.AUTHOR_ID, AuthorPeer.AUTHOR_ID,
                    Criteria.RIGHT_JOIN);
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, 
                    "b." + getRawColumnName(BookPeer.AUTHOR_ID),
                    Criteria.LEFT_JOIN);
            authorList = AuthorPeer.doSelect(criteria);
            // Here we get 11 authors:
            // the author with one book, the author without books,
            // and nine times the author with three books
            if (authorList.size() != 11) 
            {
                fail("book right join author left join book b: "
                         + "incorrect numbers of authors found : " 
                         + authorList.size()
                         + ", should be 11");
            }
            
            // test double join with aliases and "reversed" second join
            criteria = new Criteria();
            criteria.addAlias("b", BookPeer.TABLE_NAME);
            criteria.addJoin(BookPeer.AUTHOR_ID, AuthorPeer.AUTHOR_ID,
                    Criteria.RIGHT_JOIN);
            criteria.addJoin(
                    "b." + getRawColumnName(BookPeer.AUTHOR_ID),
                    AuthorPeer.AUTHOR_ID,
                    Criteria.RIGHT_JOIN);
            authorList = AuthorPeer.doSelect(criteria);
            // Here we get 11 authors:
            // the author with one book, the author without books,
            // and nine times the author with three books
            if (authorList.size() != 11) 
            {
                fail("book right join author left join book b (reversed): "
                         + "incorrect numbers of authors found : " 
                         + authorList.size()
                         + ", should be 11");
            }
        }
        catch( Exception e) {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }

    /**
     * test the order by, especially in joins and with aliases
     */
    public void testOrderBy() 
    {
        cleanBookstore();
        try 
        {
            // insert test data
            Author firstAuthor = new Author();
            firstAuthor.setName("Author 1");
            firstAuthor.save();
            Book book = new Book();
            book.setAuthor(firstAuthor);
            book.setTitle("Book 1");
            book.setIsbn("unknown");
            book.save();
                
            Author secondAuthor = new Author();
            secondAuthor.setName("Author 2");
            secondAuthor.save();
            for (int bookNr = 2; bookNr <=4; bookNr++) 
            {
                book = new Book();
                book.setAuthor(secondAuthor);
                book.setTitle("Book " + bookNr);
                book.setIsbn("unknown");
                book.save();
            }
            
            // test simple ascending order by
            Criteria criteria = new Criteria();
            criteria.addAscendingOrderByColumn(BookPeer.TITLE);
            List bookList = BookPeer.doSelect(criteria);
            if (bookList.size() != 4) 
            {
                fail("Ascending Order By: "
                         + "incorrect numbers of books found : " 
                         + bookList.size()
                         + ", should be 4");
            }
            if (! "Book 1".equals(((Book) bookList.get(0)).getTitle())) 
            {
                fail("Ascending Order By: "
                         + "Title of first Book is " 
                         + ((Book) bookList.get(0)).getTitle()
                         + ", should be \"Book 1\"");
            }
            if (! "Book 4".equals(((Book) bookList.get(3)).getTitle())) 
            {
                fail("Ascending Order By: "
                         + "Title of fourth Book is " 
                         + ((Book) bookList.get(3)).getTitle()
                         + ", should be \"Book 4\"");
            }
            
            // test simple descending order by
            criteria = new Criteria();
            criteria.addDescendingOrderByColumn(BookPeer.TITLE);
            bookList = BookPeer.doSelect(criteria);
            if (bookList.size() != 4) 
            {
                fail("Descending Order By: "
                         + "incorrect numbers of books found : " 
                         + bookList.size()
                         + ", should be 4");
            }
            if (! "Book 1".equals(((Book) bookList.get(3)).getTitle())) 
            {
                fail("Descending Order By: "
                         + "Title of fourth Book is " 
                         + ((Book) bookList.get(3)).getTitle()
                         + ", should be \"Book 1\"");
            }
            if (! "Book 4".equals(((Book) bookList.get(0)).getTitle())) 
            {
                fail("Descending Order By: "
                         + "Title of first Book is " 
                         + ((Book) bookList.get(0)).getTitle()
                         + ", should be \"Book 4\"");
            }
            
            // test ordering by Aliases and in joins
            criteria = new Criteria();
            criteria.addAlias("b", BookPeer.TABLE_NAME);
            criteria.addJoin(BookPeer.AUTHOR_ID, AuthorPeer.AUTHOR_ID);
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, 
                    "b." + getRawColumnName(BookPeer.AUTHOR_ID));
            criteria.addAscendingOrderByColumn(
                    "b." + getRawColumnName(BookPeer.TITLE));
            criteria.addDescendingOrderByColumn(BookPeer.TITLE);
            // the retrieved columns are
            // author    book   b
            // author1  book1   book1
            // author2  book4   book2
            // author2  book3   book2
            // author2  book2   book2
            // author2  book4   book3
            // ...
            bookList = BookPeer.doSelect(criteria);
            if (bookList.size() != 10) 
            {
                fail("ordering by Aliases: "
                         + "incorrect numbers of books found : " 
                         + bookList.size()
                         + ", should be 10");
            }
            if (!"Book 4".equals(((Book)bookList.get(1)).getTitle())) 
            {
                fail("ordering by Aliases: "
                         + "Title of second Book is " 
                         + ((Book) bookList.get(1)).getTitle()
                         + ", should be \"Book 4\"");
            }
            if (!"Book 3".equals(((Book)bookList.get(2)).getTitle())) 
            {
                fail("ordering by Aliases: "
                         + "Title of third Book is " 
                         + ((Book) bookList.get(2)).getTitle()
                         + ", should be \"Book 3\"");
            }
            
            criteria = new Criteria();
            criteria.addAlias("b", BookPeer.TABLE_NAME);
            criteria.addJoin(BookPeer.AUTHOR_ID, AuthorPeer.AUTHOR_ID);
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, 
                    "b." + getRawColumnName(BookPeer.AUTHOR_ID));
            criteria.addAscendingOrderByColumn(BookPeer.TITLE);
            criteria.addDescendingOrderByColumn(
                    "b." + getRawColumnName(BookPeer.TITLE));
            // the retrieved columns are
            // author    book   b
            // author1  book1   book1
            // author2  book2   book4
            // author2  book2   book3
            // author2  book2   book2
            // author2  book3   book4
            // ...
            bookList = BookPeer.doSelect(criteria);
            if (bookList.size() != 10) 
            {
                fail("ordering by Aliases (2): "
                         + "incorrect numbers of books found : " 
                         + bookList.size()
                         + ", should be 10");
            }
            if (!"Book 2".equals(((Book)bookList.get(1)).getTitle())) 
            {
                fail("ordering by Aliases (2, PS): "
                         + "Title of second Book is " 
                         + ((Book) bookList.get(1)).getTitle()
                         + ", should be \"Book 2\"");
            }
            if (!"Book 2".equals(((Book)bookList.get(2)).getTitle())) 
            {
                fail("ordering by Aliases (2, PS): "
                         + "Title of third Book is " 
                         + ((Book) bookList.get(2)).getTitle()
                         + ", should be \"Book 2\"");
            }
            
        }
        catch( Exception e) 
        {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
    
    
    /**
     * Tests whether ignoreCase works correctly
     */
    public void testIgnoreCase() 
    {
        cleanBookstore();
        try 
        {
            Author author = new Author();
            author.setName("AuTHor");
            author.save();
            
            Criteria criteria = new Criteria();
            criteria.add(AuthorPeer.NAME, author.getName().toLowerCase());
            criteria.setIgnoreCase(true);
            List result = AuthorPeer.doSelect(criteria);
            if (result.size() != 1) 
            {
                fail("Size of result is not 1, but " + result.size());
            }
        }
        catch( Exception e) 
        {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
    
    /**
     * tests AsColumns produce valid SQL code
     */
    public void testAsColumn() 
    {
        try 
        {
            Criteria criteria = new Criteria();
            criteria.addAsColumn("ALIASNAME", AuthorPeer.NAME);
            // we need an additional column to select from,
            // to indicate the table we want use
            criteria.addSelectColumn(AuthorPeer.AUTHOR_ID);
            BasePeer.doSelect(criteria);
        }
        catch( Exception e) 
        {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
    
    /**
     * Test whether same column name in different tables
     * are handled correctly
     */
    public void testSameColumnName() 
    {
        try 
        {
            cleanBookstore();
            Author author = new Author();
            author.setName("Name");
            author.save();
            
            author = new Author();
            author.setName("NotCorrespondingName");
            author.save();
            
            Book book = new Book();
            book.setTitle("Name");
            book.setAuthor(author);
            book.setIsbn("unknown");
            book.save();
            
            Criteria criteria = new Criteria();
            criteria.addJoin(BookPeer.TITLE, AuthorPeer.NAME);
            BookPeer.addSelectColumns(criteria);
            AuthorPeer.addSelectColumns(criteria);
            // basically a BaseBookPeer.setDbName(criteria);
            // and BasePeer.doSelect(criteria);
            List villageRecords = BookPeer.doSelectVillageRecords(criteria);
            Record record = (Record) villageRecords.get(0);
            book = new Book();
            BookPeer.populateObject(record, 1, book);
            author = new Author();
            AuthorPeer.populateObject(record, BookPeer.numColumns + 1, author);

            if (book.getAuthorId() == author.getAuthorId()) {
                fail("wrong Ids read");
            }
        }
        catch( Exception e) 
        {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
    
    /**
     * Tests the date, time and datetime accuracy.
     * At the moment, no upper limit for the accuracy is checked,
     * the differences are printed to stdout.
     */
    public void testDateTime() 
    {
        try
        {
            // clean Date table
            Criteria criteria = new Criteria();
            criteria.add(
                    DateTestPeer.DATE_TEST_ID, 
                    (Long) null, 
                    Criteria.NOT_EQUAL);
            DateTestPeer.doDelete(criteria);
            
            // insert new DateTest object to db
            DateTest dateTest = new DateTest();
            Date now = new Date();
            dateTest.setDateValue(now);
            dateTest.setTimeValue(now);
            dateTest.setTimestampValue(now);
            dateTest.save();
            DateFormat dateFormat = new SimpleDateFormat();
            System.out.println(
                    "testDateTime() : set date to : " 
                    + dateFormat.format(now));
            
            // reload dateTest from db
            DateTest loadedDateTest 
                    = DateTestPeer.retrieveByPK(dateTest.getPrimaryKey());
            
            System.out.println(
                    "testDateTime() : retrieved date : " 
                    + dateFormat.format(loadedDateTest.getDateValue()));
            System.out.println(
                    "testDateTime() : retrieved time : " 
                    + dateFormat.format(loadedDateTest.getTimeValue()));
            System.out.println(
                    "testDateTime() : retrieved timestamp : " 
                    + dateFormat.format(loadedDateTest.getTimestampValue()));

            // compute time differences between reloaded and original object
            long dateDifference 
                    = dateTest.getDateValue().getTime()
                        - loadedDateTest.getDateValue().getTime();
            long timeDifference 
                    = dateTest.getTimeValue().getTime()
                        - loadedDateTest.getTimeValue().getTime();
            long timestampDifference 
                    = dateTest.getTimestampValue().getTime()
                        - loadedDateTest.getTimestampValue().getTime();
            
            System.out.println(
                    "testDateTime() : Date difference (ms): " 
                    + dateDifference);
            System.out.println(
                    "testDateTime() : Time difference (ms): " 
                    + timeDifference);
            System.out.println(
                    "testDateTime() : Timestamp difference (ms): " 
                    + timestampDifference);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
    
    /**
     * Deletes all authors and books in the bookstore tables
     */
    protected void cleanBookstore() 
    {
        Criteria criteria = new Criteria();
        criteria.add(BookPeer.BOOK_ID, (Long) null, Criteria.NOT_EQUAL);
        try 
        {
            BookPeer.doDelete(criteria);
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("cleaning books : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        criteria.clear();
        criteria.add(
                AuthorPeer.AUTHOR_ID, 
                (Long) null, Criteria.NOT_EQUAL);
        try 
        {
            AuthorPeer.doDelete(criteria);
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            fail("cleaning authors : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }

    
    /**
     * Tests the CountHelper class
     */
    public void testCountHelper() 
    {
        try 
        {
            cleanBookstore();
            Author author = new Author();
            author.setName("Name");
            author.save();
            
            author = new Author();
            author.setName("Name2");
            author.save();
            
            author = new Author();
            author.setName("Name");
            author.save();
            
            Criteria criteria = new Criteria();
            int count = new CountHelper().count(
                    criteria, 
                    null, 
                    AuthorPeer.AUTHOR_ID);
            
            if (count != 3) {
                fail("counted " + count + " datasets, should be 3 ");
            }
                        
            criteria = new Criteria();
            criteria.setDistinct();
            count = new CountHelper().count(criteria, null, AuthorPeer.NAME);
            
            if (count != 2) {
                fail("counted " + count + " distinct datasets, should be 2 ");
            }

            criteria = new Criteria();
            criteria.add(AuthorPeer.NAME, "Name2");
            count = new CountHelper().count(criteria);
            
            if (count != 1) {
                fail("counted " + count + " datasets with name Name2,"
                     + " should be 1 ");
            }
        
        }
        catch( Exception e) 
        {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }
        
        
    /**
     * Strips the schema and table name from a fully qualified colum name
     * This is useful for creating Query with aliases, as the constants
     * for the colum names in the data objects are fully qualified.
     * @param fullyQualifiedColumnName the fully qualified column name, not null
     * @return the column name stripped from the table (and schema) prefixes
     */
    public static String getRawColumnName(String fullyQualifiedColumnName) 
    {
        int dotPosition = fullyQualifiedColumnName.lastIndexOf(".");
        if (dotPosition == -1) 
        {
            return fullyQualifiedColumnName;
        }
        String result = fullyQualifiedColumnName.substring(
                dotPosition + 1, 
                fullyQualifiedColumnName.length());
        return result;
    }

}
