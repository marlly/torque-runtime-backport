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
import org.apache.jcs.access.GroupCacheAccess;
import org.apache.torque.TorqueException;

/**
 * This class provides a cache for convenient storage of method results
 *
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @version $Id$
 */
public class NoOpMethodResultCache
    extends MethodResultCache
{
    public NoOpMethodResultCache(GroupCacheAccess cache)
        throws TorqueException
    {
        super();
    }

    public void clear()
    {
    }

    protected Object getImpl(MethodCacheKey key)
    {
        return null;
    }


    protected Object putImpl(MethodCacheKey key, Object value)
        throws TorqueException
    {
        return null;
    }

    protected Object removeImpl(MethodCacheKey key)
        throws TorqueException
    {
        return null;
    }


    public Object get(Serializable instanceOrClass, String method)
    {
        return null;
    }

    public Object get(Serializable instanceOrClass, String method,
                      Serializable arg1)
    {
        return null;
    }

    public Object get(Serializable instanceOrClass, String method,
                      Serializable arg1, Serializable arg2)
    {
        return null;
    }

    public Object get(Serializable instanceOrClass, String method,
                      Serializable arg1, Serializable arg2,
                      Serializable arg3)
    {
        return null;
    }

    public Object get(Serializable[] keys)
    {
        return null;
    }

    public void put(Object value, Serializable instanceOrClass,  String method)
    {
    }

    public void put(Object value, Serializable instanceOrClass,
                    String method, Serializable arg1)
    {
    }

    public void put(Object value, Serializable instanceOrClass, String method,
                    Serializable arg1, Serializable arg2)
    {
    }

    public void put(Object value, Serializable instanceOrClass, String method,
                    Serializable arg1, Serializable arg2, Serializable arg3)
    {
    }

    public void put(Object value, Serializable[] keys)
    {
    }


    public void removeAll(Serializable instanceOrClass, String method)
    {
    }


    public Object remove(Serializable instanceOrClass, String method)
    {
        return null;
    }

    public Object remove(Serializable instanceOrClass, String method,
                         Serializable arg1)
    {
        return null;
    }

    public Object remove(Serializable instanceOrClass, String method,
                         Serializable arg1, Serializable arg2)
    {
        return null;
    }

    public Object remove(Serializable instanceOrClass, String method,
                         Serializable arg1, Serializable arg2,
                         Serializable arg3)
    {
        return null;
    }

    public Object remove(Serializable[] keys)
    {
        return null;
    }
}
