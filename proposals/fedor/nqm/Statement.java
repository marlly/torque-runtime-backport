package org.apache.turbine.util.db.nqm;

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

// Java Core Classes
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 * @version $Id$
 */
public abstract class Statement implements SQLCode
{
    public static final String WHERE = SQLConstants.WHERE;
    public static final String FROM  = SQLConstants.FROM;
    public static final String COMMA  = SQLConstants.COMMA;

    private Condition condition = null;
    private Set tables = new HashSet();

    public Statement()
    {
    }

    public Statement(Condition condition)
    {
        this();
        setCondition(condition);
    }

    protected StringStackBuffer whereClause(DB db)
    {
        StringStackBuffer clause = new StringStackBuffer();
        if (condition!=null)
        {
            clause.add(WHERE).addAll(condition.toSQL(db));
        }
        return clause;
    }

    protected StringStackBuffer fromClause(DB db)
    {
        StringStackBuffer clause = new StringStackBuffer();
        if (!tables.isEmpty())
        {
            Iterator i=tables.iterator();
            clause.add(FROM).add((String)i.next());
            while(i.hasNext())
            {
                clause.add(COMMA).add((String)i.next());
            }
        }
        return clause;
    }

    public Statement setCondition(Condition condition)
    {
        this.condition = condition;
        this.tables.addAll(condition.getTables());
        return this;
    }

    public Condition getCondition()
    {
        return this.condition;
    }

    public abstract StringStackBuffer toSQL(DB db);
}
