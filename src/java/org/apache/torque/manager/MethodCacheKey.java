package org.apache.torque.manager;


/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Turbine" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.Serializable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.torque.TorqueException;

public class MethodCacheKey implements Serializable
{
    //private static final Category log =
    //    Category.getInstance("org.apache.torque");

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
     * @param Serializable[] where
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
                            h+= (moreThanThree[i] == null ?
                                 0 : moreThanThree[i].hashCode());
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
            MethodCacheKey key = (MethodCacheKey)obj;
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
