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

import java.util.List;

import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;
import org.apache.torque.test.Book;
import org.apache.torque.test.BookPeer;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Criteria;

/**
 * Runtime tests to make sure that the code which is supplied
 * in the documentation actually works ;-)
 *
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
public class DocsTest extends BaseTestCase
{
    public static final String AUTHOR_1_NAME = "Joshua Bloch";
    
    public static final String AUTHOR_2_NAME = "W. Stevens";

    public static final String AUTHOR_3_NAME = "Author without book";
    
    public static final String BOOK_1_TITLE = "Effective Java";
    
    public static final String BOOK_1_ISBN = "0-618-12902-2";
    
    public static final String BOOK_2_TITLE = "TCP/IP Illustrated";
    
    public static final String BOOK_2_ISBN = "0-201-63346-9";
  
    public static final String BOOK_3_TITLE = "TCP/IP Illustrated";
    
    public static final String BOOK_3_ISBN = "0-201-63354-X";
        
    /**
     * Creates a new instance.
     */
    public DocsTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        super.setUp();

        // clean the books database
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
        
        
        // insert some data into the database
        // taken from tutorial step 4 with some changes
        try 
        {  
            Author bloch = new Author();
            bloch.setName(AUTHOR_1_NAME);
            bloch.save();

            Author stevens = new Author();
            stevens.setName(AUTHOR_2_NAME);
            AuthorPeer.doInsert(stevens);

            Author withoutBook = new Author();
            withoutBook.setName(AUTHOR_3_NAME);
            AuthorPeer.doInsert(withoutBook);

            Book effective = new Book();
            effective.setTitle(BOOK_1_TITLE);
            effective.setIsbn(BOOK_1_ISBN);
            effective.setAuthor(bloch);
            effective.save();

            Book tcpip = new Book();
            tcpip.setTitle(BOOK_2_TITLE);
            tcpip.setIsbn(BOOK_2_ISBN);
            tcpip.setAuthorId(stevens.getAuthorId());
            tcpip.save();

            Book tcpipVolTwo = new Book();
            tcpipVolTwo.setTitle(BOOK_3_TITLE);
            tcpipVolTwo.setIsbn(BOOK_3_ISBN);
            tcpipVolTwo.setAuthorId(stevens.getAuthorId());
            tcpipVolTwo.save();

        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
    }

    /**
     * Criteria howto, section "Order by".
     */
    public void testCriteriaOrderBy() 
    {
        List books = null;
        try 
        {
            Criteria criteria = new Criteria();
            criteria.addAscendingOrderByColumn(BookPeer.TITLE);
            criteria.addAscendingOrderByColumn(BookPeer.ISBN);
    
            books = BookPeer.doSelect(criteria);            
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        
        Book book = (Book) books.get(0);
        assertTrue(
                "title of first book is not"
                + BOOK_1_TITLE
                + " but "
                + book.getTitle(),
                BOOK_1_TITLE.equals(book.getTitle())
                );

        book = (Book) books.get(2);
        assertTrue(
                "ISBN of third book is not"
                + BOOK_3_ISBN
                + " but "
                + book.getIsbn(),
                BOOK_3_ISBN.equals(book.getIsbn()));
    }
    
    /**
     * Criteria howto, section "Order by".
     */
    public void testCriteriaJoins() 
    {
        // inner joins
        List bookAuthors = null;
        try 
        {
            Criteria criteria = new Criteria();
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, 
                    BookPeer.AUTHOR_ID, 
                    Criteria.INNER_JOIN);
  
            bookAuthors = AuthorPeer.doSelect(criteria);
            
            // from Details section
            Author author = (Author) bookAuthors.get(0);
            List books = author.getBooks();
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("inner join : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        
        assertTrue(
                "inner join : size of bookAuthors is not 3, but"
                + bookAuthors.size(),
                bookAuthors.size() == 3);
        
        // test explicit sql statements from details section
        List result = null;
        try 
        {
            result = BasePeer.executeQuery(
                    "SELECT BOOK.* FROM BOOK "
                    + "INNER JOIN AUTHOR "
                    + "ON BOOK.AUTHOR_ID=AUTHOR.AUTHOR_ID");
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("Explicit SQL query 1 : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        
        assertTrue(
            "Explicit SQL query 1 : size of result is not 3, but"
            + result.size(),
            result.size() == 3);

        result = null;
        try 
        {
            result = BasePeer.executeQuery(
                    "SELECT BOOK.* FROM BOOK,AUTHOR "
                    + "WHERE BOOK.AUTHOR_ID=AUTHOR.AUTHOR_ID");
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("Explicit SQL query 2 : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        
        assertTrue(
            "Explicit SQL query 2 : size of result is not 3, but"
            + result.size(),
            result.size() == 3);
        
        // test left outer join
        bookAuthors = null;
        try 
        {
              Criteria criteria = new Criteria();
              criteria.addJoin(
                      AuthorPeer.AUTHOR_ID, 
                      BookPeer.AUTHOR_ID, 
                      Criteria.LEFT_JOIN);
              bookAuthors = AuthorPeer.doSelect(criteria);
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("left join : Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }

        assertTrue(
            "left join : size of bookAuthors is not 4, but"
            + bookAuthors.size(),
            bookAuthors.size() == 4);

    }
    
    /**
     * Criteria Howto, section "Distinct".
     */
    public void testDistinct() 
    {
        List bookAuthors = null;
        try 
        {
            Criteria criteria = new Criteria();
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, 
                    BookPeer.AUTHOR_ID, 
                    Criteria.INNER_JOIN);
            criteria.setDistinct();
        
            bookAuthors = AuthorPeer.doSelect(criteria);
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        assertTrue(
            "size of bookAuthors is not 2, but"
            + bookAuthors.size(),
            bookAuthors.size() == 2);
    }
    
    /**
     * Criteria Howto, section "Join & Order & Distinct".
     */
    public void testJoinOrderDistinct() 
    {
        List bookAuthors = null;
        try 
        {
            Criteria criteria = new Criteria();
            criteria.addJoin(
                    AuthorPeer.AUTHOR_ID, 
                    BookPeer.AUTHOR_ID, 
                    Criteria.INNER_JOIN);
            criteria.setDistinct();
            criteria.addAscendingOrderByColumn(AuthorPeer.NAME);
                  
            bookAuthors = AuthorPeer.doSelect(criteria);
        }
        catch (Exception e) 
        { 
            e.printStackTrace();
            fail("Exception caught : " 
                     + e.getClass().getName() 
                     + " : " + e.getMessage());
        }
        assertTrue(
            "size of bookAuthors is not 2, but"
            + bookAuthors.size(),
            bookAuthors.size() == 2);
        
        Author author = (Author) bookAuthors.get(0);
        assertTrue(
            "Author of first book is not"
            + AUTHOR_1_NAME
            + " but "
            + author.getName(),
            AUTHOR_1_NAME.equals(author.getName()));
  
        author = (Author) bookAuthors.get(1);
        assertTrue(
            "Author of second book is not"
            + AUTHOR_2_NAME
            + " but "
            + author.getName(),
            AUTHOR_2_NAME.equals(author.getName()));
    }
}
