package org.apache.torque.manager;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.commons.collections.FastArrayList;
import org.apache.jcs.JCS;
import org.apache.jcs.access.GroupCacheAccess;
import org.apache.jcs.access.exception.CacheException;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.Persistent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains common functionality of a Manager for
 * instantiating OM's.
 *
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @version $Id$
 */
public abstract class AbstractBaseManager
    implements Serializable
{
    /** the log */
    protected static Log log = LogFactory.getLog(AbstractBaseManager.class);

    /** used to cache the om objects. cache is set by the region property */
    protected transient GroupCacheAccess cache;

    /** method results cache */
    protected MethodResultCache mrCache;

    /** the class that the service will instantiate */
    private Class omClass;

    private String className;

    private String region;

    private boolean isNew = true;

    protected Map validFields;
    protected Map listenersMap = new HashMap();

    /**
     * Get the Class instance
     *
     * @return the om class
     */
    protected Class getOMClass()
    {
        return omClass;
    }

    /**
     * Set the Class that will be instantiated by this manager
     *
     * @param omClass the om class
     */
    protected void setOMClass(Class omClass)
    {
        this.omClass = omClass;
    }

    /**
     * Get a fresh instance of an om
     *
     * @return an instance of the om class
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected Persistent getOMInstance()
        throws InstantiationException, IllegalAccessException
    {
        return (Persistent) omClass.newInstance();
    }

    /**
     * Get the classname to instantiate for getInstance()
     * @return value of className.
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * Set the classname to instantiate for getInstance()
     * @param v  Value to assign to className.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void setClassName(String  v)
        throws TorqueException
    {
        this.className = v;

        try
        {
            setOMClass(Class.forName(getClassName()));
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new TorqueException("Could not load " + getClassName());
        }
    }


    /**
     * Return an instance of an om based on the id
     *
     * @param id
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected Persistent getOMInstance(ObjectKey id)
        throws TorqueException
    {
        return getOMInstance(id, true);
    }

    /**
     * Return an instance of an om based on the id
     *
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected Persistent getOMInstance(ObjectKey key, boolean fromCache)
        throws TorqueException
    {
        Persistent om = null;
        if (fromCache)
        {
            om = cacheGet(key);
        }

        if (om == null)
        {
            om = retrieveStoredOM(key);
            if (fromCache)
            {
                putInstanceImpl(om);
            }
        }

        return om;
    }

    protected Persistent cacheGet(Serializable key)
    {
        Persistent om = null;
        if (cache != null)
        {
            synchronized (this)
            {
                om = (Persistent) cache.get(key);
            }
        }
        return om;
    }

    /**
     * 
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected void clearImpl()
        throws TorqueException
    {
        if (cache != null)
        {
            try
            {
                cache.remove();
            }
            catch (CacheException ce)
            {
                throw new TorqueException(
                        "Could not clear cache due to internal JCS error.", ce);
            }
        }
    }

    /**
     * 
     * @param key
     * @return
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected Persistent removeInstanceImpl(Serializable key)
        throws TorqueException
    {
        Persistent oldOm = null;
        if (cache != null)
        {
            try
            {
                synchronized (this)
                {
                    oldOm = (Persistent) cache.get(key);
                    cache.remove(key);
                }
            }
            catch (CacheException ce)
            {
                throw new TorqueException
                    ("Could not remove from cache due to internal JCS error",
                     ce);
            }
        }
        return oldOm;
    }

    /**
     * 
     * @param om
     * @return
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected Persistent putInstanceImpl(Persistent om)
        throws TorqueException
    {
        ObjectKey key = om.getPrimaryKey();
        return putInstanceImpl(key, om);
    }

    /**
     * 
     * @param key
     * @param om
     * @return
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected Persistent putInstanceImpl(Serializable key, Persistent om)
        throws TorqueException
    {
        if (getOMClass() != null && !getOMClass().isInstance(om))
        {
            throw new TorqueException(om + "; class=" + om.getClass().getName()
                + "; id=" + om.getPrimaryKey() + " cannot be cached with "
                + getOMClass().getName() + " objects");
        }

        Persistent oldOm = null;
        if (cache != null)
        {
            try
            {
                synchronized (this)
                {
                    oldOm = (Persistent) cache.get(key);
                    cache.put(key, om);
                }
            }
            catch (CacheException ce)
            {
                throw new TorqueException
                    ("Could not cache due to internal JCS error", ce);
            }
        }
        return oldOm;
    }

    /**
     * 
     * @param id
     * @return
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected abstract Persistent retrieveStoredOM(ObjectKey id)
        throws TorqueException;

    /**
     * Gets a list of om's based on id's.
     *
     * @param ids a <code>ObjectKey[]</code> value
     * @return a <code>List</code> value
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected List getOMs(ObjectKey[] ids)
        throws TorqueException
    {
        return getOMs(Arrays.asList(ids));
    }

    /**
     * Gets a list of om's based on id's.
     *
     * @param ids a <code>List</code> of <code>ObjectKey</code>'s
     * @return a <code>List</code> value
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected List getOMs(List ids)
        throws TorqueException
    {
        return getOMs(ids, true);
    }

    /**
     * Gets a list of om's based on id's.
     *
     * @param ids a <code>List</code> of <code>ObjectKey</code>'s
     * @return a <code>List</code> value
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected List getOMs(List ids, boolean fromCache)
        throws TorqueException
    {
        List oms = null;
        if (ids != null && ids.size() > 0)
        {
            // start a new list where we will replace the id's with om's
            oms = new ArrayList(ids);
            List newIds = new ArrayList(ids.size());
            for (int i = 0; i < ids.size(); i++)
            {
                ObjectKey key = (ObjectKey) ids.get(i);
                Persistent om = null;
                if (fromCache)
                {
                    om = cacheGet(key);
                }
                if (om == null)
                {
                    newIds.add(key);
                }
                else
                {
                    oms.set(i, om);
                }
            }

            if (newIds.size() > 0)
            {
                List newOms = retrieveStoredOMs(newIds);
                for (int i = 0; i < oms.size(); i++)
                {
                    if (oms.get(i) instanceof ObjectKey)
                    {
                        for (int j = newOms.size() - 1; j >= 0; j--)
                        {
                            Persistent om = (Persistent) newOms.get(j);
                            if (om.getPrimaryKey().equals(oms.get(i)))
                            {
                                // replace the id with the om and add the om
                                // to the cache
                                oms.set(i, om);
                                newOms.remove(j);
                                if (fromCache)
                                {
                                    putInstanceImpl(om);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return oms;
    }

    /**
     * 
     * @param ids
     * @return
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    protected abstract List retrieveStoredOMs(List ids)
        throws TorqueException;

    /**
     * Get the value of region.
     * 
     * @return value of region.
     */
    public String getRegion()
    {
        return region;
    }

    /**
     * Set the value of region.
     *
     * @param v  Value to assign to region.
     * @throws TorqueException Any exceptions caught during processing will be
     *         rethrown wrapped into a TorqueException.
     */
    public void setRegion(String v)
        throws TorqueException
    {
        this.region = v;
        try
        {
            if (Torque.getConfiguration().getBoolean(Torque.CACHE_KEY))
            {
                cache = JCS.getInstance(getRegion());
                mrCache = new MethodResultCache(cache);
            }
            else
            {
                mrCache = new NoOpMethodResultCache(cache);
            }
        }
        catch (Exception e)
        {
            throw new TorqueException("Cache could not be initialized", e);
        }
        if (cache == null)
        {
            log.info("Cache could not be initialized for region: " + v);
        }
    }

    /**
     * @return The cache instance.
     */
    public MethodResultCache getMethodResultCache()
    {
        if (isNew)
        {
            synchronized (this)
            {
                if (isNew)
                {
                    registerAsListener();
                    isNew = false;
                }
            }
        }
        return mrCache;
    }

    /**
     * NoOp version.  Managers should override this method to notify other
     * managers that they are interested in CacheEvents.
     */
    protected void registerAsListener()
    {
    }

    /**
     * 
     * @param listener A new listener for cache events.
     */
    public void addCacheListenerImpl(CacheListener listener)
    {
        List keys = listener.getInterestedFields();
        Iterator i = keys.iterator();
        while (i.hasNext())
        {
            String key = (String) i.next();
            // Peer.column names are the fields
            if (validFields != null && validFields.containsKey(key))
            {
                List listeners = (List) listenersMap.get(key);
                if (listeners == null)
                {
                    listeners = createSubsetList(key);
                }

                boolean isNew = true;
                Iterator j = listeners.iterator();
                while (j.hasNext())
                {
                    Object listener2 =
                        ((WeakReference) j.next()).get();
                    if (listener2 == null)
                    {
                        // do a little cleanup while checking for dupes
                        // not thread-safe, not likely to be many nulls
                        // but should revisit
                        //j.remove();
                    }
                    else if (listener2 == listener)
                    {
                        isNew = false;
                        break;
                    }
                }
                if (isNew)
                {
                    listeners.add(new WeakReference(listener));
                }
            }
        }
    }

    /**
     * 
     * @param key
     * @return A subset of the list identified by <code>key</code>.
     */
    private synchronized List createSubsetList(String key)
    {
        FastArrayList list = null;
        if (listenersMap.containsKey(key))
        {
            list = (FastArrayList) listenersMap.get(key);
        }
        else
        {
            list = new FastArrayList();
            list.setFast(true);
            listenersMap.put(key, list);
        }
        return list;
    }

    /**
     * 
     * @param listeners
     * @param oldOm
     * @param om
     */
    protected void notifyListeners(List listeners,
                                   Persistent oldOm, Persistent om)
    {
        if (listeners != null)
        {
            synchronized (listeners)
            {
                Iterator i = listeners.iterator();
                while (i.hasNext())
                {
                    CacheListener listener = (CacheListener)
                        ((WeakReference) i.next()).get();
                    if (listener == null)
                    {
                        // remove reference as its object was cleared
                        i.remove();
                    }
                    else
                    {
                        if (oldOm == null)
                        {
                            // object was added
                            listener.addedObject(om);
                        }
                        else
                        {
                            // object was refreshed
                            listener.refreshedObject(om);
                        }
                    }
                }
            }
        }
    }


    /**
     * helper methods for the Serializable interface
     * 
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
    }

    /**
     * Helper methods for the <code>Serializable</code> interface.
     * 
     * @param in The stream to read a <code>Serializable</code> from.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        // initialize the cache
        try
        {
            if (region != null)
            {
                setRegion(region);
            }
        }
        catch (Exception e)
        {
            log.error("Cache could not be initialized for region '"
                      + region + "' after deserialization");
        }
    }
}
