package org.apache.torque.util;

/*
 * Copyright 2001,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
class SqlEnum implements java.io.Serializable
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
    public static final SqlEnum ILIKE =
        new SqlEnum(" ILIKE ");
    public static final SqlEnum NOT_ILIKE =
        new SqlEnum(" NOT ILIKE ");
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
    public static final SqlEnum CURRENT_DATE =
        new SqlEnum("CURRENT_DATE");
    public static final SqlEnum CURRENT_TIME =
        new SqlEnum("CURRENT_TIME");
}
