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
import java.util.Set;
import java.util.HashSet;

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *  A simple column condition such as "column=3" "column >=5" "column LIKE
 *
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
