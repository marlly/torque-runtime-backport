package org.apache.torque.engine.sql;

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

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.ArrayList;

/**
 * A simple Scanner implementation that scans an
 * sql file into usable tokens.  Used by SQLToAppData.
 *
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:andyhot@di.uoa.gr">Andreas Andreou</a>
 * @version $Id$
 */
public class SQLScanner
{
    /** white spaces */
    private static final String WHITE = "\f\r\t\n ";
    /** alphabetic characters */
    private static final String ALFA
            = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** numbers */
    private static final String NUMER = "0123456789";
    /** alphanumeric */
    private static final String ALFANUM = ALFA + NUMER;
    /** special characters */
    private static final String SPECIAL = ";(),'";
    /** comment */
    private static final char COMMENT_POUND = '#';
    /** comment */
    private static final char COMMENT_SLASH = '/';
    /** comment */
    private static final char COMMENT_STAR = '*';
    /** comment */
    private static final char COMMENT_DASH = '-';

    /** the input reader */
    private Reader in;
    /** character */
    private int chr;
    /** token */
    private String token;
    /** list of tokens */
    private List tokens;
    /** line */
    private int line;
    /** column */
    private int col;

    /**
     * Creates a new scanner with no Reader
     */
    public SQLScanner()
    {
        this(null);
    }

    /**
     * Creates a new scanner with an Input Reader
     *
     * @param input the input reader
     */
    public SQLScanner(Reader input)
    {
        setInput(input);
    }

    /**
     * Set the Input
     *
     * @param input the input reader
     */
    public void setInput(Reader input)
    {
        in = input;
    }


    /**
     * Reads the next character and increments the line and column counters.
     *
     * @throws IOException If an I/O error occurs
     */
    private void readChar() throws IOException
    {
        boolean wasLine = (char) chr == '\r';
        chr = in.read();
        if ((char) chr == '\n' || (char) chr == '\r' || (char) chr == '\f')
        {
            col = 0;
            if (!wasLine || (char) chr != '\n')
            {
                line++;
            }
        }
        else
        {
            col++;
        }
    }

    /**
     * Scans an identifier.
     *
     * @throws IOException If an I/O error occurs
     */
    private void scanIdentifier () throws IOException
    {
        token = "";
        char c = (char) chr;
        while (chr != -1 && WHITE.indexOf(c) == -1 && SPECIAL.indexOf(c) == -1)
        {
            token = token + (char) chr;
            readChar();
            c = (char) chr;
        }
        int start = col - token.length();
        tokens.add(new Token(token, line, start));
    }

    /**
     * Scans an identifier which had started with the negative sign.
     *
     * @throws IOException If an I/O error occurs
     */
    private void scanNegativeIdentifier () throws IOException
    {
        token = "-";
        char c = (char) chr;
        while (chr != -1 && WHITE.indexOf(c) == -1 && SPECIAL.indexOf(c) == -1)
        {
            token = token + (char) chr;
            readChar();
            c = (char) chr;
        }
        int start = col - token.length();
        tokens.add(new Token(token, line, start));
    }

    /**
     * Scan the input Reader and returns a list of tokens.
     *
     * @return a list of tokens
     * @throws IOException If an I/O error occurs
     */
    public List scan () throws IOException
    {
        line = 1;
        col = 0;
        boolean inComment = false;
        boolean inCommentSlashStar = false;
        boolean inCommentDash = false;

        boolean inNegative;

        tokens = new ArrayList();
        readChar();
        while (chr != -1)
        {
            char c = (char) chr;
            inNegative = false;

            if (c == COMMENT_DASH)
            {
                readChar();
                if ((char) chr == COMMENT_DASH)
                {
                    inCommentDash = true;
                }
                else
                {
                    inNegative = true;
                    c = (char) chr;
                }
            }

            if (inCommentDash)
            {
                if (c == '\n' || c == '\r')
                {
                    inCommentDash = false;
                }
                readChar();
            }
            else if (c == COMMENT_POUND)
            {
                inComment = true;
                readChar();
            }
            else if (c == COMMENT_SLASH)
            {
                readChar();
                if ((char) chr == COMMENT_STAR)
                {
                    inCommentSlashStar = true;
                }
            }
            else if (inComment || inCommentSlashStar)
            {
                if (c == '*')
                {
                    readChar();
                    if ((char) chr == COMMENT_SLASH)
                    {
                        inCommentSlashStar = false;
                    }
                }
                else if (c == '\n' || c == '\r')
                {
                    inComment = false;
                }
                readChar();
            }
            else if (ALFANUM.indexOf(c) >= 0)
            {
                if (inNegative)
                {
                    scanNegativeIdentifier();
                }
                else
                {
                    scanIdentifier();
                }
            }
            else if (SPECIAL.indexOf(c) >= 0)
            {
                tokens.add(new Token("" + c, line, col));
                readChar();
            }
            else
            {
                readChar();
            }
        }
        return tokens;
    }
}
