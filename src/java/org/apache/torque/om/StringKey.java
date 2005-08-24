package org.apache.torque.om;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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


/**
 * This class can be used as an ObjectKey to uniquely identify an
 * object within an application where the id  consists
 * of a single entity such a GUID or the value of a db row's primary key.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public class StringKey extends SimpleKey
{
    /**
     * Creates an SimpleKey whose internal representation will be
     * set later, through a set method
     */
    public StringKey()
    {
    }

    /**
     * Creates a StringKey whose internal representation is a String
     *
     * @param key the key value
     */
    public StringKey(String key)
    {
        this.key = key;
    }

    /**
     * Creates a StringKey that is equivalent to key.
     *
     * @param key the key value
     */
    public StringKey(StringKey key)
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
        this.key = key;
    }

    /**
     * Sets the internal representation to the same object used by key.
     *
     * @param key the key value
     */
    public void setValue(StringKey key)
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
     * Access the underlying String object.
     *
     * @return a <code>String</code> value
     */
    public String getString()
    {
        return (String) key;
    }

    /**
     * keyObj is equal to this StringKey if keyObj is a StringKey or String
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
                isEqual = keyObj.equals(key);
            }
            // check against a StringKey. Two keys are equal, if their
            // internal keys equivalent.
            else if (keyObj instanceof StringKey)
            {
                Object obj = ((StringKey) keyObj).getValue();
                isEqual =  key.equals(obj);
            }
        }
        return isEqual;
    }

    /**
     * get a String representation
     *
     * @return a String representation of an empty String if the value is null
     */
    public String toString()
    {
        if (key != null)
        {
            return (String) key;
        }
        return "";
    }
}
