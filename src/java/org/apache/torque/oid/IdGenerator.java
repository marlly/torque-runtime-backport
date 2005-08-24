package org.apache.torque.oid;

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

import java.sql.Connection;
import java.math.BigDecimal;

/**
 * Interface to be implemented by id generators.  It is possible
 * that some implementations might not require all the arguments,
 * for example MySQL will not require a keyInfo Object, while the
 * IDBroker implementation does not require a Connection as
 * it only rarely needs one and retrieves a connection from the
 * Connection pool service only when needed.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public interface IdGenerator
{
    /**
     * Returns an id as a primitive int.  If you use numeric
     * identifiers, it's suggested that {@link
     * #getIdAsLong(Connection, Object)} be used instead (due to the
     * limitted range of this method).
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return An int with the value for the id.
     * @exception Exception Database error.
     */
    int getIdAsInt(Connection connection, Object keyInfo)
        throws Exception;

    /**
     * Returns an id as a primitive long.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A long with the value for the id.
     * @exception Exception Database error.
     */
    long getIdAsLong(Connection connection, Object keyInfo)
        throws Exception;

    /**
     * Returns an id as a BigDecimal.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A BigDecimal id.
     * @exception Exception Database error.
     */
    BigDecimal getIdAsBigDecimal(Connection connection, Object keyInfo)
        throws Exception;

    /**
     * Returns an id as a String.
     *
     * @param connection A Connection.
     * @param keyInfo an Object that contains additional info.
     * @return A String id
     * @exception Exception Database error.
     */
    String getIdAsString(Connection connection, Object keyInfo)
        throws Exception;

    /**
     * A flag to determine the timing of the id generation
     *
     * @return a <code>boolean</code> value
     */
    boolean isPriorToInsert();

    /**
     * A flag to determine the timing of the id generation
     *
     * @return Whether id is availble post-<code>insert</code>.
     */
    boolean isPostInsert();

    /**
     * A flag to determine whether a Connection is required to
     * generate an id.
     *
     * @return a <code>boolean</code> value
     */
    boolean isConnectionRequired();
}
