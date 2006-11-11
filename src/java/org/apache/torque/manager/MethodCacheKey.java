package org.apache.torque.manager;

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

import java.io.Serializable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.torque.TorqueException;

/**
 * @version $Id$
 */
public class MethodCacheKey implements Serializable
{
    //private static final Category log =
    //    Category.getInstance("org.apache.torque");

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1831486431185021200L;

    int n;
    private Serializable instanceOrClass;
    private String method;
    private Serializable arg1;
    private Serializable arg2;
    private Serializable arg3;
    private Serializable[] moreThanThree;
    private String groupKey;

    public MethodCacheKey()
    {
    }

    public MethodCacheKey(Serializable instanceOrClass, String method)
    {
        init(instanceOrClass, method);
    }

    public MethodCacheKey(Serializable instanceOrClass, String method,
                          Serializable arg1)
    {
        init(instanceOrClass, method, arg1);
    }

    public MethodCacheKey(Serializable instanceOrClass, String method,
                          Serializable arg1, Serializable arg2)
    {
        init(instanceOrClass, method, arg1, arg2);
    }

    public MethodCacheKey(Serializable instanceOrClass, String method,
                          Serializable arg1, Serializable arg2,
                          Serializable arg3)
    {
        init(instanceOrClass, method, arg1, arg2, arg3);
    }

    public MethodCacheKey(Serializable[] moreThanThree)
    {
        init(moreThanThree);
    }

    /**
     * Initialize key for method with no arguments.
     *
     * @param instanceOrClass the Object on which the method is invoked.  if
     * the method is static, a String representing the class name is used.
     * @param method the method name
     */
    public void init(Serializable instanceOrClass, String method)
    {
        n = 0;
        this.instanceOrClass = instanceOrClass;
        this.method = method;
        groupKey = instanceOrClass.toString() + method;
    }

    /**
     * Initialize key for method with one argument.
     *
     * @param instanceOrClass the Object on which the method is invoked.  if
     * the method is static, a String representing the class name is used.
     * @param method the method name
     * @param arg1 first method arg, may be null
     */
    public void init(Serializable instanceOrClass, String method,
                     Serializable arg1)
    {
        init(instanceOrClass, method);
        n = 1;
        this.arg1 = arg1;
    }

    /**
     * Initialize key for method with two arguments.
     *
     * @param instanceOrClass the Object on which the method is invoked.  if
     * the method is static, a String representing the class name is used.
     * @param method the method name
     * @param arg1 first method arg, may be null
     * @param arg2 second method arg, may be null
     */
    public void init(Serializable instanceOrClass, String method,
                     Serializable arg1, Serializable arg2)
    {
        init(instanceOrClass, method);
        n = 2;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }


    /**
     * Initialize key for method with two arguments.
     *
     * @param instanceOrClass the Object on which the method is invoked.  if
     * the method is static, a String representing the class name is used.
     * @param method the method name
     * @param arg1 first method arg, may be null
     * @param arg2 second method arg, may be null
     */
    public void init(Serializable instanceOrClass, String method,
                     Serializable arg1, Serializable arg2,
                     Serializable arg3)
    {
        init(instanceOrClass, method);
        n = 3;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    /**
     * Initialize key for method with more than two arguments.
     *
     * @param keys Serializable[] where
     * [0]=>the Object on which the method is invoked
     * if the method is static, a String representing the class name is used.
     * [1]=>the method name
     * [n] where n>1 are the method arguments
     */
    public void init(Serializable[] keys)
    {
        init(keys[0], (String) keys[1]);
        n = keys.length - 2;
        if (n > 0)
        {
            this.arg1 = keys[2];
            if (n > 1)
            {
                this.arg2 = keys[3];
                if (n > 2)
                {
                    this.arg2 = keys[4];
                    if (n > 3)
                    {
                        this.moreThanThree = keys;
                    }
                }
            }
        }
    }

    public String getGroupKey()
    {
        return groupKey;
    }

    public boolean equals(Object obj)
    {
        boolean equal = false;
        if (obj instanceof MethodCacheKey)
        {
            MethodCacheKey sck = (MethodCacheKey) obj;
            equal = (sck.n == n);
            equal &= ObjectUtils.equals(sck.method, method);
            equal &= ObjectUtils.equals(sck.instanceOrClass, instanceOrClass);
            if (equal && n > 0)
            {
                equal &= ObjectUtils.equals(sck.arg1, arg1);
                if (equal && n > 1)
                {
                    equal &= ObjectUtils.equals(sck.arg2, arg2);
                    if (equal && n > 2)
                    {
                        equal &= ObjectUtils.equals(sck.arg3, arg3);
                        if (equal && n > 3)
                        {
                            for (int i = 5; i < n + 2; i++)
                            {
                                equal &= ObjectUtils.equals(
                                        sck.moreThanThree[i], moreThanThree[i]);
                            }
                        }
                    }
                }
            }
        }

        return equal;
    }

    public int hashCode()
    {
        int h = instanceOrClass.hashCode();
        h += method.hashCode();
        if (n > 0)
        {
            h += (arg1 == null ? 0 : arg1.hashCode());
            if (n > 1)
            {
                h += (arg2 == null ? 0 : arg2.hashCode());
                if (n > 2)
                {
                    h += (arg3 == null ? 0 : arg3.hashCode());
                    if (n > 3)
                    {
                        for (int i = 5; i < n + 2; i++)
                        {
                            h += (moreThanThree[i] == null ? 0
                                    : moreThanThree[i].hashCode());
                        }
                    }
                }
            }
        }
        return h;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(50);
        sb.append(instanceOrClass);
        sb.append("::");
        sb.append(method).append('(');
        if (n > 0)
        {
           sb.append(arg1);
           if (n > 1)
           {
               sb.append(", ").append(arg2);
               if (n > 2)
               {
                   sb.append(", ").append(arg3);
                   if (n > 3)
                   {
                       for (int i = 5; i < n + 2; i++)
                       {
                           sb.append(", ").append(moreThanThree[i]);
                       }
                   }
               }
           }
        }
        sb.append(')');
        return sb.toString();
    }

    // ************* PoolableObjectFactory implementation *******************

    public static class Factory
        extends BasePoolableObjectFactory
    {
        /**
         * Creates an instance that can be returned by the pool.
         * @return an instance that can be returned by the pool.
         */
        public Object makeObject()
            throws Exception
        {
            return new MethodCacheKey();
        }

        /**
         * Uninitialize an instance to be returned to the pool.
         * @param obj the instance to be passivated
         */
        public void passivateObject(Object obj)
            throws Exception
        {
            MethodCacheKey key = (MethodCacheKey) obj;
            if (key.instanceOrClass == null && key.method == null)
            {
                throw new TorqueException(
                    "Attempted to return key to pool twice.");
            }
            key.instanceOrClass = null;
            key.method = null;
            key.arg1 = null;
            key.arg2 = null;
            key.arg3 = null;
            key.moreThanThree = null;
            key.groupKey = null;
        }
    }
}
