package org.apache.torque.util;

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

import java.util.ArrayList;

/**
 * List with unique entries. UniqueList does not allow null nor duplicates.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @version $Id$
 */
public class UniqueList extends ArrayList
{
    /**
     * Adds an Object to the list.
     *
     * @param o the Object to add
     * @return true if the Object is added
     */
    public boolean add(Object o)
    {
        if (o != null && !contains(o))
        {
            return super.add(o);
        }
        return false;
    }
}
