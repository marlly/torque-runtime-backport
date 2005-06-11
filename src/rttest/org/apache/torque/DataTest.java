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

import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.adapter.DBHypersonicSQL;
import org.apache.torque.adapter.DBOracle;
import org.apache.torque.om.StringKey;
import org.apache.torque.test.A;
import org.apache.torque.test.APeer;
import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;
import org.apache.torque.test.BitTest;
import org.apache.torque.test.BitTestPeer;
import org.apache.torque.test.BlobTest;
import org.apache.torque.test.BlobTestPeer;
import org.apache.torque.test.Book;
import org.apache.torque.test.BookPeer;
import org.apache.torque.test.BooleanCheck;
import org.apache.torque.test.BooleanCheckPeer;
import org.apache.torque.test.ClobTest;
import org.apache.torque.test.ClobTestPeer;
import org.apache.torque.test.DateTest;
import org.apache.torque.test.DateTestPeer;
import org.apache.torque.test.IntegerPk;
import org.apache.torque.test.LargePk;
import org.apache.torque.test.LargePkPeer;
import org.apache.torque.test.MultiPk;
import org.apache.torque.test.MultiPkForeignKey;
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
public class DataTest extends BaseRuntimeTestCase
{
    private static Log log = LogFactory.getLog(DataTest.class);;
    
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
     * test whether we can connect to the database at all
     * @throws Exception if no connection can be established
     */
    public void testConnect() throws Exception
    {
        Connection connection = null;
        try 
        {
        	connection = Torque.getConnection();
            connection.close();
            connection = null;
        }
        finally 
        {
        	if (connection != null)
            {
        		connection.close();
            }
        }
    }
    
