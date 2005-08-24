package org.apache.torque.map;

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

/**
 * MapBuilders are wrappers around DatabaseMaps.  You use a MapBuilder
 * to populate a DatabaseMap.  You should implement this interface to create
 * your own MapBuilders.  The MapBuilder interface exists to support ease of
 * casting.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id$
 */
public interface MapBuilder
{
    /**
     * Build up the database mapping.
     *
     * @exception Exception Couldn't build mapping.
     */
    void doBuild()
        throws Exception;

    /**
     * Tells us if the database mapping is built so that we can avoid
     * re-building it repeatedly.
     *
     * @return Whether the DatabaseMap is built.
     */
    boolean isBuilt();

    /**
     * Gets the database mapping this map builder built.
     *
     * @return A DatabaseMap.
     */
    DatabaseMap getDatabaseMap();
}
