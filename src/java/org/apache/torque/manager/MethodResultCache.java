package org.apache.torque.manager;

/* ================================================================
 * Copyright (c) 2001 Collab.Net.  All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if
 * any, must include the following acknowlegement: "This product includes
 * software developed by Collab.Net <http://www.Collab.Net/>."
 * Alternately, this acknowlegement may appear in the software itself, if
 * and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The hosted project names must not be used to endorse or promote
 * products derived from this software without prior written
 * permission. For written permission, please contact info@collab.net.
 * 
 * 5. Products derived from this software may not use the "Tigris" or 
 * "Scarab" names nor may "Tigris" or "Scarab" appear in their names without 
 * prior written permission of Collab.Net.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL COLLAB.NET OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Collab.Net.
 */ 

import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Iterator;
import java.io.Serializable;
import org.apache.log4j.Category;
import org.apache.stratum.jcs.access.GroupCacheAccess;
import org.apache.stratum.jcs.access.exception.CacheException;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import org.apache.torque.TorqueException;

/**
 * This class provides a cache for convenient storage of method results
 *
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @version $Id$
 */
public class MethodResultCache 
{
    private static final Category log = 
        Category.getInstance("org.apache.torque");

    private static final String keyClassName =
        "org.apache.torque.manager.MethodCacheKey";
    private ObjectPool pool;
    private Map keys;
    private GroupCacheAccess jcsCache;
    private boolean lockCache;
    private int inGet;
    private Map groups;

    public MethodResultCache(GroupCacheAccess cache)
        throws TorqueException
    {
        keys = new WeakHashMap();            
        this.jcsCache = cache;            
        groups = new HashMap();

        try
        {
            PoolableObjectFactory factory = 
                    new MethodCacheKey.Factory();
            pool = new StackObjectPool(factory, 10000);
        }
        catch (Exception e)
        {
            throw new TorqueException(e);
        }
    }

    public void clear()
    {
        if (jcsCache != null)
        {
            try
            {
                jcsCache.remove();

                // clear out local map of keys, return to pool
                Iterator i = keys.keySet().iterator();
                while (i.hasNext()) 
                {
                    try
                    {
                        pool.returnObject(i.next());
                    }
                    catch (Exception e)
                    {
                        log.warn(
                            "Nonfatal error. Could not return key to pool", e);
                    }
                }
                keys.clear();
            }
            catch (CacheException ce)
            {
                log.error(new TorqueException(
                    "Could not clear cache due to internal JCS error.", ce));
            }
        }
    }

    protected Object getImpl(MethodCacheKey key)
    {
        Object result = null;
        if (jcsCache != null)
        {
            if (lockCache)
            {
                synchronized (this)
                {
                    result = jcsCache.getFromGroup(key, key.getGroupKey());
                }
            }
            else
            {
                inGet++;
                result = jcsCache.getFromGroup(key, key.getGroupKey());
                inGet--;
            }
        }

        if (result != null) 
        {
            log.debug("MethodResultCache saved expensive operation: " + key);
        }
        
        return result;
    }


    protected Object putImpl(MethodCacheKey key, Object value)
        throws TorqueException
    {
        //register the group, if this is the first occurrence
        String group = key.getGroupKey();
        if (!groups.containsKey(group)) 
        {
            synchronized (jcsCache)
            {
                if (!groups.containsKey(group)) 
                {
                    try
                    {
                        jcsCache.defineGroup(group);
                    }
                    catch (CacheException ce)
                    {
                        throw new TorqueException(ce);
                    }
                    groups.put(group, null);
                }                
            }
        }

        Object old = null;
        if (jcsCache != null)
        {
            synchronized (this)
            {
                lockCache = true;
                try
                {
                    old = jcsCache.getFromGroup(key, group);
                    while (inGet > 0) 
                    {
                        Thread.yield();
                    }                    
                    jcsCache.putInGroup(key, group, value);
                }
                catch (CacheException ce)
                {
                    lockCache = false;
                    throw new TorqueException(
                        "Could not cache due to internal JCS error.", ce);
                }
                finally
                {
                    lockCache = false;
                }
            }
        }
        return old;
    }