    /**
     * does some inserts.
     * @throws Exception if the test fails
     */
    public void testInsertData() throws Exception
    {
        // insert books and authors
        for (int i = 1; i <= 10; i++)
        {
            Author author = new Author();
            author.setName("Author " + i);
            author.save();
            assertTrue("authorId should not be 0 after insert",
                    author.getAuthorId() != 0);

            for (int j = 1; j <= 10; j++)
            {
                Book book = new Book();
                book.setAuthor(author);
                book.setTitle("Book " + j + " - Author " + i);
                book.setIsbn("unknown");
                book.save();
            }
        }
        // clean booleancheck table (because insert uses fixed keys)
        Criteria criteria = new Criteria();
        criteria.add(BooleanCheckPeer.TEST_KEY, (Object) null, Criteria.NOT_EQUAL);
        BooleanCheckPeer.doDelete(criteria);
        
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

    /**
     * multiple pk test (TRQ12)
     * @throws Exception if the test fails
     */
    public void testMultiplePk() throws Exception
    {
        // clean table
        Criteria criteria = new Criteria();
        criteria.add(MultiPkPeer.PK1, (Object) null, Criteria.NOT_EQUAL);
        MultiPkPeer.doDelete(criteria);
        
        // do test
        MultiPk mpk = new MultiPk();
        mpk.setPrimaryKey("Svarchar:N5:Schar:");
        mpk.save();
    }

    private static final String[] validTitles = {
        "Book 7 - Author 8", "Book 6 - Author 8", "Book 7 - Author 7", 
        "Book 6 - Author 7", "Book 7 - Author 6", "Book 6 - Author 6",
        "Book 7 - Author 5", "Book 6 - Author 5", "Book 7 - Author 4",
        "Book 6 - Author 4"};

    /**
     * test limit/offset which was broken for oracle (TRQ47)
     * @throws Exception if the test fails
     */
    public void testLimitOffset() throws Exception
    {
        Map titleMap = new HashMap();
        for (int j = 0; j < validTitles.length; j++) 
        {
            titleMap.put(validTitles[j], null);
        }

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
    
    /**
     * Checks whether the setSingleRecord() method in criteria works
     */
    public void testSingleRecord() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.setSingleRecord(true);
        criteria.setLimit(1);
        criteria.setOffset(5);
        List books = BookPeer.doSelect(criteria);
        assertTrue("List should have 1 books, not " + books.size(), 
                books.size() == 1);
        
        criteria.clear();
        criteria.setSingleRecord(true);
        criteria.setLimit(2);
        try
        {
            books = BookPeer.doSelect(criteria);
            fail("doSelect should have failed "
                    + "because two records were selected "
                    + " and one was expected");
        }
        catch (TorqueException e)
        {   
        }
    }
    
    /**
     * tests whether null values can be processed successfully by datadump
     * For this, a row containing null values is inserted here,
     * the actual test is done later 
     * @throws Exception if inserting the test data fails
     */
    public void testDataDump() throws Exception
    {
        NullValueTable nvt = new NullValueTable();
        nvt.setNumber1(1);
        nvt.setNumber3(3);
        nvt.setText1("text");
        nvt.setNumberObj1(new Integer(1));
        nvt.save();
    }
    
    /**
     * test boolean values
     * @throws Exception if the test fails
     */
    public void testBooleanValues() throws Exception
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
    
    /**
     * Tests whether column type BIT can be written and read correctly
     * and works in criteria as expected
     * @throws Exception if the test fails
     */
    public void testBitType() throws Exception
    {
        if (Torque.getDB(Torque.getDefaultDB()) instanceof DBOracle)
        {
            log.error("testBitType(): BIT is known not to work with Oracle");
            // failing is "expected", so exit without error
            return;
        }
        
        // clean table
        Criteria criteria = new Criteria();
        criteria.add(BitTestPeer.ID, (Object) null, Criteria.NOT_EQUAL);
        BitTestPeer.doDelete(criteria);
        
        // insert Data
        BitTest bitTest = new BitTest();
        bitTest.setId("t1");
        bitTest.setBitValue(true);
        bitTest.save();
        bitTest = new BitTest();
        bitTest.setId("f1");
        bitTest.setBitValue(false);
        bitTest.save();

        // read data
        bitTest = BitTestPeer.retrieveByPK(new StringKey("t1"));
        assertTrue("BIT should be true but is: " 
                + bitTest.getBitValue(), bitTest.getBitValue());
        
        bitTest = BitTestPeer.retrieveByPK(new StringKey("f1"));
        assertFalse("BIT should be false but is: " 
                + bitTest.getBitValue(), bitTest.getBitValue());
        
        // query data
        criteria.clear();
        criteria.add(BitTestPeer.BIT_VALUE, new Boolean(true));
        List bitTestList = BitTestPeer.doSelect(criteria);
        assertTrue("Should have read 1 dataset "
                + "but read " + bitTestList.size(), 
                bitTestList.size() == 1);
        bitTest = (BitTest) bitTestList.get(0);
        // use trim() for testkey because some databases will return the
        // testkey filled up with blanks, as it is defined as char(10)
        assertTrue("Primary key of data set should be t1 but is "
                + bitTest.getId().trim(),
                "t1".equals(bitTest.getId().trim()));

        criteria.clear();
        criteria.add(BitTestPeer.BIT_VALUE, new Boolean(false));
        bitTestList = BitTestPeer.doSelect(criteria);
        assertTrue("Should have read 1 dataset "
                + "but read " + bitTestList.size(), 
                bitTestList.size() == 1);
        bitTest = (BitTest) bitTestList.get(0);
        assertTrue("Primary key of data set should be f1 but is "
                + bitTest.getId().trim(),
                "f1".equals(bitTest.getId().trim()));

    }
    
    /**
     * check whether we can select from boolean columns 
     * @throws Exception if the test fails
     */
    public void testBooleanSelects() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.add(BooleanCheckPeer.BCHAR_VALUE, new Boolean(true));
        criteria.add(BooleanCheckPeer.BINT_VALUE, new Boolean(true));
        List booleanCheckList = BooleanCheckPeer.doSelect(criteria);
        assertTrue("Should have read 1 dataset with both values true "
                + "but read " + booleanCheckList.size(), 
                booleanCheckList.size() == 1);
        BooleanCheck booleanCheck = (BooleanCheck) booleanCheckList.get(0);
        // use trim() for testkey because some databases will return the
        // testkey filled up with blanks, as it is defined as char(10)
        assertTrue("Primary key of data set should be t1 but is "
                + booleanCheck.getTestKey().trim(),
                "t1".equals(booleanCheck.getTestKey().trim()));
        
        criteria.clear();
        criteria.add(BooleanCheckPeer.BCHAR_VALUE, new Boolean(false));
        criteria.add(BooleanCheckPeer.BINT_VALUE, new Boolean(false));
        booleanCheckList = BooleanCheckPeer.doSelect(criteria);
        assertTrue("Should have read 1 dataset with both values false "
                + "but read " + booleanCheckList.size(), 
                booleanCheckList.size() == 1);
        booleanCheck = (BooleanCheck) booleanCheckList.get(0);
        assertTrue("Primary key of data set should be f1 but is "
                + booleanCheck.getTestKey().trim(),
                "f1".equals(booleanCheck.getTestKey().trim()));
    }

