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
//import org.apache.stratum.configuration.Configuration;
import org.apache.stratum.pool.DefaultPoolManager;
import org.apache.stratum.jcs.access.behavior.ICacheAccess;
import org.apache.stratum.jcs.access.exception.CacheException;

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
    private DefaultPoolManager pool;
    private Map keys;
    private ICacheAccess jcsCache;
    private Class keyClass;
    private boolean lockCache;
    private int inGet;

    /*
    public MethodResultCache()
    {
        keyClass = Class
            .forName(keyClassName);
    }
    */

    public MethodResultCache(ICacheAccess cache)
        throws ClassNotFoundException
    {
        keys = new WeakHashMap();            
        keyClass = Class.forName(keyClassName);
        this.jcsCache = cache;            
        pool =  new DefaultPoolManager();
        pool.setCapacity(keyClassName, 10000);
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
                    pool.putInstance(i.next());
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
                    result = jcsCache.get(key);
                }
            }
            else
            {
                inGet++;
                result = jcsCache.get(key);
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
        Object old = null;
        if (jcsCache != null)
        {
            synchronized (this)
            {
                lockCache = true;
                try
                {
                    old = jcsCache.get(key);
                    while (inGet > 0) 
                    {
                        Thread.yield();
                    }                    
                    jcsCache.put(key, value);
                    keys.put(key, key);
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
                    old = jcsCache.get(key);
                    while (inGet > 0) 
                    {
                        Thread.yield();
                    }
                    jcsCache.remove(key);
                    pool.putInstance(keys.remove(key));
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


    public Object get(Serializable instanceOrClass, String method)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method);
                result = getImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method, arg1);
                result = getImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method, arg1, arg2);
                result = getImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method, arg1, arg2, arg3);
                result = getImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(keys);
                result = getImpl(key);
                pool.putInstance(key);
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
                (MethodCacheKey)pool.getInstance(keyClass);
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
                (MethodCacheKey)pool.getInstance(keyClass);
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
                (MethodCacheKey)pool.getInstance(keyClass);
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
                (MethodCacheKey)pool.getInstance(keyClass);
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
                (MethodCacheKey)pool.getInstance(keyClass);
            key.init(keys);
            putImpl(key, value);
        }
        catch (Exception e)
        {
            log.error(e);
        }
    }


    public int removeAll(Serializable instanceOrClass, String method)
    {
        int result = -1;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method);
                key.setLenient(true);
                Object obj = null;
                do 
                {
                    obj = removeImpl(key);
                    result++;
                }
                while (obj != null);
                pool.putInstance(key);
            }
            catch (Exception e)
            {
                log.error(e);
            }            
        }
        return result;
    }


    public Object remove(Serializable instanceOrClass, String method)
    {
        Object result = null;
        if (jcsCache != null) 
        {
            try
            {
                MethodCacheKey key = 
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method);
                result = removeImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method, arg1);
                result = removeImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method, arg1, arg2);
                result = removeImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(instanceOrClass, method, arg1, arg2, arg3);
                result = removeImpl(key);
                pool.putInstance(key);
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
                    (MethodCacheKey)pool.getInstance(keyClass);
                key.init(keys);
                result = removeImpl(key);
                pool.putInstance(key);
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        return result;
    }
}
