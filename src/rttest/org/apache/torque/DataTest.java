package org.apache.torque;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import org.apache.log4j.Category;
import org.apache.torque.BaseTestCase;
import org.apache.torque.test.Author;
import org.apache.torque.test.Book;
import org.apache.torque.test.BookPeer;
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
        for (int j=0; j<validTitles.length; j++) 
        {
            titleMap.put(validTitles[j], null);
        }

        try
        {
            Criteria crit = new Criteria();
            Criteria.Criterion c = crit.getNewCriterion(BookPeer.TITLE, (Object)"Book 6 - Author 1", Criteria.GREATER_EQUAL);
            c.and(crit.getNewCriterion(BookPeer.TITLE, (Object)"Book 8 - Author 3", Criteria.LESS_EQUAL));
            crit.add(c);
            crit.addDescendingOrderByColumn(BookPeer.BOOK_ID);
            crit.setLimit(10);
            crit.setOffset(5);
            List books = BookPeer.doSelect(crit);
            assertTrue("List should have 10 books, not " + books.size(), 
                       books.size() == 10);
            for (Iterator i=books.iterator(); i.hasNext();) 
            {
                String title = ((Book)i.next()).getTitle();
                assertTrue("Incorrect title: " + title, 
                           titleMap.containsKey(title));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
