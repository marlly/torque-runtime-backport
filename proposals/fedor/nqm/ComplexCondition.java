package org.apache.turbine.util.db.nqm;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Java Core Classes
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Collection;

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 * @version $Id$
 */
public abstract class ComplexCondition implements Condition
{
    public static final String OPEN_PARENS = "(";
    public static final String CLOSE_PARENS = ")";

    private Vector conditions = new Vector();

    public ComplexCondition()
    {
    }

    public ComplexCondition(Condition condition)
    {
        this.conditions.add(condition);
    }

    public ComplexCondition(Collection conditions)
    {
        this.conditions.addAll(conditions);
    }

    public abstract String getOperator(DB db);

    public ComplexCondition add(Condition condition)
    {
        this.conditions.add(condition);
        return this;
    }

    public ComplexCondition addAll(Collection conditions)
    {
        this.conditions.addAll(conditions);
        return this;
    }
    /**
     *  method returns SQL representation of the condition
     *
     */
    public StringStackBuffer toSQL(DB db)
    {
        if (this.conditions.size()==0)
        {
            return null;
        }
        StringStackBuffer ssb =((Condition)this.conditions.get(0)).toSQL(db);
        for (int i=1; i<this.conditions.size(); i++)
        {
            ssb.add(getOperator(db));
            Condition cond = (Condition)this.conditions.get(i);
            if (cond instanceof ComplexCondition)
            {
                ssb.add(OPEN_PARENS)
                    .addAll(((Condition)this.conditions.get(i)).toSQL(db))
                    .add(CLOSE_PARENS);
            }
            ssb.addAll(((Condition)this.conditions.get(i)).toSQL(db));
        }
        return ssb;
    }

    public Set getTables()
    {
        HashSet tables = new HashSet(this.conditions.size());
        for (int i=0; i<this.conditions.size(); i++)
        {
            tables.addAll(((Condition)this.conditions.get(i)).getTables());
        }
        return tables;
    }
}
