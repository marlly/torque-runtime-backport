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

import java.sql.Connection;

/**
 * This interface defines methods related to saving an object
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @author <a href="mailto:fedor@apache.org">Fedor K.</a>
 * @version $Id$
 */
public interface Persistent
{
    /**
     * getter for the object primaryKey.
     *
     * @return the object primaryKey as an Object
     */
    ObjectKey getPrimaryKey();

    /**
     * Sets the PrimaryKey for the object.
     *
     * @param primaryKey The new PrimaryKey for the object.
     * @throws Exception This method might throw an exception
     */
    void setPrimaryKey(ObjectKey primaryKey) throws Exception;

    /**
     * Sets the PrimaryKey for the object.
     *
     * @param primaryKey the String should be of the form produced by
     *        ObjectKey.toString().
     * @throws Exception This method might throw an exception
     */
    void setPrimaryKey(String primaryKey) throws Exception;

    /**
     * Returns whether the object has been modified, since it was
     * last retrieved from storage.
     *
     * @return True if the object has been modified.
     */
    boolean isModified();

    /**
     * Returns whether the object has ever been saved.  This will
     * be false, if the object was retrieved from storage or was created
     * and then saved.
     *
     * @return true, if the object has never been persisted.
     */
    boolean isNew();

    /**
     * Setter for the isNew attribute.  This method will be called
     * by Torque-generated children and Peers.
     *
     * @param b the state of the object.
     */
    void setNew(boolean b);

    /**
     * Sets the modified state for the object.
     *
     * @param m The new modified state for the object.
     */
    void setModified(boolean m);

    /**
     * Saves the object.
     *
     * @throws Exception This method might throw an exception
     */
    void save() throws Exception;

    /**
     * Stores the object in the database.  If the object is new,
     * it inserts it; otherwise an update is performed.
     *
     * @param dbName the name of the database
     * @throws Exception This method might throw an exception
     */
    void save(String dbName) throws Exception;

    /**
     * Stores the object in the database.  If the object is new,
     * it inserts it; otherwise an update is performed.  This method
     * is meant to be used as part of a transaction, otherwise use
     * the save() method and the connection details will be handled
     * internally
     *
     * @param con the Connection used to store the object
     * @throws Exception This method might throw an exception
     */
    void save(Connection con) throws Exception;
}
