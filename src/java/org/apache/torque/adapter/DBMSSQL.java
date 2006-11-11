package org.apache.torque.adapter;

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
 * This is used to connect to a MSSQL database.  For now, this class
 * simply extends the adaptor for Sybase.  You can use one of several
 * commercial JDBC drivers; the one I use is:
 *
 *   i-net SPRINTA(tm) 2000 Driver Version 3.03 for MS SQL Server
 *   http://www.inetsoftware.de/
 *
 * @author <a href="mailto:gonzalo.diethelm@sonda.com">Gonzalo Diethelm</a>
 * @version $Id$
 */
public class DBMSSQL extends DBSybase
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2924485528975497044L;

    /**
     * Empty constructor.
     */
    protected DBMSSQL()
    {
    }

    /**
     * Whether an escape clause in like should be used.
     * Example : select * from AUTHOR where AUTHOR.NAME like '\_%' ESCAPE '\';
     *
     * TODO: check the following:
     * MS-SQL does not need this, so this implementation always returns
     * <code>false</code>.
     *
     * @return whether the escape clause should be appended or not.
     */
    public boolean useEscapeClauseForLike()
    {
        return false;
    }
}