    protected Object removeImpl(MethodCacheKey key)
        throws TorqueException
    {
        Object old = null;
        if (jcsCache != null)
        {
            synchronized (this)
            {
                lockCache = true;
                try
                {
                    old = jcsCache.getFromGroup(key, key.getGroupKey());
                    while (inGet > 0) 
                    {
                        Thread.yield();
                    }
                    jcsCache.remove(key, key.getGroupKey());
                    try
                    {
                        pool.returnObject(key);
                    }
                    catch (Exception e)
                    {
                        log.warn(
                            "Nonfatal error. Could not return key to pool", e);
                    }
                }
                // jcs does not throw an exception here, might remove this
                catch (Exception ce) 
                {
                    lockCache = false;
                    throw new TorqueException(
                        "Could not cache due to internal JCS error.", ce);
                }
                finally
                {
                    lockCache = false;
                }
            }
        }
        return old;
    }


    public Object get(Serializable instanceOrClass, String method)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method);
                result = getImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }            
        }
        return result;
    }

    public Object get(Serializable instanceOrClass, String method,
                      Serializable arg1)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method, arg1);
                result = getImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public Object get(Serializable instanceOrClass, String method,
                      Serializable arg1, Serializable arg2)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method, arg1, arg2);
                result = getImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public Object get(Serializable instanceOrClass, String method,
                      Serializable arg1, Serializable arg2,
                      Serializable arg3)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method, arg1, arg2, arg3);
                result = getImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public Object get(Serializable[] keys)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(keys);
                result = getImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public void put(Object value, Serializable instanceOrClass,  String method)
    {
        try
        {
            MethodCacheKey key =  
                (MethodCacheKey)pool.borrowObject();
            key.init(instanceOrClass, method);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error(e);
        }
    }

    public void put(Object value, Serializable instanceOrClass, 
                    String method, Serializable arg1)
    {
        try
        {
            MethodCacheKey key =  
                (MethodCacheKey)pool.borrowObject();
            key.init(instanceOrClass, method, arg1);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error(e);
        }
    }

    public void put(Object value, Serializable instanceOrClass, String method,
                    Serializable arg1, Serializable arg2)
    {
        try
        {
            MethodCacheKey key =  
                (MethodCacheKey)pool.borrowObject();
            key.init(instanceOrClass, method, arg1, arg2);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error(e);
        }
    }

    public void put(Object value, Serializable instanceOrClass, String method,
                    Serializable arg1, Serializable arg2, Serializable arg3)
    {
        try
        {
            MethodCacheKey key =  
                (MethodCacheKey)pool.borrowObject();
            key.init(instanceOrClass, method, arg1, arg2, arg3);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error(e);
        }
    }

    public void put(Object value, Serializable[] keys)
    {
        try
        {
            MethodCacheKey key =  
                (MethodCacheKey)pool.borrowObject();
            key.init(keys);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error(e);
        }
    }


    public void removeAll(Serializable instanceOrClass, String method)
    {
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method);
                jcsCache.invalidateGroup(key.getGroupKey());
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }            
        }
    }


    public Object remove(Serializable instanceOrClass, String method)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }            
        }
        return result;
    }

    public Object remove(Serializable instanceOrClass, String method,
                         Serializable arg1)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method, arg1);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public Object remove(Serializable instanceOrClass, String method,
                         Serializable arg1, Serializable arg2)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method, arg1, arg2);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public Object remove(Serializable instanceOrClass, String method,
                         Serializable arg1, Serializable arg2,
                         Serializable arg3)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(instanceOrClass, method, arg1, arg2, arg3);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }

    public Object remove(Serializable[] keys)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.borrowObject();
                key.init(keys);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error.  Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }
}
