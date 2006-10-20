package org.apache.torque.manager;

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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import org.apache.jcs.access.GroupCacheAccess;
import org.apache.jcs.access.exception.CacheException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.TorqueException;

/**
 * This class provides a cache for convenient storage of method
 * results.
 *
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @version $Id$
 */
public class MethodResultCache
{
    private ObjectPool pool;
    private GroupCacheAccess jcsCache;
    private Map groups;

    /** Logging */
    private static Log log = LogFactory.getLog(MethodResultCache.class);

    public MethodResultCache(GroupCacheAccess cache)
        throws TorqueException
    {
        this.jcsCache = cache;
        groups = new HashMap();
        pool = new StackObjectPool(new MethodCacheKey.Factory(), 10000);
    }

    /**
     * Allows subclasses to have ctors that do not require a cache.
     * This is used by NullMethodResultCache which has no-op versions
     * of all methods.
     */
    protected MethodResultCache()
    {
    }

    public void clear()
    {
        if (jcsCache != null)
        {
            try
            {
                jcsCache.remove();
                groups.clear();
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
            synchronized (this)
            {
                result = jcsCache.getFromGroup(key, key.getGroupKey());
            }
        }

        if (result != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("MethodResultCache saved expensive operation: " + key);
            }
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
            groups.put(group, null);
        }

        Object old = null;
        if (jcsCache != null)
        {
            try
            {
                synchronized (this)
                {
                    old = jcsCache.getFromGroup(key, group);
                    jcsCache.putInGroup(key, group, value);
                }
            }
            catch (CacheException ce)
            {
                throw new TorqueException
                    ("Could not cache due to internal JCS error", ce);
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
                old = jcsCache.getFromGroup(key, key.getGroupKey());
                jcsCache.remove(key, key.getGroupKey());
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("", e);
            }
        }
        return result;
    }

    public void put(Object value, Serializable instanceOrClass,  String method)
    {
        try
        {
            MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
            key.init(instanceOrClass, method);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }

    public void put(Object value, Serializable instanceOrClass,
                    String method, Serializable arg1)
    {
        try
        {
            MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
            key.init(instanceOrClass, method, arg1);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }

    public void put(Object value, Serializable instanceOrClass, String method,
                    Serializable arg1, Serializable arg2)
    {
        try
        {
            MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
            key.init(instanceOrClass, method, arg1, arg2);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }

    public void put(Object value, Serializable instanceOrClass, String method,
                    Serializable arg1, Serializable arg2, Serializable arg3)
    {
        try
        {
            MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
            key.init(instanceOrClass, method, arg1, arg2, arg3);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }

    public void put(Object value, Serializable[] keys)
    {
        try
        {
            MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
            key.init(keys);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error("", e);
        }
    }


    public void removeAll(Serializable instanceOrClass, String method)
    {
        if (jcsCache != null)
        {
            try
            {
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
                key.init(instanceOrClass, method);
                String groupName = key.getGroupKey();
                jcsCache.invalidateGroup(groupName);
                groups.remove(groupName);
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
                log.error("", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("Error removing element", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
                key.init(instanceOrClass, method, arg1, arg2);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error: Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error("Error removing element from cache", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
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
                log.error("Error removing element from cache", e);
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
                MethodCacheKey key = (MethodCacheKey) pool.borrowObject();
                key.init(keys);
                result = removeImpl(key);
                try
                {
                    pool.returnObject(key);
                }
                catch (Exception e)
                {
                    log.warn(
                        "Nonfatal error: Could not return key to pool", e);
                }
            }
            catch (Exception e)
            {
                log.error("Error removing element from cache", e);
            }
        }
        return result;
    }
}
