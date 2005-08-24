package org.apache.torque;

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

import java.util.List;

import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;
import org.apache.torque.test.Book;
import org.apache.torque.test.BookPeer;
import org.apache.torque.test.bean.AuthorBean;
import org.apache.torque.test.bean.BookBean;
import org.apache.torque.util.Criteria;

/**
 * Runtime tests to make sure that the code which is supplied
 * in the documentation actually works ;-)
 *
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
public class BeanTest extends BaseRuntimeTestCase
{
    public static final String AUTHOR_1_NAME = "Joshua Bloch";

    public static final int AUTHOR_1_ID = 123;

    public static final String BOOK_1_TITLE = "Effective Java";

    public static final String BOOK_1_ISBN = "0-618-12902-2";

    public static final int BOOK_1_ID = 456;

    public static final String AUTHOR_2_NAME = "W. Stevens";

    /**
     * Creates a new instance.
     */
    public BeanTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        super.setUp();
    }

    /**
     * tests the creation of beans from objects and vice versa
     */
    public void testCreateBeans() throws Exception
    {
        Author author = new Author();
        author.setName(AUTHOR_1_NAME);
        author.setAuthorId(AUTHOR_1_ID);

        AuthorBean authorBean = author.getBean();
        assertTrue("bean.getName() is " + authorBean.getName()
                + " should be " + author.getName(),
                author.getName().equals(authorBean.getName()));
        assertTrue("bean.getId() is " + authorBean.getAuthorId()
                + " should be " + AUTHOR_1_ID,
                author.getAuthorId() == authorBean.getAuthorId());

        Author authorFromBean = Author.createAuthor(authorBean);
        assertTrue("author from bean has name " + authorFromBean.getName()
                + " should be " + author.getName(),
                author.getName().equals(authorFromBean.getName()));
        assertTrue("author from bean has Id " + authorFromBean.getAuthorId()
                + " should be " + author.getAuthorId(),
                author.getAuthorId() == authorBean.getAuthorId());
    }

    /**
     * tests whether object relations are transferred correctly,
     * if two objects refer to each other
     */
    public void testSameObjectRelations() throws Exception
    {
        Author author = new Author();
        author.setAuthorId(AUTHOR_1_ID);

        Book book = new Book();
        book.setBookId(BOOK_1_ID);

        author.addBook(book);
        book.setAuthor(author);

        // check one roundtrip from author
        assertTrue("author from book should be the same object as author",
                author == book.getAuthor());

        AuthorBean authorBean = author.getBean();
        BookBean bookBean = (BookBean) authorBean.getBookBeans().get(0);
        assertTrue("authorBean from BookBean should be the same "
                + "object as authorBean",
                bookBean.getAuthorBean() == authorBean);

        author = Author.createAuthor(authorBean);
        book = (Book) author.getBooks().get(0);

        assertTrue("author from book should be the same object as author "
                + "after creating from bean",
                author == book.getAuthor());

        // check one roundtrip from book
        assertTrue("book from author should be the same object as book",
                book == author.getBooks().get(0));

        bookBean = book.getBean();
        authorBean = bookBean.getAuthorBean();
        assertTrue("bookBean from authorBean should be the same "
                + "object as bookBean",
                authorBean.getBookBeans().get(0) == bookBean);

        book = Book.createBook(bookBean);
        author = book.getAuthor();

        assertTrue("book from author should be the same object as book "
                + "after creating from bean",
                author.getBooks().get(0) == book);
    }

    /**
     * tests whether object relations are transferred correctly,
     * if there is no mutual reference between objects
     * @throws Exception
     */
    public void testDifferentObjectRelations() throws Exception
    {
        // create a relation chain:
        //
        //      getBooks()  getAuthor()          getBooks()
        //         |            |                    |
        // author ----> book -----> differentAuthor ---> differentBook
        Author author = new Author();
        author.setAuthorId(AUTHOR_1_ID);

        Book book = new Book();
        book.setBookId(BOOK_1_ID);

        Author differentAuthor = new Author();
        author.setAuthorId(AUTHOR_1_ID);

        author.addBook(book);
        book.setAuthor(differentAuthor);

        Book differentBook = new Book();
        book.setBookId(BOOK_1_ID);

        differentAuthor.addBook(differentBook);

        // check one roundtrip from author
        assertTrue("author from book should not be the same object as author",
                author != book.getAuthor());

        AuthorBean authorBean = author.getBean();
        BookBean bookBean = (BookBean) authorBean.getBookBeans().get(0);
        assertTrue("authorBean from BookBean should not be the same "
                + "object as authorBean",
                bookBean.getAuthorBean() != authorBean);

        author = Author.createAuthor(authorBean);
        book = (Book) author.getBooks().get(0);

        assertTrue("author from book should not be the same object as author "
                + "after creating from bean",
                author != book.getAuthor());

        // check one roundtrip from book
        assertTrue("book from differentAuthor should not be "
                + "the same object as book",
                book != differentAuthor.getBooks().get(0));

        bookBean = book.getBean();
        AuthorBean differentAuthorBean = bookBean.getAuthorBean();
        assertTrue("bookBean from differentAuthorBean should not be the same "
                + "object as bookBean",
                differentAuthorBean.getBookBeans().get(0) != bookBean);

        book = Book.createBook(bookBean);
        differentAuthor = book.getAuthor();

        assertTrue("book from differentAuthor should not be "
                + "the same object as book "
                + "after creating from bean",
                differentAuthor.getBooks().get(0) != book);
    }

    public void testSaves() throws Exception
    {
        Criteria criteria = new Criteria();
        criteria.add(BookPeer.BOOK_ID, (Long) null, Criteria.NOT_EQUAL);
        BookPeer.doDelete(criteria);

        criteria = new Criteria();
        criteria.add(AuthorPeer.AUTHOR_ID, (Long) null, Criteria.NOT_EQUAL);
        AuthorPeer.doDelete(criteria);

        Author author = new Author();
        author.setName(AUTHOR_1_NAME);
        author.save();

        assertFalse("isModified() should return false after save",
                author.isModified());
        assertFalse("isNew() should return false after save",
                author.isNew());

        AuthorBean authorBean = author.getBean();

        assertFalse("bean.isModified() should return false after save "
                + "and bean creation",
                authorBean.isModified());
        assertFalse("bean.isNew() should return false after save "
                + "and bean creation",
                authorBean.isNew());

        author = Author.createAuthor(authorBean);

        assertFalse("isModified() should return false after save "
                + "and bean roundtrip",
                author.isModified());
        assertFalse("isNew() should return false after save "
                + "and bean rounddtrip",
                author.isNew());

        authorBean.setName(AUTHOR_2_NAME);
        assertTrue("bean.isModified() should return true after it was modified",
                authorBean.isModified());
        assertFalse("bean.isNew() should still return false "
                + "after bean creation and modification",
                authorBean.isNew());

        author = Author.createAuthor(authorBean);
        assertTrue("isModified() should return true after creation of object "
                + "from modified bean",
                author.isModified());

        author.save();

        List authorList = AuthorPeer.doSelect(new Criteria());
        Author readAuthor = (Author) authorList.get(0);
        assertEquals("name from read Author is " + readAuthor.getName()
                +" but should be " + authorBean.getName(),
                readAuthor.getName(),
                authorBean.getName());

        BookBean bookBean = new BookBean();
        bookBean.setTitle(BOOK_1_TITLE);
        bookBean.setIsbn(BOOK_1_ISBN);

        Book book = Book.createBook(bookBean);
        assertTrue("isModified() should return true after creation of object "
                + " from new bean",
                book.isModified());
        assertTrue("isNew() should return true after creation of object "
                + " from new bean",
                book.isNew());
        book.setAuthor(author);
        book.save();

        List bookList = BookPeer.doSelect(new Criteria());
        assertTrue("Ther should be one book in DB but there are "
                + bookList.size(),
                bookList.size() == 1);
    }
}
