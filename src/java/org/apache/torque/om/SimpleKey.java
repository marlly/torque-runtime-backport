package org.apache.torque.om;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
 * This empty class  marks an ObjectKey as being capable of being
 * represented as a single column in a database.  
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:drfish@cox.net">J. Russell Smyth</a>
 * @version $Id$
 */
public abstract class SimpleKey extends ObjectKey
{
    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(java.math.BigDecimal key)
    {
        return new NumberKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(int key)
    {
        return new NumberKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(long key)
    {
        return new NumberKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(double key)
    {
        return new NumberKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(Number key)
    {
        return new NumberKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(NumberKey key)
    {
        return new NumberKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(String key)
    {
        return new StringKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(StringKey key)
    {
        return new StringKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(java.util.Date key)
    {
        return new DateKey(key);
    }

    /**
     * Creates a SimpleKey equivalent to key
     * @param key the key value
     * @return a SimpleKey
     */    
    public static SimpleKey keyFor(DateKey key)
    {
        return new DateKey(key);
    }
}
