package org.apache.turbine.util.db.nqm;

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

// Java Core Classes
import java.util.Set;
import java.util.HashSet;

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *  A simple column condition such as "column=3" "column >=5" "column LIKE
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 * @version $Id$
 */
public abstract class Comparison implements Condition
{
    public static final String QUOTE = "'";
    public static final char DOT = '.';

    private String column;
    private Object value;
    private boolean quoteValue = true;
    private boolean autoQuoteValue = true;

    /**
     *  Default constructor
     */
    public Comparison(String column, Object value)
    {
        this.column = column;
        this.value = value;
        this.autoQuoteValue = true;
    }

    /**
     *  Constructor allowing to explicitly specify whether value should be
     *  quoted
     */
    public Comparison(String column, Object value, boolean quoteValue)
    {
        this.column = column;
        this.value = value;
        this.autoQuoteValue = false;
        this.quoteValue = quoteValue;
    }

    /**
     *  returns comparison operator
     */
    public abstract String getOperator(DB db);

    /**
     *  method returns SQL representation of the condition
     *
     */
    public StringStackBuffer toSQL(DB db)
    {
        StringStackBuffer ssb = new StringStackBuffer()
                                    .add(column)
                                    .add(getOperator(db));
        if ((autoQuoteValue && (value instanceof Number)) || !quoteValue)
        {
            ssb.add(value.toString());
        }
        else
        {
            ssb.add(QUOTE)
                .add(value.toString())
                .add(QUOTE);
        }
        return ssb;
    }

    public Set getTables()
    {
        HashSet tables;
        int dot = column.indexOf(DOT);
        if (dot<0)
        {
            tables = new HashSet(0);
        }
        else
        {
            tables = new HashSet(1);
            tables.add(column.substring(0,dot).toUpperCase());
        }
        return tables;
    }
}
