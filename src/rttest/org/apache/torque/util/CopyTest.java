package org.apache.torque.util;

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

import org.apache.torque.BaseTestCase;
import org.apache.torque.test.Author;
import org.apache.torque.test.AuthorPeer;
import org.apache.torque.test.Book;

/**
 * Test code for TorqueObject.copy().
 *
 * @author <a href="mailto:torque@kivus.myip.org">Rafal Maczewski</a>
 * @version $Id $
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
