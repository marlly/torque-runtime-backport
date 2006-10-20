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

import java.io.Serializable;
import org.apache.torque.TorqueException;

/**
 * This class can be used to uniquely identify an object within
 * an application.  There are four subclasses: StringKey, NumberKey,
 * and DateKey, and ComboKey which is a Key made up of a combination
 * ofthe first three.
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @version $Id$
 */
public abstract class ObjectKey implements Serializable, Comparable
{
    /**
     * The underlying key value.
     */
    protected Object key;

    /**
     * Initializes the internal key value to <code>null</code>.
     */
    protected ObjectKey()
    {
        key = null;
    }

    /**
     * Returns the hashcode of the underlying value (key), if key is
     * not null.  Otherwise calls Object.hashCode()
     *
     * @return an <code>int</code> value
     */
    public int hashCode()
    {
        if (key == null)
        {
            return super.hashCode();
        }
        return key.hashCode();
    }

    /**
     * Get the underlying object.
     *
     * @return the underlying object
     */
    public Object getValue()
    {
        return key;
    }

    /**
     * Appends a String representation of the key to a buffer.
     *
     * @param sb a <code>StringBuffer</code>
     */
    public void appendTo(StringBuffer sb)
    {
        sb.append(this.toString());
    }

    /**
     * Implements the compareTo method.
     *
     * @param obj the object to compare to this object
     * @return a numeric comparison of the two values
     */
    public int compareTo(Object obj)
    {
        return toString().compareTo(obj.toString());
    }

    /**
     * Reset the underlying object using a String.
     *
     * @param s a <code>String</code> value
     * @exception TorqueException if an error occurs
     */
    public abstract void setValue(String s) throws TorqueException;
}
