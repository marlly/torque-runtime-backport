package org.apache.torque.util;

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

/**
 * This is a utility class which eases counting of Datasets
 *
 * @author <a href="mailto:Martin.Goulet@sungard.com">Martin Goulet</a>
 * @author <a href="mailto:eric.lambert@sungard.com">Eric Lambert</a>
 * @author <a href="mailto:sebastien.paquette@sungard.com">Sebastien Paquette</a>
 * @author <a href="mailto:fischer@seitenbau.de">Thomas Fischer</a>
 * @version $Id$
 */
import java.sql.Connection;
import java.util.List;

import org.apache.torque.TorqueException;

import com.workingdogs.village.DataSetException;
import com.workingdogs.village.Record;

public class CountHelper 
{  
    /**
     * The COUNT function returns the number of rows in a query. 
     * Does not use a connection, hardcode the column to "*" and 
     * set the distinct qualifier to false.
     * Only use this function if you have added additional constraints to
     * the criteria, otherwise Torque does not know which table it should
     * count the datasets in.
     * 
     * @param c Criteria to get the count for.
     * @return number of rows matching the query provided
     * @throws TorqueException if the query could not be executed
     */
    public int count( Criteria c ) throws TorqueException
    {
        return count( c, null, "*" );
    }

    /**
     * The COUNT function returns the number of rows in a query. 
     * Hard code the distinct parameter to false and set the column to "*".
     * Only use this function if you have added additional constraints to
     * the criteria, otherwise Torque does not know which table it should
     * count the datasets in.
     * 
     * @param c Criteria to get the count for.
     * @param conn Connection to use
     * @return number of rows matching the query provided
     * @throws TorqueException if the query could not be executed
     */    
    public int count( Criteria c, Connection conn ) throws TorqueException
    {
        return count( c, conn, "*" );       
    }

    /**
     * Returns the number of rows in a query.
     * 
     * @param c Criteria to get the count for.
     * @param columnName Name of database Column which is counted. Preferably,
     *        use the primary key here.
     * @return number of rows matching the query provided
     * @throws TorqueException if the query could not be executed
     */
    public int count( Criteria c, String columnName ) 
        throws TorqueException
    {
        return count( c, null, columnName );
    }

    /**
     * Returns the number of rows in a query. 
     * 
     * @param c Criteria to get the count for.
     * @param conn Connection to use
     * @param columnName Name of database Column which is counted. Preferably,
     *        use the primary key here.
     * @return number of rows matching the query provided
     * @throws TorqueException if the query could not be executed
     */
    public int count( Criteria c, Connection conn, String columnName ) 
        throws TorqueException
    {        
        /* Clear the select columns. */
        c.getSelectColumns().clear();
        c.getOrderByColumns().clear();
        c.getGroupByColumns().clear();

        UniqueList criteriaSelectModifiers;
        criteriaSelectModifiers = c.getSelectModifiers();

        boolean distinct = false;
        if( criteriaSelectModifiers != null && 
            criteriaSelectModifiers.size() > 0 && 
            criteriaSelectModifiers.contains( SqlEnum.DISTINCT.toString() ) )
    	{
            criteriaSelectModifiers.remove( SqlEnum.DISTINCT.toString() );
            distinct = true;
        }

        StringBuffer countStr = new StringBuffer( "COUNT(" );
        countStr.append( distinct == true ? SqlEnum.DISTINCT.toString() : "" );
        countStr.append( columnName );
        countStr.append( ")" );

        c.addSelectColumn( countStr.toString() );

        List result;
        if( conn == null )
        {
            result = BasePeer.doSelect( c );
        }
        else
    	{
            result = BasePeer.doSelect( c, conn );
        } 
        Record record = (Record) result.get(0);
        try {
            return record.getValue(1).asInt();
        }
        catch (DataSetException e) {
            throw new TorqueException(e);
        }
    }
}