    /**
     * test whether delete works as expected
     * @throws Exception if the test fails
     */
    public void testDelete() throws Exception
    {
        cleanBookstore();
        
        Author author = new Author();
        author.setName("Name");
        author.save();

        Book book = new Book();
        book.setTitle("title");
        book.setAuthor(author);
        book.setIsbn("ISBN");
        book.save();

        // delete without matching data
        Criteria criteria = new Criteria();
        criteria.add(
                AuthorPeer.AUTHOR_ID, 
                author.getAuthorId(), 
                Criteria.NOT_EQUAL);
        AuthorPeer.doDelete(criteria);
        List authorResult = AuthorPeer.doSelect(new Criteria());
        assertTrue("deleted too many records", authorResult.size() == 1);

        BookPeer.doDelete(book);
        List bookResult = BookPeer.doSelect(new Criteria());
        authorResult = AuthorPeer.doSelect(new Criteria());
        // check that the book has disappeared
        assertTrue("delete by object failed", 
            bookResult.size() == 0);
        // check that the underlying author has not been deleted
        assertTrue("delete by object deleted in cascade", 
            authorResult.size() == 1);

        // delete with matching data
        criteria.clear();
        criteria.add(AuthorPeer.AUTHOR_ID, author.getAuthorId());
        AuthorPeer.doDelete(criteria);
        authorResult = AuthorPeer.doSelect(new Criteria());
        assertTrue("deleted not enough records", 
            authorResult.size() == 0);
    }
    
    /**
     * test special cases in the select clause
     * @throws Exception if the test fails
     */
    public void testSelectClause() throws Exception
    {     
        // test double functions in select columns
        Criteria criteria = new Criteria();
        criteria.addSelectColumn("count(distinct(" + BookPeer.BOOK_ID + "))");
        List result = BookPeer.doSelectVillageRecords(criteria);
        
        // test qualifiers in function in select columns
        criteria = new Criteria();
        criteria.addSelectColumn("count(distinct " + BookPeer.BOOK_ID + ")");
        result = BookPeer.doSelectVillageRecords(criteria);
    }
    
    /**
     * test the behaviour if a connection is supplied to access the database,
     * but it is null. All methods on the user level should be able to 
     * handle this.
     */
    public void testNullConnection() throws Exception
    {
        Criteria criteria = new Criteria();
        List result = BookPeer.doSelectVillageRecords(criteria, null);
        
        criteria = new Criteria();
        criteria.add(BookPeer.BOOK_ID, (Long) null, Criteria.NOT_EQUAL);
        BookPeer.doDelete(criteria, null);
        
        Author author = new Author();
        author.setName("name");
        author.save((Connection) null);
    }
    
