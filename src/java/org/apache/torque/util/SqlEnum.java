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


/**
 * A typesafe enum of SQL string fragments.  Used by Criteria and SqlExpression
 * to build queries.  Criteria also makes most of the constants available
 * in order to specify a criterion.
 *
 * @author <a href="mailto:jmcnally@collab,net"></a>
 * @version $Id$
 * @since 3.0
 */
class SqlEnum 
{
    private final String s;
    
    private SqlEnum(String s) 
    { 
        this.s = s;
    }

    public final String toString()  
    { 
        return s;
    }
    
    public static final SqlEnum EQUAL = 
        new SqlEnum("=");
    public static final SqlEnum NOT_EQUAL = 
            new SqlEnum("<>");
    public static final SqlEnum ALT_NOT_EQUAL = 
        new SqlEnum("!=");
    public static final SqlEnum GREATER_THAN =
        new SqlEnum(">");
    public static final SqlEnum LESS_THAN =
        new SqlEnum("<");
    public static final SqlEnum GREATER_EQUAL =
        new SqlEnum(">=");
    public static final SqlEnum LESS_EQUAL =
        new SqlEnum("<=");  
    public static final SqlEnum LIKE =
        new SqlEnum(" LIKE ");
    public static final SqlEnum NOT_LIKE =
        new SqlEnum(" NOT LIKE ");
    public static final SqlEnum IN =
        new SqlEnum(" IN ");
    public static final SqlEnum NOT_IN =
        new SqlEnum(" NOT IN ");
    public static final SqlEnum CUSTOM =
        new SqlEnum("CUSTOM");
    public static final SqlEnum JOIN =
        new SqlEnum("JOIN");
    public static final SqlEnum DISTINCT =
        new SqlEnum("DISTINCT ");
    public static final SqlEnum ALL =
        new SqlEnum("ALL ");
    public static final SqlEnum ASC =
        new SqlEnum("ASC");
    public static final SqlEnum DESC =
        new SqlEnum("DESC");
    public static final SqlEnum ISNULL =
        new SqlEnum(" IS NULL ");
    public static final SqlEnum ISNOTNULL =
        new SqlEnum(" IS NOT NULL ");
}
