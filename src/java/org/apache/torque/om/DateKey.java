package org.apache.torque.om;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.util.Date;

/**
 * This class can be used as an ObjectKey to uniquely identify an
 * object within an application where the id is a Date.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public class DateKey extends SimpleKey
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3102583536685348517L;

    /**
     * Creates an DateKey whose internal representation will be
     * set later, through a set method
     */
    public DateKey()
    {
    }

    /**
     * Creates a DateKey whose internal representation is a Date
     * given by the long number given by the String
     *
     * @param key the key value
     * @throws NumberFormatException if key is not valid
     */
    public DateKey(String key) throws NumberFormatException
    {
        this.key = new Date(Long.parseLong(key));
    }

    /**
     * Creates a DateKey
     *
     * @param key the key value
     */
    public DateKey(Date key)
    {
        this.key = key;
    }

    /**
     * Creates a DateKey that is equivalent to key.
     *
     * @param key the key value
     */
    public DateKey(DateKey key)
    {
        if (key != null)
        {
            this.key = key.getValue();
        }
        else
        {
            this.key = null;
        }
    }

    /**
     * Sets the internal representation to a String
     *
     * @param key the key value
     */
    public void setValue(String key)
    {
        this.key = new Date(Long.parseLong(key));
    }

    /**
     * Sets the internal representation to the same object used by key.
     *
     * @param key the key value
     */
    public void setValue(DateKey key)
    {
        if (key != null)
        {
            this.key = key.getValue();
        }
        else
        {
            this.key = null;
        }
    }

    /**
     * Access the underlying Date object.
     *
     * @return a <code>Date</code> value
     */
    public Date getDate()
    {
        return (Date) key;
    }

    /**
     * keyObj is equal to this DateKey if keyObj is a DateKey or String
     * that contains the same information this key contains.  Two ObjectKeys
     * that both contain null values are not considered equal.
     *
     * @param keyObj the comparison value
     * @return whether the two objects are equal
     */
    public boolean equals(Object keyObj)
    {
        boolean isEqual = false;

        if (key != null)
        {
            if (keyObj instanceof String)
            {
                isEqual =  toString().equals(keyObj);
            }
            // check against a DateKey. Two keys are equal, if their
            // internal keys equivalent.
            else if (keyObj instanceof DateKey)
            {
                Object obj = ((DateKey) keyObj).getValue();
                isEqual =  key.equals(obj);
            }
        }
        return isEqual;
    }

    /**
     * get a String representation
     *
     * @return a String representation of the Date or an empty String if the
     *          Date is null
     */
    public String toString()
    {
        Date dt = getDate();
        return (dt == null ? "" : Long.toString(dt.getTime()));
    }
}
