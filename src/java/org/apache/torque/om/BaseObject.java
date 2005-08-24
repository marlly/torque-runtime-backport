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

import java.io.Serializable;
import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.torque.TorqueException;

/**
 * This class contains attributes and methods that are used by all
 * business objects within the system.
 *
 * @author <a href="mailto:frank.kim@clearink.com">Frank Y. Kim</a>
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public abstract class BaseObject implements Persistent, Serializable
{
    /** The constant denoting an unset numeric database identifier. */
    public static final int NEW_ID = -1;

    /**
     * Shared portion of the error message thrown for methods which
     * are not implemented.
     */
    private static final String NOT_IMPLEMENTED
            = "Not implemented: Method must be overridden if called";

    /** attribute to determine if this object has previously been saved. */
    private boolean isNew = true;

    /** The unique id for the object which can be used for persistence. */
    private ObjectKey primaryKey = null;

    /**
     * A flag that indicates an object has been modified since it was
     * last retrieved from the persistence mechanism.  This flag is
     * used to determine if this object should be saved to the
     * database.  We initialize it to true to force new objects to be
     * saved.
     */
    private boolean modified = true;

    /** Cache the log to avoid looking it up every time its needed. */
    private transient Log log = null;

    /**
     * getter for the object primaryKey.
     *
     * @return the object primaryKey as an Object
     */
    public ObjectKey getPrimaryKey()
    {
        return primaryKey;
    }

    /**
     * Returns whether the object has been modified.
     *
     * @return True if the object has been modified.
     */
    public boolean isModified()
    {
        return modified;
    }

    /**
     * Returns whether the object has ever been saved.  This will
     * be false, if the object was retrieved from storage or was created
     * and then saved.
     *
     * @return true, if the object has never been persisted.
     */
    public boolean isNew()
    {
        return isNew;
    }

    /**
     * Setter for the isNew attribute.  This method will be called
     * by Torque-generated children and Peers.
     *
     * @param b the state of the object.
     */
    public void setNew(boolean b)
    {
        this.isNew = b;
    }

    /**
     * Sets the PrimaryKey for the object.
     *
     * @param primaryKey The new PrimaryKey for the object.
     * @exception TorqueException This method will not throw any exceptions
     * but this allows for children to override the method more easily
     */
    public void setPrimaryKey(String primaryKey) throws TorqueException
    {
        this.primaryKey = new StringKey(primaryKey);
    }

    /**
     * Sets the PrimaryKey for the object as an Object.
     *
     * @param primaryKey The new PrimaryKey for the object.
     * @exception TorqueException This method will not throw any exceptions
     * but this allows for children to override the method more easily
     */
    public void setPrimaryKey(SimpleKey[] primaryKey) throws TorqueException
    {
        this.primaryKey = new ComboKey(primaryKey);
    }

    /**
     * Sets the PrimaryKey for the object as an Object.
     *
     * @param primaryKey The new PrimaryKey for the object.
     * @exception TorqueException This method will not throw any exceptions
     * but this allows for children to override the method more easily
     */
    public void setPrimaryKey(ObjectKey primaryKey) throws TorqueException
    {
        this.primaryKey = primaryKey;
    }

    /**
     * Sets the modified state for the object.
     *
     * @param m The new modified state for the object.
     */
    public void setModified(boolean m)
    {
        modified = m;
    }

    /**
     * Sets the modified state for the object to be false.
     */
    public void resetModified()
    {
        modified = false;
    }

    /**
     * Retrieves a field from the object by name. Must be overridden if called.
     * BaseObject's implementation will throw an Error.
     *
     * @param field The name of the field to retrieve.
     * @return The retrieved field value
     *
     */
    public Object getByName(String field)
    {
        throw new Error("BaseObject.getByName: " + NOT_IMPLEMENTED);
    }

    /**
     * Retrieves a field from the object by name passed in
     * as a String.  Must be overridden if called.
     * BaseObject's implementation will throw an Error.
     *
     * @param name field name
     * @return value of the field
     */
    public Object getByPeerName(String name)
    {
        throw new Error("BaseObject.getByPeerName: " + NOT_IMPLEMENTED);
    }

    /**
     * Retrieves a field from the object by position as specified
     * in a database schema for example.  Must be overridden if called.
     * BaseObject's implementation will throw an Error.
     *
     * @param pos field position
     * @return value of the field
     */
    public Object getByPosition(int pos)
    {
        throw new Error("BaseObject.getByPosition: " + NOT_IMPLEMENTED);
    }

    /**
     * Compares this with another <code>BaseObject</code> instance.  If
     * <code>obj</code> is an instance of <code>BaseObject</code>, delegates to
     * <code>equals(BaseObject)</code>.  Otherwise, returns <code>false</code>.
     *
     * @param obj The object to compare to.
     * @return    Whether equal to the object specified.
     */
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof BaseObject)
        {
            return equals((BaseObject) obj);
        }
        else
        {
            return false;
        }
    }

    /**
     * Compares the primary key of this instance with the key of another.
     *
     * @param bo The object to compare to.
     * @return   Whether the primary keys are equal.
     */
    public boolean equals(BaseObject bo)
    {
        if (bo == null)
        {
            return false;
        }
        if (this == bo)
        {
            return true;
        }
        else if (getPrimaryKey() == null || bo.getPrimaryKey() == null)
        {
            return false;
        }
        else
        {
            return getPrimaryKey().equals(bo.getPrimaryKey());
        }
    }

    /**
     * If the primary key is not <code>null</code>, return the hashcode of the
     * primary key.  Otherwise calls <code>Object.hashCode()</code>.
     *
     * @return an <code>int</code> value
     */
    public int hashCode()
    {
        ObjectKey ok = getPrimaryKey();
        if (ok == null)
        {
            return super.hashCode();
        }

        return ok.hashCode();
    }

    /**
     * gets a commons-logging Log based on class name.
     *
     * @return a <code>Log</code> to write log to.
     */
    protected Log getLog()
    {
        if (log == null)
        {
            log = LogFactory.getLog(getClass().getName());
        }
        return log;
    }

    /**
     * @see org.apache.torque.om.Persistent#save()
     */
    public abstract void save() throws Exception;

    /**
     * @see org.apache.torque.om.Persistent#save(String)
     */
    public abstract void save(String dbName) throws Exception;

    /**
     * @see org.apache.torque.om.Persistent#save(Connection)
     */
    public abstract void save(Connection con) throws Exception;
}
