package org.apache.torque.map;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * InheritanceMap is used to model OM inheritance classes.
 *
 * @author <a href="mailto:greg.monroe@dukece.com">Greg Monroe</a>
 * @version $Id$
 */
public class InheritanceMap
{
    /**
     * The value in the related column that is associated with
     * this information.
     */
    private String key;

    /**
     * The name of the class which impliments this inheritance mode.
     */
    private String className;

    /**
     * The name of class which class name extends.
     * Retrieved via getExtends().
     */
    private String ancestor;

    /** The column this info is related to. */
    private ColumnMap column;

    /**
     * Create an inheritance map object.
     *
     * @param column The column this inheritance map belongs to.
     * @param key Key to determine which subclass applies
     * @param className package.Name of sub class to use for record.
     * @param ancestor package.Name of class that className extends.
     */
    public InheritanceMap(ColumnMap column, String key, String className,
            String ancestor)
    {
        setColumn(column);
        setKey(key);
        setClassName(className);
        setExtends(ancestor);
    }

    /**
     * Returns the ancestor class for the class described by this
     * InheritanceMap.
     *
     * @return the ancestor class for the class described by this
     *         InheritanceMap.
     */
    public String getExtends()
    {
        return ancestor;
    }

    /**
     * Sets the ancestor class for the class described by this InheritanceMap.
     *
     * @param ancestor The ancestor for the class described by this
     *        InheritanceMap.
     */
    public void setExtends(String ancestor)
    {
        this.ancestor = ancestor;
    }

    /**
     * Returns the class name for this InheritanceMap.
     *
     * @return The class name for this InheritanceMap.
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * Sets the class name for this InheritanceMap.
     *
     * @param className The className for this InheritanceMap.
     */
    public void setClassName(String className)
    {
        this.className = className;
    }

    /**
     * Returns the column this inheritance map belongs to.
     *
     * @return the column this inheritance map belongs to.
     */
    public ColumnMap getColumn()
    {
        return column;
    }

    /**
     * Sets the column this inheritance map belongs to.
     *
     * @param column the column this inheritance map belongs to.
     */
    public void setColumn(ColumnMap column)
    {
        this.column = column;
    }

    /**
     * Returns the key by which this inheritanceMap is activated.
     *
     * @return The key by which this inheritanceMap is activated.
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Sets the key by which this inheritanceMap is activated.
     *
     * @param key The key by which this inheritanceMap is activated.
     */
    public void setKey(String key)
    {
        this.key = key;
    }
}
