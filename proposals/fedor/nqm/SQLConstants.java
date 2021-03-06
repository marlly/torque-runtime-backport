package org.apache.turbine.util.db.nqm;

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

// Java Core Classes

// Turbine Classes
import org.apache.turbine.util.db.adapter.DB;
import org.apache.turbine.util.StringStackBuffer;

/**
 *
 * @author <a href="mailto:fedor.karpelevitch@home.com">Fedor Karpelevitch</a>
 * @version $Id$
 */
public interface SQLConstants
{
    public static final String SELECT = "SELECT ";
    public static final String COMMA = ",";
    public static final String ASTERISK = "*";
    public static final String DOT = ".";
    public static final String ASC = " ASC";
    public static final String DESC = " DESC";
    public static final String WHERE = " WHERE ";
    public static final String FROM  = " FROM ";
    public static final String ORDER_BY  = " ORDER BY ";
}
