package org.apache.turbine.util.db.nqm;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
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

// Java Core Classes

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *  A simple column condition such as "column=3" "column >=5" "column LIKE
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 * @version $Id$
 */
public class Equal extends Comparison
{
    public static String OPERATOR="=";

    /**
     *  Default constructor
     */
    public Equal(String column, Object value)
    {
        super(column, value);
    }

    /**
     *  Constructor allowing to explicitly specify whether value should be
     *  quoted
     */
    public Equal(String column, Object value, boolean quoteValue)
    {
        super(column, value, quoteValue);
    }

    /**
     *  returns comparison operator (=)
     */
    public String getOperator(DB db)
    {
        return OPERATOR;
    }
}