    /**
     * test joins
     * @throws Exception if the test fails
     */
    public void testJoins() throws Exception
    {
        cleanBookstore();

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

        if (Torque.getDB(Torque.getDefaultDB()) instanceof DBHypersonicSQL)
        {
            log.error("testJoins(): Right joins are not supported by HSQLDB");
            // failing is "expected", so exit without error
            return;
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

    /**
     * test the order by, especially in joins and with aliases
     * @throws Exception if the test fails
     */
    public void testOrderBy() throws Exception
    {
        cleanBookstore();

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
        
        // test usage of Expressions in order by
        criteria = new Criteria();
        criteria.addAscendingOrderByColumn("UPPER(" + BookPeer.TITLE + ")");
        criteria.setIgnoreCase(true);
        BookPeer.doSelect(criteria);
    }
    
    
    /**
     * Tests whether ignoreCase works correctly
     * @throws Exception if the test fails
     */
    public void testIgnoreCase() throws Exception
    {
        cleanBookstore();

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
    
    /**
     * tests whether AsColumns produce valid SQL code
     * @throws Exception if the test fails
     */
    public void testAsColumn() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.addAsColumn("ALIASNAME", AuthorPeer.NAME);
        // we need an additional column to select from,
        // to indicate the table we want use
        criteria.addSelectColumn(AuthorPeer.AUTHOR_ID);
        BasePeer.doSelect(criteria);
    }
    
    /**
     * Test whether same column name in different tables
     * are handled correctly
     * @throws Exception if the test fails
     */
    public void testSameColumnName() throws Exception
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
    
    /**
     * Tests the date, time and datetime accuracy.
     * At the moment, no upper limit for the accuracy is checked,
     * the differences are printed to stdout.
     * @throws Exception if the test fails
     */
    public void testDateTime() throws Exception
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
    
    /**
     * tests whether large primary keys are inserted and read correctly 
     * @throws Exception if the test fails
     */
    public void testLargePk() throws Exception
    {
        // clean Date table
        Criteria criteria = new Criteria();
        criteria.add(
        		LargePkPeer.LARGE_PK_ID, 
                (Long) null, 
                Criteria.NOT_EQUAL);
        LargePkPeer.doDelete(criteria);

        long longId = 8771507845873286l;
        LargePk largePk = new LargePk();
        largePk.setLargePkId(longId);
        largePk.setName("testLargePk");
        largePk.save();
        
        List largePkList = LargePkPeer.doSelect(new Criteria());
        LargePk readLargePk = (LargePk) largePkList.get(0);
        assertTrue("the inserted Id, " + largePk.getLargePkId()
        		+ " , and the read id, " + readLargePk.getLargePkId()
				+ " , should be equal",
				readLargePk.getLargePkId() == largePk.getLargePkId());
        assertTrue("the inserted Id, " + largePk.getLargePkId()
        		+ " , should be equal to " + longId,
        		longId == largePk.getLargePkId());
    }
    
    /**
     * Tests the CountHelper class
     * @throws Exception if the test fails
     */
    public void testCountHelper() throws Exception
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
    
    
    /**
     * Tests whether we can handle multiple primary keys some of which are 
     * also foreign keys
     * @throws Exception if the test fails
     */
    public void testMultiplePrimaryForeignKey() throws Exception
    {
        IntegerPk integerPk = new IntegerPk();
        integerPk.save();
        MultiPkForeignKey multiPkForeignKey = new MultiPkForeignKey();
        multiPkForeignKey.setId(10);
        multiPkForeignKey.setIntegerPk(integerPk);
        multiPkForeignKey.save();
        integerPk.save();
    }
        
    /**
     * Tests inserting single quotes in Strings.
     * This may not crash now, but in a later task like datasql, 
     * so the data has to be inserted in a table which does not get cleaned
     * during the runtime test.
     * @throws Exception if inserting the test data fails
     */
    public void testSingleQuotes() throws Exception
    {
        // clean A table
        Criteria criteria = new Criteria();
        criteria.add(APeer.A_ID, (Long) null, Criteria.NOT_EQUAL);
        APeer.doDelete(criteria);
        
        A a = new A();
        a.setName("has Single ' Quote");
        a.save();
    }
    
    
    /**
     * check that blob cloumns can be read and written correctly
     * @throws Exception if the test fails
     */
    public void testLobs() throws Exception
    {
        // clean LobTest table
        {
            Criteria criteria = new Criteria();
            criteria.add(
                    BlobTestPeer.ID, 
                    (Long) null, 
                    Criteria.NOT_EQUAL);
            BlobTestPeer.doDelete(criteria);
        }

        // create a new BlobTest Object with large blob and clob values
        // and save it
        BlobTest blobTest = new BlobTest();
        {
            int length = 100000;
            byte[] bytes = new byte[length];
            StringBuffer chars = new StringBuffer();
            String charTemplate = "1234567890abcdefghijklmnopqrstuvwxyz";
            for (int i = 0; i < length; ++i)
            {
          	    bytes[i] = new Integer(i % 256).byteValue();
                chars.append(charTemplate.charAt(i % charTemplate.length()));
            }
            blobTest.setBlobValue(bytes);
        }
        blobTest.save();
        
        // read the BlobTests from the database
        // and check the values against the original values
        List lobTestList = BlobTestPeer.doSelect(new Criteria());
        assertTrue("blobTestList should contain 1 object but contains " 
                + lobTestList.size(),
                lobTestList.size() == 1);
        
        BlobTest readBlobTest = (BlobTest) lobTestList.get(0);        
        assertTrue("read and written blobs should be equal. "
                + "Size of read blob is"
                + readBlobTest.getBlobValue().length
                + " size of written blob is "
                + blobTest.getBlobValue().length, 
                Arrays.equals(
                        blobTest.getBlobValue(),
                        readBlobTest.getBlobValue()));
    }
        
        
    /**
     * check that clob cloumns can be read and written correctly
     * @throws Exception if the test fails
     */
    public void testClobs() throws Exception
    {
        // clean ClobTest table
        {
            Criteria criteria = new Criteria();
            criteria.add(
                    ClobTestPeer.ID, 
                    (Long) null, 
                    Criteria.NOT_EQUAL);
            ClobTestPeer.doDelete(criteria);
        }

        // create a new ClobTest Object with a large clob value
        // and save it
        ClobTest clobTest = new ClobTest();
        {
            int length = 10000;
            StringBuffer chars = new StringBuffer();
            String charTemplate = "1234567890abcdefghijklmnopqrstuvwxyz";
            for (int i = 0; i < length; ++i)
            {
                 chars.append(charTemplate.charAt(i % charTemplate.length()));
            }
            clobTest.setClobValue(chars.toString());
        }
        clobTest.save();
        
        // read the ClobTests from the database
        // and check the values against the original values
        List clobTestList = ClobTestPeer.doSelect(new Criteria());
        assertTrue("clobTestList should contain 1 object but contains " 
                + clobTestList.size(),
                clobTestList.size() == 1);
        
        ClobTest readClobTest = (ClobTest) clobTestList.get(0);
        assertTrue("read and written clobs should be equal", 
                clobTest.getClobValue().equals(readClobTest.getClobValue()));
    }
        
        
    /**
     * Deletes all authors and books in the bookstore tables
     * @throws Exception if the bookstore could not be cleaned
     */
    protected void cleanBookstore() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.add(BookPeer.BOOK_ID, (Long) null, Criteria.NOT_EQUAL);
        BookPeer.doDelete(criteria);

        criteria.clear();
        criteria.add(
                AuthorPeer.AUTHOR_ID, 
                (Long) null, Criteria.NOT_EQUAL);
        AuthorPeer.doDelete(criteria);
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
