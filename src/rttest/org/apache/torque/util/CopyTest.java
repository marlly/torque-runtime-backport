package org.apache.torque.util;

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

import org.apache.torque.BaseTestCase;
import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;
import org.apache.torque.test.Book;

/**
 * Test code for TorqueObject.copy().
 *
 * @author <a href="mailto:torque@kivus.myip.org">Rafal Maczewski</a>
 * @version $Id$
 */
public class CopyTest extends BaseTestCase
{
    /**
     * Creates a new instance.
     */
    public CopyTest(String name)
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
    public void testCopyObject() throws Exception
    {
                Author author = new Author();
                author.setName("Author to be copied");
                author.save();

                for (int j = 1; j <= 10; j++)
                {
                        Book book = new Book();
                        book.setAuthor(author);
                        book.setTitle("Book " + j + " - " + author.getName());
                        book.setIsbn("unknown");
                        book.save();
                }
                assertTrue("Number of books before copy should be 10, was "
                + author.getBooks().size(), author.getBooks().size() == 10);
                Author authorCopy = author.copy();
                authorCopy.save();

                author = AuthorPeer.retrieveByPK(author.getPrimaryKey());
                assertTrue("Number of books in original object should be 10, was "
                + author.getBooks().size(), author.getBooks().size() == 10);

                assertTrue("Number of books after copy should be 10, was "
                + author.getBooks().size(), authorCopy.getBooks().size() == 10);
    }
}
